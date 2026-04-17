package com.bachld.service;

import com.bachld.client.PersonalComputerApiClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.request.PersonalComputerUpdateRequest;
import com.bachld.model.response.PersonalComputerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Service for personal computer management operations.
 * Executes API calls on background threads via SwingWorker.
 */
public class PersonalComputerService {

    private static final Logger logger = LoggerFactory.getLogger(PersonalComputerService.class);
    private final PersonalComputerApiClient apiClient;

    public PersonalComputerService(PersonalComputerApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Callback interface for fetching personal computer info.
     */
    public interface FetchCallback {
        void onSuccess(PersonalComputerResponse response);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for updating personal computer info.
     */
    public interface UpdateCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Asynchronously fetches the current user's personal computer info.
     * Callback is invoked on the EDT.
     */
    public void fetchMyComputerAsync(FetchCallback callback) {
        logger.info("Fetching personal computer info...");
        new SwingWorker<PersonalComputerResponse, Void>() {
            @Override
            protected PersonalComputerResponse doInBackground() throws Exception {
                return apiClient.getMyComputer();
            }

            @Override
            protected void done() {
                try {
                    PersonalComputerResponse response = get();
                    if (response != null && response.getStatusCode() == 200) {
                        logger.info("Successfully fetched personal computer info");
                        callback.onSuccess(response);
                    } else {
                        callback.onError("Không thể tải thông tin máy tính cá nhân.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    String errorMessage = mapExceptionToUserMessage(e.getCause());
                    logger.error("Failed to fetch personal computer info", e.getCause());
                    callback.onError(errorMessage);
                }
            }
        }.execute();
    }

    /**
     * Asynchronously updates the current user's personal computer IP address.
     * Callback is invoked on the EDT.
     */
    public void updateComputerAsync(String ipAddress, UpdateCallback callback) {
        logger.info("Updating personal computer IP to: {}", ipAddress);
        new SwingWorker<PersonalComputerResponse, Void>() {
            @Override
            protected PersonalComputerResponse doInBackground() throws Exception {
                PersonalComputerUpdateRequest request = new PersonalComputerUpdateRequest(ipAddress);
                return apiClient.updateComputer(request);
            }

            @Override
            protected void done() {
                try {
                    PersonalComputerResponse response = get();
                    if (response != null && response.getStatusCode() == 200) {
                        logger.info("Successfully updated personal computer IP");
                        callback.onSuccess();
                    } else {
                        callback.onError("Không thể cập nhật thông tin máy tính cá nhân.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    // Check for RestClientException with 400 status (validation error)
                    if (cause instanceof RestClientException) {
                        RestClientException rce = (RestClientException) cause;
                        if (rce.hasStatusCode() && rce.getStatusCode() == 400) {
                            logger.warn("Validation error: {}", rce.getMessage());
                            callback.onError(rce.getMessage());
                            return;
                        }
                    }
                    String errorMessage = mapExceptionToUserMessage(cause);
                    logger.error("Failed to update personal computer", cause);
                    callback.onError(errorMessage);
                }
            }
        }.execute();
    }

    /**
     * Maps exceptions to user-friendly error messages.
     */
    private String mapExceptionToUserMessage(Throwable throwable) {
        Throwable cause = throwable;
        if (throwable instanceof RestClientException && throwable.getCause() != null) {
            cause = throwable.getCause();
        }
        if (cause instanceof SocketTimeoutException) {
            return "Yêu cầu đã hết thời gian chờ. Vui lòng thử lại.";
        } else if (cause instanceof ConnectException) {
            return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
        } else if (cause instanceof UnknownHostException) {
            return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra cấu hình.";
        }
        return "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.";
    }
}

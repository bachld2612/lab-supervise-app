package com.bachld.service;

import com.bachld.client.UserApiClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.request.ChangePasswordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserApiClient userApiClient;

    public UserService(UserApiClient userApiClient) {
        this.userApiClient = userApiClient;
    }

    public interface ChangePasswordCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void changePasswordAsync(String oldPassword, String newPassword, ChangePasswordCallback callback) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                userApiClient.changePassword(new ChangePasswordRequest(oldPassword, newPassword));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    callback.onSuccess();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    String message;
                    if (cause instanceof RestClientException) {
                        message = cause.getMessage();
                    } else if (cause instanceof SocketTimeoutException) {
                        message = "Yêu cầu đã hết thời gian chờ. Vui lòng thử lại.";
                    } else if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                        message = "Không thể kết nối đến máy chủ.";
                    } else {
                        message = "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.";
                    }
                    logger.error("Change password error", cause);
                    callback.onError(message);
                }
            }
        }.execute();
    }
}
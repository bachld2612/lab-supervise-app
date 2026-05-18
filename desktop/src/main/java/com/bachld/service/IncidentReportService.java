package com.bachld.service;

import com.bachld.client.IncidentReportApiClient;
import com.bachld.model.request.IncidentReportCreateRequest;
import com.bachld.model.response.IncidentReportListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

public class IncidentReportService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentReportService.class);
    private final IncidentReportApiClient apiClient;

    public IncidentReportService(IncidentReportApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public interface FetchCallback {
        void onSuccess(IncidentReportListResponse response);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void fetchMyReportsAsync(FetchCallback callback) {
        logger.info("Fetching student incident reports...");
        new SwingWorker<IncidentReportListResponse, Void>() {
            @Override
            protected IncidentReportListResponse doInBackground() throws Exception {
                return apiClient.getMyReports(0, 50);
            }

            @Override
            protected void done() {
                try {
                    IncidentReportListResponse response = get();
                    if (response != null && response.getStatusCode() == 200) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError("Không thể tải danh sách báo cáo.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (ExecutionException e) {
                    String msg = extractMessage(e);
                    logger.error("Failed to fetch reports", e.getCause());
                    callback.onError(msg);
                }
            }
        }.execute();
    }

    public void createReportAsync(String title, OperationCallback callback) {
        logger.info("Creating incident report...");
        IncidentReportCreateRequest request = new IncidentReportCreateRequest(title);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.createReport(request);
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
                } catch (ExecutionException e) {
                    String msg = extractMessage(e);
                    logger.error("Failed to create report", e.getCause());
                    callback.onError(msg);
                }
            }
        }.execute();
    }

    public void updateReportAsync(int id, String title, OperationCallback callback) {
        logger.info("Updating incident report {}...", id);
        IncidentReportCreateRequest request = new IncidentReportCreateRequest(title);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.updateReport(id, request);
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
                } catch (ExecutionException e) {
                    String msg = extractMessage(e);
                    logger.error("Failed to update report {}", id, e.getCause());
                    callback.onError(msg);
                }
            }
        }.execute();
    }

    private String extractMessage(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof com.bachld.exception.RestClientException rce) {
            return rce.getMessage();
        }
        return "Lỗi kết nối đến máy chủ.";
    }
}
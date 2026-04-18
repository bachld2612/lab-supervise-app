package com.bachld.service;

import com.bachld.client.ClassApiClient;
import com.bachld.model.response.ClassListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;

/**
 * Service for class management operations.
 */
public class ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);
    private final ClassApiClient apiClient;

    public ClassService(ClassApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public interface FetchCallback {
        void onSuccess(ClassListResponse response);
        void onError(String errorMessage);
    }

    /**
     * Asynchronously fetches the student's classes.
     */
    public void fetchMyClassesAsync(FetchCallback callback) {
        logger.info("Fetching student classes...");
        new SwingWorker<ClassListResponse, Void>() {
            @Override
            protected ClassListResponse doInBackground() throws Exception {
                return apiClient.getMyClasses();
            }

            @Override
            protected void done() {
                try {
                    ClassListResponse response = get();
                    if (response != null && response.getStatusCode() == 200) {
                        logger.info("Successfully fetched student classes");
                        callback.onSuccess(response);
                    } else {
                        callback.onError("Không thể tải danh sách lớp học.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    logger.error("Failed to fetch classes", e.getCause());
                    callback.onError("Lỗi kết nối đến máy chủ.");
                }
            }
        }.execute();
    }
}

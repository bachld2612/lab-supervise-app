package com.bachld.service;

import com.bachld.client.ExamRoomApiClient;
import com.bachld.model.response.ExamRoomListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;

public class ExamRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ExamRoomService.class);
    private final ExamRoomApiClient apiClient;

    public ExamRoomService(ExamRoomApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public interface FetchCallback {
        void onSuccess(ExamRoomListResponse response);
        void onError(String errorMessage);
    }

    public void fetchMyExamRoomsAsync(FetchCallback callback) {
        logger.info("Fetching student exam rooms...");
        new SwingWorker<ExamRoomListResponse, Void>() {
            @Override
            protected ExamRoomListResponse doInBackground() throws Exception {
                return apiClient.getMyExamRooms();
            }

            @Override
            protected void done() {
                try {
                    ExamRoomListResponse response = get();
                    if (response != null && response.getStatusCode() == 200) {
                        logger.info("Successfully fetched student exam rooms");
                        callback.onSuccess(response);
                    } else {
                        callback.onError("Không thể tải lịch thi.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    logger.error("Failed to fetch exam rooms", e.getCause());
                    callback.onError("Lỗi kết nối đến máy chủ.");
                }
            }
        }.execute();
    }
}

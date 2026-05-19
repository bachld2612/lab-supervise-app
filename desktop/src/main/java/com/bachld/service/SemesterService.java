package com.bachld.service;

import com.bachld.client.SemesterApiClient;
import com.bachld.model.response.SemesterData;
import com.bachld.model.response.SemesterPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;

public class SemesterService {

    private static final Logger logger = LoggerFactory.getLogger(SemesterService.class);
    private final SemesterApiClient apiClient;

    public SemesterService(SemesterApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public interface FetchCallback {
        void onSuccess(List<SemesterData> semesters);
        void onError(String errorMessage);
    }

    public void fetchSemestersAsync(FetchCallback callback) {
        logger.info("Fetching semesters...");
        new SwingWorker<List<SemesterData>, Void>() {
            @Override
            protected List<SemesterData> doInBackground() throws Exception {
                SemesterPageResponse response = apiClient.getSemesters();
                if (response != null && response.getStatusCode() == 200
                        && response.getData() != null
                        && response.getData().getContent() != null) {
                    return response.getData().getContent();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    callback.onSuccess(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    callback.onError("Yêu cầu đã bị hủy.");
                } catch (java.util.concurrent.ExecutionException e) {
                    logger.error("Failed to fetch semesters", e.getCause());
                    callback.onSuccess(new ArrayList<>());
                }
            }
        }.execute();
    }
}

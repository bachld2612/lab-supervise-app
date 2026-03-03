package com.bachld.backend.schedule;

import com.bachld.backend.dto.request.StudentLogCreateRequest;
import com.bachld.backend.model.StudentLog;
import com.bachld.backend.repository.StudentLogRepository;
import com.bachld.backend.service.SseService;
import com.bachld.backend.service.StudentLogService;
import com.bachld.backend.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TrackingScheduleJob {

    @Autowired
    private StudentLogService studentLogService;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private SseService sseService;

    @Scheduled(fixedRate = 5000)
    public void trackingInfo() throws InterruptedException {
        List<String> browserTabs = trackingService.getOpenBrowserTabs();
        List<StudentLogCreateRequest> studentLogs = new ArrayList<>();
        for (String browserTab : browserTabs) {
            StudentLogCreateRequest logRequest = StudentLogCreateRequest.builder()
                    .appLog(browserTab)
                    .studentId(1)
                    .build();
            studentLogs.add(logRequest);

            log.info(browserTab);

        }
        studentLogService.create(studentLogs);
        sseService.handleEvent();
    }

}

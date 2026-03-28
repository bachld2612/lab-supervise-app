package com.bachld.backend.service;

import com.bachld.backend.dto.request.ScheduleCreateRequest;
import com.bachld.backend.dto.request.ScheduleUpdateRequest;
import com.bachld.backend.dto.response.ScheduleResponse;
import com.bachld.backend.model.Schedule;
import com.bachld.backend.repository.ScheduleRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduleService {

    ScheduleRepository scheduleRepository;

    public Page<ScheduleResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }

        return scheduleRepository.findByKeyword(pageable, keyword, status);
    }

    public ScheduleResponse getById(Integer id) {
        return scheduleRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(ScheduleCreateRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }

        Schedule schedule = new Schedule();
        schedule.setName(request.getName());
        schedule.setDaysOfWeek(request.getDaysOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setStatus(Status.ACTIVE.getValue());

        scheduleRepository.save(schedule);
    }

    @Transactional
    public void update(ScheduleUpdateRequest request, int id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch học có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            schedule.setName(request.getName());
        }

        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            schedule.setDaysOfWeek(request.getDaysOfWeek());
        }

        LocalTime newStart = request.getStartTime() != null ? request.getStartTime() : schedule.getStartTime();
        LocalTime newEnd = request.getEndTime() != null ? request.getEndTime() : schedule.getEndTime();

        if (newStart.isAfter(newEnd) || newStart.equals(newEnd)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }

        schedule.setStartTime(newStart);
        schedule.setEndTime(newEnd);

        scheduleRepository.save(schedule);
    }
}

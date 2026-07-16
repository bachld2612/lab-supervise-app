package com.bachld.backend.service;

import com.bachld.backend.dto.request.ScheduleCreateRequest;
import com.bachld.backend.dto.request.ScheduleUpdateRequest;
import com.bachld.backend.dto.response.ScheduleResponse;
import com.bachld.backend.model.Schedule;
import com.bachld.backend.repository.ScheduleRepository;
import com.bachld.backend.util.enums.Period;
import com.bachld.backend.util.enums.Status;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    Schedule schedule = new Schedule();
    schedule.setName(request.getName());
    schedule.setDaysOfWeek(request.getDaysOfWeek());
    schedule.setStatus(Status.ACTIVE.getValue());

    processPeriods(schedule, request.getPeriods());

    scheduleRepository.save(schedule);
  }

  @Transactional
  public void update(ScheduleUpdateRequest request, int id) {
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy lịch học có id: " + id));

    if (request.getName() != null && !request.getName().isEmpty()) {
      schedule.setName(request.getName());
    }

    if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
      schedule.setDaysOfWeek(request.getDaysOfWeek());
    }

    if (request.getPeriods() != null && !request.getPeriods().isEmpty()) {
      processPeriods(schedule, request.getPeriods());
    }

    scheduleRepository.save(schedule);
  }

  @Transactional
  public void delete(int id) {
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy lịch học có id: " + id));

    schedule.setStatus(Status.INACTIVE.getValue());
    scheduleRepository.save(schedule);
  }

  private void processPeriods(Schedule schedule, String periodsStr) {
    if (periodsStr == null || periodsStr.isEmpty()) return;

    List<Integer> periodList =
        Arrays.stream(periodsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Integer::parseInt)
            .distinct()
            .sorted()
            .toList();

    if (periodList.isEmpty()) {
      throw new IllegalArgumentException("Danh sách tiết học không được để trống");
    }

    String sortedPeriods =
        periodList.stream().map(Object::toString).collect(Collectors.joining(","));
    schedule.setPeriods(sortedPeriods);

    Period firstPeriod = Period.fromValue(periodList.get(0));
    Period lastPeriod = Period.fromValue(periodList.get(periodList.size() - 1));

    schedule.setStartTime(firstPeriod.getStartTime());
    schedule.setEndTime(lastPeriod.getEndTime());
  }
}

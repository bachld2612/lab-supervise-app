package com.bachld.backend.service;

import com.bachld.backend.dto.request.IncidentReportStudentCreateRequest;
import com.bachld.backend.dto.request.IncidentReportTeacherCreateRequest;
import com.bachld.backend.dto.response.ClassResponse;
import com.bachld.backend.dto.response.IncidentReportResponse;
import com.bachld.backend.model.IncidentReport;
import com.bachld.backend.model.Schedule;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.IncidentReportRepository;
import com.bachld.backend.repository.RoomRepository;
import com.bachld.backend.repository.ScheduleRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IncidentReportService {

  IncidentReportRepository incidentReportRepository;

  ClassRepository classRepository;

  ScheduleRepository scheduleRepository;

  RoomRepository roomRepository;

  Util util;

  public Page<IncidentReportResponse> getList(
      Pageable pageable, String keyword, Integer status, Integer roomId) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }
    return incidentReportRepository.findAllByKeyword(pageable, keyword, status, roomId, null);
  }

  public Page<IncidentReportResponse> getListForTeacher(
      Pageable pageable, String keyword, Integer status, Integer roomId) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }
    return incidentReportRepository.findAllByKeyword(
        pageable, keyword, status, roomId, util.getCurrentUser().getId());
  }

  public Page<IncidentReportResponse> getListForStudent(
      Pageable pageable, String keyword, Integer status) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }
    return incidentReportRepository.findAllByKeyword(
        pageable, keyword, status, null, util.getCurrentUser().getId());
  }

  public void createForStudent(IncidentReportStudentCreateRequest request) {
    User currentUser = util.getCurrentUser();

    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    int dayOfWeek = today.getDayOfWeek().getValue();

    List<ClassResponse> classes =
        classRepository.findActiveClassByStudentUserId(currentUser.getId(), today);

    Integer activeRoomId = null;
    for (ClassResponse cls : classes) {
      Schedule schedule = scheduleRepository.findById(cls.getScheduleId()).orElse(null);
      if (schedule == null || schedule.getDaysOfWeek() == null) continue;

      boolean isToday =
          Arrays.asList(schedule.getDaysOfWeek().split(",")).contains(String.valueOf(dayOfWeek));
      boolean isActiveTime =
          !now.isBefore(schedule.getStartTime()) && !now.isAfter(schedule.getEndTime());

      if (isToday && isActiveTime) {
        if (cls.getRoomId() == null) {
          throw new IllegalArgumentException("Lớp học hiện tại chưa được gán phòng học");
        }
        activeRoomId = cls.getRoomId();
        break;
      }
    }

    if (activeRoomId == null) {
      throw new IllegalArgumentException("Bạn không có giờ học đang diễn ra");
    }

    IncidentReport report = new IncidentReport();
    report.setTitle(request.getTitle());
    report.setRoomId(activeRoomId);
    report.setReporterId(currentUser.getId());
    report.setReporterRole("STUDENT");
    report.setStatus(0);
    incidentReportRepository.save(report);
  }

  public void createForTeacher(IncidentReportTeacherCreateRequest request) {
    User currentUser = util.getCurrentUser();

    if (roomRepository.findByIdAndStatus(request.getRoomId(), Status.ACTIVE.getValue()) == null) {
      throw new IllegalArgumentException("Phòng học không tồn tại");
    }

    IncidentReport report = new IncidentReport();
    report.setTitle(request.getTitle());
    report.setRoomId(request.getRoomId());
    report.setReporterId(currentUser.getId());
    report.setReporterRole("TEACHER");
    report.setStatus(0);
    incidentReportRepository.save(report);
  }

  public void updateForStudent(Integer id, IncidentReportStudentCreateRequest request) {
    IncidentReport report = getAndValidatePending(id);
    validateOwner(report);

    if (request.getTitle() != null && !request.getTitle().isEmpty()) {
      report.setTitle(request.getTitle());
    }
    incidentReportRepository.save(report);
  }

  public void updateForTeacher(Integer id, IncidentReportTeacherCreateRequest request) {
    IncidentReport report = getAndValidatePending(id);
    validateOwner(report);

    if (request.getTitle() != null && !request.getTitle().isEmpty()) {
      report.setTitle(request.getTitle());
    }
    if (request.getRoomId() != null) {
      if (roomRepository.findByIdAndStatus(request.getRoomId(), Status.ACTIVE.getValue()) == null) {
        throw new IllegalArgumentException("Phòng học không tồn tại");
      }
      report.setRoomId(request.getRoomId());
    }
    incidentReportRepository.save(report);
  }

  public void resolve(Integer id) {
    IncidentReport report = getAndValidatePending(id);
    report.setStatus(1);
    report.setHandlerId(util.getCurrentUser().getId());
    incidentReportRepository.save(report);
  }

  public void reject(Integer id) {
    IncidentReport report = getAndValidatePending(id);
    report.setStatus(2);
    report.setHandlerId(util.getCurrentUser().getId());
    incidentReportRepository.save(report);
  }

  private IncidentReport getAndValidatePending(Integer id) {
    IncidentReport report =
        incidentReportRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy báo cáo sự cố có id: " + id));
    if (report.getStatus() != 0) {
      throw new IllegalArgumentException(
          "Không thể thao tác với báo cáo đã được xử lý hoặc từ chối");
    }
    return report;
  }

  private void validateOwner(IncidentReport report) {
    User currentUser = util.getCurrentUser();
    if (!report.getReporterId().equals(currentUser.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa báo cáo này");
    }
  }
}

package com.bachld.backend.service;

import com.bachld.backend.dto.request.ClassCreateRequest;
import com.bachld.backend.dto.request.ClassUpdateRequest;
import com.bachld.backend.dto.response.*;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {

    ClassRepository classRepository;

    SubjectRepository subjectRepository;

    TeacherRepository teacherRepository;

    ScheduleRepository scheduleRepository;

    SemesterRepository semesterRepository;

    StudentRepository studentRepository;

    StudentClassInfoRepository studentClassInfoRepository;

    StudentClassRepository studentClassRepository;

    Util util;

    public Page<ClassResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }

        return classRepository.findByKeyword(pageable, keyword, status);
    }

    public ClassResponse getById(Integer id) {
        return classRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(ClassCreateRequest request) {
        SubjectResponse subject = subjectRepository.findByIdAndStatus(request.getSubjectId(), Status.ACTIVE.getValue());
        if (subject == null) {
            throw new IllegalArgumentException("Không tìm thấy môn học có id: " + request.getSubjectId());
        }

        TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());
        if (teacher == null) {
            throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
        }

        ScheduleResponse schedule = scheduleRepository.findByIdAndStatus(request.getScheduleId(), Status.ACTIVE.getValue());
        if (schedule == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch học có id: " + request.getScheduleId());
        }

        SemesterResponse semester = semesterRepository.findByIdAndStatus(request.getSemesterId(), Status.ACTIVE.getValue());
        if (semester == null) {
            throw new IllegalArgumentException("Không tìm thấy học kì có id: " +  request.getSemesterId());
        }

        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn ngày kết thúc");
        }

        util.validateRoom(request.getRoomId(), request.getScheduleId(), startDate, endDate, null);

        Classes classes = new Classes();
        classes.setName(request.getName());
        classes.setMaxStudent(request.getMaxStudent());
        classes.setSubjectId(request.getSubjectId());
        classes.setTeacherId(request.getTeacherId());
        classes.setScheduleId(request.getScheduleId());
        classes.setStartDate(startDate);
        classes.setEndDate(endDate);
        classes.setSemesterId(request.getSemesterId());
        classes.setStatus(Status.ACTIVE.getValue());
        classes.setRoomId(request.getRoomId());

        int sessionCount = schedule.getPeriods().split(",").length;
        int sessionNumber = (subject.getCreditNumber() * 15) / sessionCount;
        classes.setSessionNumber(sessionNumber);

        classRepository.save(classes);
    }

    public List<ClassResponse> getListByStudentUserId() {
        User currentUser = util.getCurrentUser();
        LocalDate today = LocalDate.now();
        List<ClassResponse> response = classRepository.findActiveClassByStudentUserId(currentUser.getId(),  today);

        List<Schedule> schedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        LocalTime nowTime = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        // studyStatus = 1 --> ongoing, 0 --> upcoming, 2 --> ended
        for (ClassResponse clazz : response) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));
                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());
                boolean isEnded = isToday && nowTime.isAfter(schedule.getEndTime());
                if (isToday && isActiveTime) {
                    clazz.setStudyStatus(1);
                } else if (isEnded) {
                    clazz.setStudyStatus(2);
                } else {
                    clazz.setStudyStatus(0);
                }
            } else {
                clazz.setStudyStatus(0);
            }
        }

        return response.stream()
                .sorted(Comparator.comparingInt((ClassResponse c) -> {
                    int s = c.getStudyStatus() == null ? 0 : c.getStudyStatus();
                    return s == 1 ? 0 : s == 0 ? 1 : 2;
                }))
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(ClassUpdateRequest request, int id) {
        Classes classes = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            classes.setName(request.getName());
        }

        if (request.getMaxStudent() != null) {
            classes.setMaxStudent(request.getMaxStudent());
        }

        if (request.getTeacherId() != null) {
            TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());
            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
            }
            classes.setTeacherId(request.getTeacherId());
        }

        if (request.getSubjectId() != null) {
            SubjectResponse subject = subjectRepository.findByIdAndStatus(request.getSubjectId(), Status.ACTIVE.getValue());
            if (subject == null) {
                throw new IllegalArgumentException("Không tìm thấy môn học có id: " + request.getSubjectId());
            }
            classes.setSubjectId(request.getSubjectId());
        }

        if (request.getScheduleId() != null) {
            ScheduleResponse schedule = scheduleRepository.findByIdAndStatus(request.getScheduleId(), Status.ACTIVE.getValue());
            if (schedule == null) {
                throw new IllegalArgumentException("Không tìm thấy lịch học có id: " + request.getScheduleId());
            }
            classes.setScheduleId(request.getScheduleId());
        }

        if (request.getSemesterId() != null) {
            SemesterResponse semester = semesterRepository.findByIdAndStatus(request.getSemesterId(), Status.ACTIVE.getValue());
            if (semester == null) {
                throw new IllegalArgumentException("Không tìm thấy học kì có id: " +  request.getSemesterId());
            }
            classes.setSemesterId(request.getSemesterId());
        }

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            classes.setStartDate(LocalDate.parse(request.getStartDate()));
        }

        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            classes.setEndDate(LocalDate.parse(request.getEndDate()));
        }

        if (classes.getStartDate().isAfter(classes.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải sớm hơn ngày kết thúc");
        }

        if (request.getRoomId() != null) {
            classes.setRoomId(request.getRoomId());
        }
        util.validateRoom(classes.getRoomId(), classes.getScheduleId(), classes.getStartDate(), classes.getEndDate(), id);

        SubjectResponse currentSubject = subjectRepository.findByIdAndStatus(classes.getSubjectId(), Status.ACTIVE.getValue());
        ScheduleResponse currentSchedule = scheduleRepository.findByIdAndStatus(classes.getScheduleId(), Status.ACTIVE.getValue());
        
        if (currentSubject != null && currentSchedule != null) {
            int currentSessionCount = currentSchedule.getPeriods().split(",").length;
            int newSessionNumber = (currentSubject.getCreditNumber() * 15) / currentSessionCount;
            classes.setSessionNumber(newSessionNumber);
        }

        classRepository.save(classes);
    }

    public List<ClassResponse> getListByTeacherUserId() {
        User currentUser = util.getCurrentUser();
        LocalDate today = LocalDate.now();
        List<ClassResponse> response = classRepository.findActiveClassByTeacherUserId(currentUser.getId(), today);

        List<Schedule> schedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        LocalTime nowTime = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        for (ClassResponse clazz : response) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));
                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());
                boolean isEnded = isToday && nowTime.isAfter(schedule.getEndTime());
                if (isToday && isActiveTime) {
                    clazz.setStudyStatus(1);
                } else if (isEnded) {
                    clazz.setStudyStatus(2);
                } else {
                    clazz.setStudyStatus(0);
                }
            } else {
                clazz.setStudyStatus(0);
            }
        }

        return response.stream()
                .sorted(Comparator.comparingInt((ClassResponse c) -> {
                    int s = c.getStudyStatus() == null ? 0 : c.getStudyStatus();
                    return s == 1 ? 0 : s == 0 ? 1 : 2;
                }))
                .collect(Collectors.toList());
    }

    public List<ClassStudentTrackingResponse> getTrackingByClassId(Integer classId, LocalDate date) {
        List<ClassStudentTrackingResponse> students = studentClassInfoRepository.findStudentsWithLatestTracking(classId);

        List<StudentAppUsageRaw> rawUsage = studentClassInfoRepository.findAppUsageByClassIdAndDate(classId, date);

        Map<Integer, List<AppUsageItem>> usageByStudent = rawUsage.stream()
                .collect(Collectors.groupingBy(
                        StudentAppUsageRaw::getStudentId,
                        Collectors.mapping(
                                r -> AppUsageItem.builder()
                                        .applicationName(r.getApplicationName())
                                        .createdAt(r.getCreatedAt())
                                        .isBanApplication(r.isBanApplication())
                                        .connectionType(r.getConnectionType())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        students.forEach(s -> s.setApplicationsToday(usageByStudent.getOrDefault(s.getStudentId(), List.of())));

        return students;
    }

    public int getStudyStatus(Integer classId) {
        Classes clazz = classRepository.findById(classId).orElse(null);
        if (clazz == null) return 0;

        LocalDate today = LocalDate.now();
        if (today.isBefore(clazz.getStartDate()) || today.isAfter(clazz.getEndDate())) return 0;

        Schedule schedule = scheduleRepository.findById(clazz.getScheduleId()).orElse(null);
        if (schedule == null || schedule.getDaysOfWeek() == null) return 0;

        boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                .contains(String.valueOf(today.getDayOfWeek().getValue()));
        if (!isToday) return 0;

        LocalTime now = LocalTime.now();
        boolean isActiveTime = !now.isBefore(schedule.getStartTime()) && !now.isAfter(schedule.getEndTime());
        if (isActiveTime) return 1;
        if (now.isAfter(schedule.getEndTime())) return 2;
        return 0;
    }

    public Page<StudentResponse> getStudentsByClassId(Integer classId, Pageable pageable, String keyword) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }
        return studentRepository.findByClassId(pageable, classId, keyword);
    }

    @Transactional
    public void updateWifiSsid(int id, String wifiSsid) {
        Classes classes = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + id));
        classes.setWifiSsid(wifiSsid);
        classRepository.save(classes);
    }

    @Transactional
    public String generateWifiSsid(int id) {
        Classes classes = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + id));
        String ssid = UUID.randomUUID().toString().replace("-", "");
        classes.setWifiSsid(ssid);
        classRepository.save(classes);
        return ssid;
    }

    public void delete(Integer id) {
        Classes cs = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần có id: " + id));

        cs.setStatus(Status.INACTIVE.getValue());
        classRepository.save(cs);
    }

    public ResponseEntity<InputStreamResource> downloadClassStudentImportTemplate() throws IOException {
        org.springframework.core.io.ClassPathResource file =
                new org.springframework.core.io.ClassPathResource("templates/download/checkin_import_student_template.xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=checkin_import_student_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(file.getInputStream()));
    }

    @Transactional
    public void importStudentsToClass(Integer classId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        Classes classes = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần có id: " + classId));

        if (classes.getStatus() != Status.ACTIVE.getValue()) {
            throw new IllegalArgumentException("Lớp học phần không còn hoạt động");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            List<Student> studentsToEnroll = new ArrayList<>();

            // Template bắt đầu từ dòng 10 (index 9), cột 1 (index 0) là mã sinh viên
            for (int i = 9; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || !isValidDataRow(row, 5)) {
                    continue;
                }

                int rowNum = i + 1;
                String studentCode = util.getCellStringValue(row.getCell(1));
                if (studentCode == null || studentCode.isBlank()) {
                    continue;
                }

                Student student = studentRepository.findByCodeAndStatus(studentCode, Status.ACTIVE.getValue())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Dòng " + rowNum + ": Không tìm thấy sinh viên với mã: " + studentCode));

                boolean alreadyEnrolled = studentClassRepository
                        .findByStudentIdAndClassId(student.getId(), classId)
                        .isPresent();

                if (!alreadyEnrolled) {
                    studentsToEnroll.add(student);
                }
            }

            long currentCount = studentClassRepository.countByClassId(classId);
            if (currentCount + studentsToEnroll.size() > classes.getMaxStudent()) {
                throw new IllegalArgumentException(
                        "Vượt quá sĩ số tối đa. Hiện tại: " + currentCount
                        + ", thêm mới: " + studentsToEnroll.size()
                        + ", tối đa: " + classes.getMaxStudent());
            }

            for (Student student : studentsToEnroll) {
                StudentClass studentClass = new StudentClass();
                studentClass.setClassId(classId);
                studentClass.setStudentId(student.getId());
                studentClassRepository.save(studentClass);
            }
        }
    }

    private boolean isValidDataRow(Row row, int requiredColumnCount) {
        for (int c = 0; c < requiredColumnCount; c++) {
            String val = util.getCellStringValue(row.getCell(c));
            if (val == null || val.isBlank()) return false;
        }
        return true;
    }
}

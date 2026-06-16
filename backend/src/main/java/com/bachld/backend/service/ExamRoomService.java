package com.bachld.backend.service;

import com.bachld.backend.config.ConnectedExamStudentRegistry;
import com.bachld.backend.dto.request.ExamRoomCreateRequest;
import com.bachld.backend.dto.request.ExamRoomUpdateRequest;
import com.bachld.backend.dto.response.AppUsageItem;
import com.bachld.backend.dto.response.ClassStudentTrackingResponse;
import com.bachld.backend.dto.response.ExamRoomResponse;
import com.bachld.backend.dto.response.StudentAppUsageRaw;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Period;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamRoomService {

    ExamRoomRepository examRoomRepository;
    StudentExamRoomRepository studentExamRoomRepository;
    StudentExamRoomInfoRepository studentExamRoomInfoRepository;
    StudentRepository studentRepository;
    TeacherRepository teacherRepository;
    ConnectedExamStudentRegistry connectedExamStudentRegistry;
    ClipboardTextCryptoService clipboardTextCryptoService;
    Util util;

    public Page<ExamRoomResponse> getList(Pageable pageable, String keyword, Integer semesterId, Integer status) {
        String kw = keyword != null ? "%" + keyword.trim().toLowerCase() + "%" : null;
        return examRoomRepository.findByKeyword(pageable, kw, semesterId, status);
    }

    public ExamRoomResponse getById(Integer id) {
        ExamRoomResponse response = examRoomRepository.findByIdProjected(id);
        if (response == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng thi có id: " + id);
        }
        return response;
    }

    public List<ExamRoomResponse> getStudentExamRooms() {
        User currentUser = util.getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
        List<ExamRoomResponse> rooms = examRoomRepository.findByStudent(student.getId());
        fillStudyStatus(rooms);
        return rooms;
    }

    public List<ExamRoomResponse> getTeacherExamRooms() {
        User currentUser = util.getCurrentUser();
        if (com.bachld.backend.util.enums.Role.IT_CENTER.getValue() == currentUser.getRoleId()) {
            List<ExamRoomResponse> rooms = examRoomRepository.findAllActive();
            fillStudyStatus(rooms);
            return rooms;
        }
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên"));
        List<ExamRoomResponse> rooms = examRoomRepository.findByTeacher(teacher.getId());
        fillStudyStatus(rooms);
        return rooms;
    }

    private void fillStudyStatus(List<ExamRoomResponse> rooms) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        rooms.forEach(er -> {
            if (er.getExamDate() == null || er.getStartTime() == null || er.getEndTime() == null) {
                er.setStudyStatus(0);
                return;
            }
            if (today.isBefore(er.getExamDate())) { er.setStudyStatus(0); return; }
            if (today.isAfter(er.getExamDate())) { er.setStudyStatus(2); return; }
            if (!now.isBefore(er.getStartTime()) && !now.isAfter(er.getEndTime())) { er.setStudyStatus(1); return; }
            if (now.isAfter(er.getEndTime())) { er.setStudyStatus(2); return; }
            er.setStudyStatus(0);
        });
    }

    @Transactional
    public void create(ExamRoomCreateRequest request) {
        LocalDate examDate = LocalDate.parse(request.getExamDate());
        PeriodRange periodRange = parsePeriodRange(request.getPeriods());

        if (request.getTeacher1Id().equals(request.getTeacher2Id())) {
            throw new IllegalArgumentException("Hai giảng viên coi thi phải khác nhau");
        }

        validateRoomConflict(request.getRoomId(), examDate, periodRange.startTime(), periodRange.endTime(), null);
        validateTeacherConflict(request.getTeacher1Id(), examDate, periodRange.startTime(), periodRange.endTime(), null);
        validateTeacherConflict(request.getTeacher2Id(), examDate, periodRange.startTime(), periodRange.endTime(), null);

        ExamRoom examRoom = new ExamRoom();
        examRoom.setCode(request.getCode());
        examRoom.setRoomId(request.getRoomId());
        examRoom.setTeacher1Id(request.getTeacher1Id());
        examRoom.setTeacher2Id(request.getTeacher2Id());
        examRoom.setSubjectId(request.getSubjectId());
        examRoom.setSemesterId(request.getSemesterId());
        examRoom.setMaxStudent(request.getMaxStudent());
        examRoom.setExamDate(examDate);
        examRoom.setPeriods(periodRange.periods());
        examRoom.setStartTime(periodRange.startTime());
        examRoom.setEndTime(periodRange.endTime());
        examRoom.setStatus(Status.ACTIVE.getValue());
        examRoomRepository.save(examRoom);
    }

    @Transactional
    public void update(ExamRoomUpdateRequest request, Integer id) {
        ExamRoom examRoom = examRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            examRoom.setCode(request.getCode());
        }
        if (request.getRoomId() != null) examRoom.setRoomId(request.getRoomId());
        if (request.getTeacher1Id() != null) examRoom.setTeacher1Id(request.getTeacher1Id());
        if (request.getTeacher2Id() != null) examRoom.setTeacher2Id(request.getTeacher2Id());
        if (request.getSubjectId() != null) examRoom.setSubjectId(request.getSubjectId());
        if (request.getSemesterId() != null) examRoom.setSemesterId(request.getSemesterId());
        if (request.getMaxStudent() != null) examRoom.setMaxStudent(request.getMaxStudent());

        LocalDate examDate = request.getExamDate() != null
                ? LocalDate.parse(request.getExamDate()) : examRoom.getExamDate();
        String periods = examRoom.getPeriods();
        LocalTime startTime = examRoom.getStartTime();
        LocalTime endTime = examRoom.getEndTime();

        if (request.getPeriods() != null && !request.getPeriods().isBlank()) {
            PeriodRange periodRange = parsePeriodRange(request.getPeriods());
            periods = periodRange.periods();
            startTime = periodRange.startTime();
            endTime = periodRange.endTime();
        } else if (request.getStartTime() != null || request.getEndTime() != null) {
            startTime = request.getStartTime() != null
                    ? LocalTime.parse(request.getStartTime()) : examRoom.getStartTime();
            endTime = request.getEndTime() != null
                    ? LocalTime.parse(request.getEndTime()) : examRoom.getEndTime();
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
            }
        }
        if (examRoom.getTeacher1Id().equals(examRoom.getTeacher2Id())) {
            throw new IllegalArgumentException("Hai giảng viên coi thi phải khác nhau");
        }

        validateRoomConflict(examRoom.getRoomId(), examDate, startTime, endTime, id);
        validateTeacherConflict(examRoom.getTeacher1Id(), examDate, startTime, endTime, id);
        validateTeacherConflict(examRoom.getTeacher2Id(), examDate, startTime, endTime, id);

        examRoom.setExamDate(examDate);
        examRoom.setPeriods(periods);
        examRoom.setStartTime(startTime);
        examRoom.setEndTime(endTime);
        examRoomRepository.save(examRoom);
    }

    public void delete(Integer id) {
        ExamRoom examRoom = examRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + id));
        examRoom.setStatus(Status.INACTIVE.getValue());
        examRoomRepository.save(examRoom);
    }

    @Transactional
    public List<String> importStudents(Integer examRoomId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + examRoomId));

        if (examRoom.getStatus() != Status.ACTIVE.getValue()) {
            throw new IllegalArgumentException("Phòng thi không còn hoạt động");
        }

        List<String> errors = new ArrayList<>();
        List<StudentExamRoom> toEnroll = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 9; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String studentCode = util.getCellStringValue(row.getCell(1));
                if (studentCode == null || studentCode.isBlank()) continue;

                Student student = studentRepository.findByCodeAndStatus(studentCode, Status.ACTIVE.getValue())
                        .orElse(null);
                if (student == null) {
                    errors.add("Dòng " + (i + 1) + ": Không tìm thấy sinh viên với mã: " + studentCode);
                    continue;
                }

                boolean alreadyEnrolled = studentExamRoomRepository
                        .findByStudentIdAndExamRoomId(student.getId(), examRoomId).isPresent();
                if (alreadyEnrolled) continue;

                // Kiểm tra SV có trùng lịch thi không
                List<StudentExamRoom> conflicts = studentExamRoomRepository
                        .findByStudentIdAndExamDate(student.getId(), examRoom.getExamDate());
                boolean hasConflict = conflicts.stream().anyMatch(ser -> {
                    ExamRoom other = examRoomRepository.findById(ser.getExamRoomId()).orElse(null);
                    if (other == null || other.getId() == examRoomId) return false;
                    return examRoom.getStartTime().isBefore(other.getEndTime())
                            && examRoom.getEndTime().isAfter(other.getStartTime());
                });

                if (hasConflict) {
                    errors.add("Dòng " + (i + 1) + ": Sinh viên " + studentCode + " bị trùng lịch thi");
                    continue;
                }

                toEnroll.add(buildStudentExamRoom(examRoomId, student.getId()));
            }

            long currentCount = studentExamRoomRepository.countByExamRoomIdAndStatus(examRoomId, Status.ACTIVE.getValue());
            if (currentCount + toEnroll.size() > examRoom.getMaxStudent()) {
                throw new IllegalArgumentException(
                        "Vượt quá sĩ số tối đa. Hiện tại: " + currentCount
                        + ", thêm mới: " + toEnroll.size()
                        + ", tối đa: " + examRoom.getMaxStudent());
            }

            studentExamRoomRepository.saveAll(toEnroll);
        }

        return errors;
    }

    public List<ClassStudentTrackingResponse> getTrackingByExamRoomId(Integer examRoomId) {
        List<ClassStudentTrackingResponse> students =
                studentExamRoomInfoRepository.findStudentsWithLatestTracking(examRoomId);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<StudentAppUsageRaw> rawUsage =
                studentExamRoomInfoRepository.findAppUsageByExamRoomIdAndDate(examRoomId, startOfDay, endOfDay);

        Map<Integer, List<AppUsageItem>> usageByStudent = rawUsage.stream()
                .collect(Collectors.groupingBy(
                        StudentAppUsageRaw::getStudentId,
                        Collectors.mapping(
                                r -> AppUsageItem.builder()
                                        .applicationName(r.getApplicationName())
                                        .action(r.getAction() == null ? 0 : r.getAction().getValue())
                                        .clipboardText(decryptClipboardText(r))
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

    private String decryptClipboardText(StudentAppUsageRaw raw) {
        if (raw.getAction() == null || raw.getAction().getValue() == 0) {
            return null;
        }
        return clipboardTextCryptoService.decrypt(
                raw.getClipboardTextEncrypted(),
                raw.getClipboardKeyEncrypted(),
                raw.getClipboardIv()
        );
    }

    @Transactional
    public void setTrackingEnabled(Integer examRoomId, boolean enabled) {
        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + examRoomId));
        examRoom.setTrackingEnabled(enabled);
        examRoomRepository.save(examRoom);
    }

    @Transactional
    public void updateWifiSsid(int id, String wifiSsid) {
        ExamRoom examRoom = examRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + id));
        examRoom.setWifiSsid(wifiSsid);
        examRoomRepository.save(examRoom);
    }

    @Transactional
    public String generateWifiSsid(int id) {
        ExamRoom examRoom = examRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + id));
        String ssid = UUID.randomUUID().toString().replace("-", "");
        examRoom.setWifiSsid(ssid);
        examRoomRepository.save(examRoom);
        return ssid;
    }

    private PeriodRange parsePeriodRange(String periodsStr) {
        if (periodsStr == null || periodsStr.isBlank()) {
            throw new IllegalArgumentException("Tiết thi không được để trống");
        }

        List<Integer> periodList = Arrays.stream(periodsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Tiết thi không hợp lệ: " + value);
                    }
                })
                .distinct()
                .sorted()
                .toList();

        if (periodList.isEmpty()) {
            throw new IllegalArgumentException("Tiết thi không được để trống");
        }

        for (Integer period : periodList) {
            if (period < 1 || period > 12) {
                throw new IllegalArgumentException("Tiết thi phải từ 1 đến 12");
            }
        }

        for (int i = 1; i < periodList.size(); i++) {
            if (periodList.get(i) != periodList.get(i - 1) + 1) {
                throw new IllegalArgumentException("Các tiết thi phải liên tục");
            }
        }

        Period firstPeriod = Period.fromValue(periodList.get(0));
        Period lastPeriod = Period.fromValue(periodList.get(periodList.size() - 1));
        String sortedPeriods = periodList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        return new PeriodRange(sortedPeriods, firstPeriod.getStartTime(), lastPeriod.getEndTime());
    }

    private void validateRoomConflict(Integer roomId, LocalDate examDate, LocalTime startTime, LocalTime endTime, Integer excludeId) {
        List<ExamRoom> sameRoom = examRoomRepository.findByRoomIdAndExamDate(roomId, examDate);
        for (ExamRoom er : sameRoom) {
            if (excludeId != null && er.getId() == excludeId) continue;
            if (startTime.isBefore(er.getEndTime()) && endTime.isAfter(er.getStartTime())) {
                throw new IllegalArgumentException("Phòng đã được sử dụng trong khung giờ này");
            }
        }
    }

    private void validateTeacherConflict(Integer teacherId, LocalDate examDate, LocalTime startTime, LocalTime endTime, Integer excludeId) {
        List<ExamRoom> teacherExams = examRoomRepository.findByTeacherAndDate(teacherId, examDate);
        for (ExamRoom er : teacherExams) {
            if (excludeId != null && er.getId() == excludeId) continue;
            if (startTime.isBefore(er.getEndTime()) && endTime.isAfter(er.getStartTime())) {
                throw new IllegalArgumentException("Giảng viên đã có lịch coi thi trong khung giờ này");
            }
        }
    }

    public List<Integer> getConnectedStudents(Integer examRoomId) {
        return List.copyOf(connectedExamStudentRegistry.getConnectedStudents(examRoomId));
    }

    public int getStudyStatus(Integer examRoomId) {
        ExamRoom examRoom = examRoomRepository.findById(examRoomId).orElse(null);
        if (examRoom == null || examRoom.getExamDate() == null
                || examRoom.getStartTime() == null || examRoom.getEndTime() == null) return 0;
        LocalDate today = LocalDate.now();
        if (today.isBefore(examRoom.getExamDate())) return 0;
        if (today.isAfter(examRoom.getExamDate())) return 2;
        LocalTime now = LocalTime.now();
        if (!now.isBefore(examRoom.getStartTime()) && !now.isAfter(examRoom.getEndTime())) return 1;
        if (now.isAfter(examRoom.getEndTime())) return 2;
        return 0;
    }

    private StudentExamRoom buildStudentExamRoom(Integer examRoomId, Integer studentId) {
        StudentExamRoom ser = new StudentExamRoom();
        ser.setExamRoomId(examRoomId);
        ser.setStudentId(studentId);
        ser.setStatus(Status.ACTIVE.getValue());
        return ser;
    }

    private record PeriodRange(String periods, LocalTime startTime, LocalTime endTime) {
    }
}

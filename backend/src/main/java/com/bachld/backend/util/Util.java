package com.bachld.backend.util;

import com.bachld.backend.dto.response.SectionResponse;
import com.bachld.backend.dto.response.SubjectResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.enums.Status;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Util {

    UserRepository userRepository;

    RoleRepository roleRepository;

    TeacherRepository teacherRepository;

    StudentRepository studentRepository;

    SubjectRepository subjectRepository;

    SectionRepository sectionRepository;

    PersonalComputerRepository personalComputerRepository;

    ClassRepository classRepository;

    ScheduleRepository scheduleRepository;

    Validator validator;

    public User getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByIdAndStatus(Integer.valueOf(userId), Status.ACTIVE.getValue()).orElse(null);
    }

    public void validateEmail(String email, Integer userId) {
        Optional<User> user = userRepository.findByEmailAndStatus(email, Status.ACTIVE.getValue());
        if (user.isPresent()) {
            if (userId == null || user.get().getId() != userId) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
        }
    }

    public void validatePhone(String phone, Integer userId) {
        Optional<User> user = userRepository.findByPhoneAndStatus(phone, Status.ACTIVE.getValue());
        if (user.isPresent()) {
            if (userId == null || user.get().getId() != userId) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại");
            }
        }
    }

    public void validateRole(Integer roleId) {
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty() || role.get().getStatus() == Status.INACTIVE.getValue()) {
            throw new IllegalArgumentException("Role không hợp lệ");
        }
    }

    public void validateSection(Integer sectionId) {
        SectionResponse section = sectionRepository.findByIdAndStatus(sectionId, Status.ACTIVE.getValue());
        if (section == null) {
            throw new IllegalArgumentException("Bộ môn không hợp lệ");
        }
    }

    public void validateTeacherCode(String code, Integer teacherId) {
        Optional<Teacher> teacher = teacherRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (teacher.isPresent()) {
            if (teacherId == null || teacher.get().getId() != teacherId) {
                throw new IllegalArgumentException("Mã giảng viên đã tồn tại");
            }
        }
    }

    public void validateStudentCode(String code, Integer studentId) {
        Optional<Student> student = studentRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (student.isPresent()) {
            if (studentId == null || student.get().getId() != studentId) {
                throw new IllegalArgumentException("Mã sinh viên đã tồn tại");
            }
        }
    }

    public void validateSubjectCode(String code, Integer id) {
        Optional<Subject> subject = subjectRepository.findByCodeAndStatus(code, Status.ACTIVE.getValue());
        if (subject.isPresent()) {
            if (id == null || subject.get().getId() != id) {
                throw new IllegalArgumentException("Mã môn học đã tồn tại");
            }
        }
    }

    public void validateIpAddress(String ipAddress, Integer userId, Integer roleId) {
        Optional<PersonalComputer> pc = personalComputerRepository.findByIpAddressAndRoleId(ipAddress, roleId);

        if (pc.isPresent()) {
            if (userId == null || !pc.get().getUserId().equals(userId)) {
                throw new IllegalArgumentException("Địa chỉ IP đã tồn tại ở thiết bị khác");
            }
        }
    }

    public String getStringDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateStr, formatter).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public <T> void validateBean(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }
    }

    public void validateImportTemplate(Sheet sheet, String[] expectedHeaders) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("File không đúng định dạng template: không tìm thấy hàng tiêu đề");
        }
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            String value = cell != null ? cell.toString().trim() : "";
            if (!expectedHeaders[i].equals(value)) {
                throw new IllegalArgumentException(
                        "File không đúng định dạng template: cột " + (i + 1) + " phải là '" + expectedHeaders[i] + "'");
            }
        }
    }

    public String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        String value;
        if (cell.getCellType() == CellType.STRING) {
            value = cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            value = BigDecimal.valueOf(cell.getNumericCellValue())
                    .stripTrailingZeros()
                    .toPlainString();
        } else {
            value = cell.toString().trim();
        }
        return value.isEmpty() ? null : value;
    }

    public boolean isRowEmpty(Row row) {
        for (int c = 1; c <= 7; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kiểm tra phòng học có bị trùng lịch với lớp khác không.
     * Conflict khi: cùng roomId + khoảng ngày overlap + chia sẻ ít nhất 1 ngày trong tuần + giờ học overlap.
     *
     * @param roomId          phòng cần kiểm tra
     * @param scheduleId      ca học của lớp đang tạo/sửa
     * @param startDate       ngày bắt đầu của lớp đang tạo/sửa
     * @param endDate         ngày kết thúc của lớp đang tạo/sửa
     * @param excludeClassId  id lớp đang sửa (null nếu đang tạo mới)
     */
    public void validateRoom(Integer roomId, Integer scheduleId, LocalDate startDate, LocalDate endDate, Integer excludeClassId) {
        Schedule newSchedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (newSchedule == null || newSchedule.getDaysOfWeek() == null) return;

        Set<String> newDays = new HashSet<>(Arrays.asList(newSchedule.getDaysOfWeek().split(",")));

        List<Classes> sameRoomClasses = classRepository.findByRoomIdAndStatus(roomId, Status.ACTIVE.getValue());

        for (Classes c : sameRoomClasses) {
            if (excludeClassId != null && c.getId() == excludeClassId) continue;

            // Kiểm tra khoảng ngày có overlap không
            boolean dateOverlap = !startDate.isAfter(c.getEndDate()) && !endDate.isBefore(c.getStartDate());
            if (!dateOverlap) continue;

            Schedule otherSchedule = scheduleRepository.findById(c.getScheduleId()).orElse(null);
            if (otherSchedule == null || otherSchedule.getDaysOfWeek() == null) continue;

            Set<String> otherDays = new HashSet<>(Arrays.asList(otherSchedule.getDaysOfWeek().split(",")));

            // Kiểm tra có chung ngày trong tuần không
            boolean sharedDay = newDays.stream().anyMatch(otherDays::contains);
            if (!sharedDay) continue;

            // Kiểm tra giờ học overlap
            boolean timeOverlap = newSchedule.getStartTime().isBefore(otherSchedule.getEndTime())
                    && newSchedule.getEndTime().isAfter(otherSchedule.getStartTime());

            if (timeOverlap) {
                throw new IllegalArgumentException(
                        "Phòng học đã được sử dụng bởi lớp \"" + c.getName() + "\" trong khung giờ này"
                );
            }
        }
    }
}

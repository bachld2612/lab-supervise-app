package com.bachld.backend.service;

import com.bachld.backend.dto.request.StudentCreateRequest;
import com.bachld.backend.dto.request.StudentUpdateRequest;
import com.bachld.backend.dto.response.StudentResponse;
import com.bachld.backend.model.ManageClass;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.ManageClassRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import jakarta.transaction.Transactional;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentService {

    StudentRepository studentRepository;

    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    Util util;

    ManageClassRepository manageClassRepository;

    public Page<StudentResponse> getList(Pageable pageable, String keyword, Integer status, Integer manageClassId) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return studentRepository.findByKeyword(pageable, keyword, status, manageClassId);
    }

    public StudentResponse getById(Integer id) {
        return studentRepository.findStudentByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(StudentCreateRequest request) {
        util.validatePhone(request.getPhone(), null);
        util.validateEmail(request.getEmail(), null);
        util.validateStudentCode(request.getCode(), null);
        manageClassRepository.findClassByIdAndStatus(request.getManageClassId(), Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp quản lý có id: " + request.getManageClassId()));

        User user = new User();
        user.setEmail(request.getEmail());
        String rawPassword = "tlu" + request.getPhone().substring(request.getPhone().length() - 3);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRawPassword(rawPassword);
        user.setFullName(request.getFullName());
        user.setHometown(request.getHometown());
        user.setBirthday(LocalDate.parse(request.getBirthday()));
        user.setPhone(request.getPhone());
        user.setRoleId(Role.STUDENT.getValue());
        user.setStatus(Status.ACTIVE.getValue());

        userRepository.save(user);

        Student student = new Student();
        student.setCode(request.getCode());
        student.setManageClassId(request.getManageClassId());
        student.setUserId(user.getId());
        student.setStatus(Status.ACTIVE.getValue());
        studentRepository.save(student);
    }

    @Transactional
    public void update(StudentUpdateRequest request, int id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + id));

        User user = userRepository.findByIdAndStatus(student.getUserId(), Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có id: " + student.getUserId()));

        util.validatePhone(request.getPhone(), user.getId());
        util.validateEmail(request.getEmail(), user.getId());
        util.validateStudentCode(request.getCode(), student.getId());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }

        if (request.getCode() != null && !request.getCode().isEmpty()) {
            student.setCode(request.getCode());
        }

        if (request.getManageClassId() != null) {
            manageClassRepository.findClassByIdAndStatus(request.getManageClassId(), Status.ACTIVE.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp quản lý có id: " + request.getManageClassId()));
            student.setManageClassId(request.getManageClassId());
        }

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getHometown() != null && !request.getHometown().isEmpty()) {
            user.setHometown(request.getHometown());
        }

        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            user.setBirthday(LocalDate.parse(request.getBirthday()));
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);
        studentRepository.save(student);
    }

    public void delete(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + id));
        User user = userRepository.findById(student.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có id: " + student.getUserId()));

        user.setStatus(Status.INACTIVE.getValue());
        userRepository.save(user);
    }

    public ResponseEntity<InputStreamResource> downloadStudentImportTemplate() throws IOException {
        org.springframework.core.io.ClassPathResource file =
                new org.springframework.core.io.ClassPathResource("templates/download/student_import_template.xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_import_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(file.getInputStream()));
    }

    @Transactional
    public void importStudents(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] headers = new String[]{"STT", "Mã Sinh Viên", "Họ Tên", "Lớp quản lý", "Email", "Số điện thoại", "Ngày sinh", "Quê quán"};
            util.validateImportTemplate(sheet, headers);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || util.isRowEmpty(row)) {
                    continue;
                }

                int rowNum = i + 1;
                try {
                    String manageClassName = util.getCellStringValue(row.getCell(3));
                    if (manageClassName == null || manageClassName.isEmpty()) {
                        throw new IllegalArgumentException("Tên lớp quản lý không được để trống");
                    }

                    ManageClass manageClass = manageClassRepository
                            .findClassByNameAndStatus(manageClassName, Status.ACTIVE.getValue())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Không tìm thấy lớp quản lý: " + manageClassName));

                    StudentCreateRequest request = new StudentCreateRequest();
                    request.setCode(util.getCellStringValue(row.getCell(1)));
                    request.setFullName(util.getCellStringValue(row.getCell(2)));
                    request.setManageClassId(manageClass.getId());
                    request.setEmail(util.getCellStringValue(row.getCell(4)));
                    request.setPhone(util.getCellStringValue(row.getCell(5)));
                    String birthDay = util.getCellStringValue(row.getCell(6));
                    request.setBirthday(util.getStringDate(birthDay));
                    request.setHometown(util.getCellStringValue(row.getCell(7)));

                    util.validateBean(request);
                    create(request);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Dòng " + rowNum + ": " + e.getMessage());
                }
            }
        }
    }

}

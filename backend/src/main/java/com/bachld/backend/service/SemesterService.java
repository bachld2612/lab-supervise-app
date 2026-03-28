package com.bachld.backend.service;

import com.bachld.backend.dto.request.SemesterCreateRequest;
import com.bachld.backend.dto.request.SemesterUpdateRequest;
import com.bachld.backend.dto.response.SemesterResponse;
import com.bachld.backend.model.Semester;
import com.bachld.backend.repository.SemesterRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemesterService {

    SemesterRepository semesterRepository;

    public Page<SemesterResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }

        return semesterRepository.findByKeyword(pageable, keyword, status);
    }

    public SemesterResponse getById(Integer id) {
        return semesterRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(SemesterCreateRequest request) {
        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Semester semester = new Semester();
        semester.setName(request.getName());
        semester.setStudyYear(request.getStudyYear());
        semester.setStartDate(start);
        semester.setEndDate(end);
        semester.setStatus(Status.ACTIVE.getValue());

        semesterRepository.save(semester);
    }

    @Transactional
    public void update(SemesterUpdateRequest request, int id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học kỳ có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            semester.setName(request.getName());
        }

        if (request.getStudyYear() != null && !request.getStudyYear().isEmpty()) {
            semester.setStudyYear(request.getStudyYear());
        }

        LocalDate newStart = request.getStartDate() != null && !request.getStartDate().isEmpty() 
            ? LocalDate.parse(request.getStartDate()) 
            : semester.getStartDate();
            
        LocalDate newEnd = request.getEndDate() != null && !request.getEndDate().isEmpty() 
            ? LocalDate.parse(request.getEndDate()) 
            : semester.getEndDate();

        if (newStart != null && newEnd != null && newStart.isAfter(newEnd)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        semester.setStartDate(newStart);
        semester.setEndDate(newEnd);

        semesterRepository.save(semester);
    }
}

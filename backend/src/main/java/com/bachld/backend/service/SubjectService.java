package com.bachld.backend.service;

import com.bachld.backend.dto.request.SubjectCreateRequest;
import com.bachld.backend.dto.request.SubjectUpdateRequest;
import com.bachld.backend.dto.response.SectionResponse;
import com.bachld.backend.dto.response.SubjectResponse;
import com.bachld.backend.model.Subject;
import com.bachld.backend.repository.SectionRepository;
import com.bachld.backend.repository.SubjectRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
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
public class SubjectService {

  SubjectRepository subjectRepository;

  SectionRepository sectionRepository;

  Util util;

  public Page<SubjectResponse> getList(Pageable pageable, String keyword, Integer status) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }

    return subjectRepository.findByKeyword(pageable, keyword, status);
  }

  public SubjectResponse getById(Integer id) {
    return subjectRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
  }

  @Transactional
  public void create(SubjectCreateRequest request) {
    util.validateSubjectCode(request.getCode(), null);

    SectionResponse section =
        sectionRepository.findByIdAndStatus(request.getSectionId(), Status.ACTIVE.getValue());
    if (section == null) {
      throw new IllegalArgumentException("Không tìm thấy bộ môn có id: " + request.getSectionId());
    }

    Subject subject = new Subject();
    subject.setName(request.getName());
    subject.setCode(request.getCode());
    subject.setCreditNumber(request.getCreditNumber());
    subject.setSectionId(request.getSectionId());
    subject.setStatus(Status.ACTIVE.getValue());

    subjectRepository.save(subject);
  }

  @Transactional
  public void update(SubjectUpdateRequest request, int id) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học có id: " + id));

    if (request.getCode() != null && !request.getCode().isEmpty()) {
      util.validateSubjectCode(request.getCode(), id);
      subject.setCode(request.getCode());
    }

    if (request.getName() != null && !request.getName().isEmpty()) {
      subject.setName(request.getName());
    }

    if (request.getCreditNumber() != null) {
      subject.setCreditNumber(request.getCreditNumber());
    }

    if (request.getSectionId() != null) {
      SectionResponse section =
          sectionRepository.findByIdAndStatus(request.getSectionId(), Status.ACTIVE.getValue());

      if (section == null) {
        throw new IllegalArgumentException(
            "Không tìm thấy bộ môn có id: " + request.getSectionId());
      }

      subject.setSectionId(request.getSectionId());
    }

    subjectRepository.save(subject);
  }

  public void deleteById(Integer id) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học có id: " + id));

    subject.setStatus(Status.INACTIVE.getValue());
    subjectRepository.save(subject);
  }
}

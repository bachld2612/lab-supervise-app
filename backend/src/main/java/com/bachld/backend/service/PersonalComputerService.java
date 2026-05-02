package com.bachld.backend.service;

import com.bachld.backend.dto.request.PersonalComputerUpdateRequest;
import com.bachld.backend.dto.response.PersonalComputerResponse;
import com.bachld.backend.model.PersonalComputer;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.PersonalComputerRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalComputerService {

    PersonalComputerRepository personalComputerRepository;

    Util util;

    public void update(PersonalComputerUpdateRequest request) {
        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }

        Integer userId = currentUser.getId();
        
        util.validateIpAddress(request.getIpAddress(), userId, currentUser.getRoleId());

        PersonalComputer pc = personalComputerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PersonalComputer newPc = new PersonalComputer();
                    newPc.setUserId(userId);
                    newPc.setStatus(Status.ACTIVE.getValue());
                    return newPc;
                });

        pc.setIpAddress(request.getIpAddress());
        personalComputerRepository.save(pc);
    }

    public PersonalComputerResponse getByUserId() {
        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }

        return personalComputerRepository.findByUserId(currentUser.getId())
                .map(pc -> new PersonalComputerResponse(pc.getIpAddress(), pc.getUserId()))
                .orElse(null);
    }
}

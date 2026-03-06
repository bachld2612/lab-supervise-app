package com.bachld.backend.util;

import com.bachld.backend.model.User;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Util {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByIdAndStatus(Integer.valueOf(userId), Status.ACTIVE.getValue()).orElse(null);
    }

}

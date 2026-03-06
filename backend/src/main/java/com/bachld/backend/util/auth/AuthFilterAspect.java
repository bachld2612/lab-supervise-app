package com.bachld.backend.util.auth;

import com.bachld.backend.model.Role;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.RoleRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class AuthFilterAspect {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private Util util;

    @Around("@annotation(AuthFilter)")
    public Object authFilterExecute(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        AuthFilter authFilter = signature.getMethod().getAnnotation(AuthFilter.class);

        User currentUser = util.getCurrentUser();
        Role role = roleRepository.findByIdAndStatus(currentUser.getRoleId(), Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại hoặc đã bị xoá"));

        List<String> roles = Arrays.asList(authFilter.role().split(","));

        if (roles.contains(role.getName())) {
            return pjp.proceed();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

}

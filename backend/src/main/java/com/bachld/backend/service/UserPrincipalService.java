package com.bachld.backend.service;


import com.bachld.backend.model.User;
import com.bachld.backend.repository.UserRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserPrincipalService implements UserDetailsService {

    UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Optional<User> userInfo = repository.findByIdAndStatus(Integer.valueOf(id), Status.ACTIVE.getValue());

        if (userInfo.isEmpty()) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }

        User user = userInfo.get();
        HashSet<GrantedAuthority> authorities = new HashSet<>();
        return new org.springframework.security.core.userdetails.User(String.valueOf(user.getId()), user.getPassword(), authorities);
    }
}
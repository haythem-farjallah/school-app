package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.dto.UserProfileUpdateRequest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.mapper.UserMapper;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    private BaseUser currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userDetailsService.findBaseUserByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile() {
        BaseUser user = currentUser();
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateCurrentUserProfile(UserProfileUpdateRequest request) {
        BaseUser user = currentUser();
        
        // Update only allowed fields: telephone and address
        user.setTelephone(request.getTelephone());
        user.setAddress(request.getAddress());
        
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }
}

package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final AdministrationRepository adminRepo;
    private final WorkerRepository workerRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        BaseUser user = findBaseUserByEmail(email);
        var authorities = user.getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getName()))
                .toList();
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), authorities);
    }

    public BaseUser findBaseUserByEmail(String email) {
        return Stream.of(
                        studentRepo.findByEmail(email),
                        teacherRepo.findByEmail(email),
                        parentRepo.findByEmail(email),
                        adminRepo.findByEmail(email),
                        workerRepo.findByEmail(email)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
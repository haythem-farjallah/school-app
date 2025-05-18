package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.BaseUser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public interface UserRepository extends BaseUserRepository<BaseUser> { }
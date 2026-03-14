package com.smarthire.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.auth.dto.request.LoginRequest;
import com.smarthire.modules.auth.dto.request.RegisterRequest;
import com.smarthire.modules.auth.dto.response.AuthResponse;
import com.smarthire.modules.auth.dto.response.UserProfileResponse;
import com.smarthire.modules.auth.entity.RoleEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.entity.UserRoleEntity;
import com.smarthire.modules.auth.mapper.RoleMapper;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.mapper.UserRoleMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.auth.service.AuthService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_CANDIDATE = "CANDIDATE";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
        UserMapper userMapper,
        RoleMapper roleMapper,
        UserRoleMapper userRoleMapper,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        UserEntity existingUser = userMapper.selectOne(
            Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, request.email())
        );
        if (existingUser != null) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        RoleEntity candidateRole = roleMapper.selectOne(
            Wrappers.<RoleEntity>lambdaQuery().eq(RoleEntity::getCode, ROLE_CANDIDATE)
        );
        if (candidateRole == null) {
            throw new BusinessException("ROLE_NOT_INITIALIZED", "Candidate role is not initialized");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setStatus(STATUS_ACTIVE);
        userMapper.insert(user);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(candidateRole.getId());
        userRoleMapper.insert(userRole);

        List<String> roleCodes = List.of(candidateRole.getCode());
        return buildAuthResponse(user, roleCodes);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(
            Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, request.email())
        );

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("USER_DISABLED", "User is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return buildAuthResponse(user, loadRoleCodes(user.getId()));
    }

    private AuthResponse buildAuthResponse(UserEntity user, List<String> roleCodes) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            roleCodes
        );

        return new AuthResponse(
            jwtTokenService.generateToken(authenticatedUser),
            "Bearer",
            jwtTokenService.getAccessTokenExpirationSeconds(),
            UserProfileResponse.fromUserEntity(user, roleCodes)
        );
    }

    private List<String> loadRoleCodes(Long userId) {
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
            Wrappers.<UserRoleEntity>lambdaQuery().eq(UserRoleEntity::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            throw new BusinessException("ROLE_NOT_ASSIGNED", "User does not have any assigned role");
        }

        List<Long> roleIds = userRoles.stream()
            .map(UserRoleEntity::getRoleId)
            .distinct()
            .toList();

        List<RoleEntity> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.isEmpty()) {
            throw new BusinessException("ROLE_NOT_FOUND", "Assigned role records were not found");
        }

        return roles.stream()
            .map(RoleEntity::getCode)
            .distinct()
            .sorted()
            .toList();
    }
}

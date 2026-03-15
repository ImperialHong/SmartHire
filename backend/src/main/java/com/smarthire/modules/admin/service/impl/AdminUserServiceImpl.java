package com.smarthire.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.admin.dto.request.AdminUserListRequest;
import com.smarthire.modules.admin.dto.request.AdminUserStatusUpdateRequest;
import com.smarthire.modules.admin.dto.response.AdminUserResponse;
import com.smarthire.modules.admin.service.AdminUserService;
import com.smarthire.modules.auth.entity.RoleEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.entity.UserRoleEntity;
import com.smarthire.modules.auth.mapper.RoleMapper;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.mapper.UserRoleMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public AdminUserServiceImpl(
        UserMapper userMapper,
        UserRoleMapper userRoleMapper,
        RoleMapper roleMapper
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public PageResponse<AdminUserResponse> listUsers(AuthenticatedUser currentUser, AdminUserListRequest request) {
        validateAdminRole(currentUser);

        List<Long> roleFilteredUserIds = resolveRoleFilteredUserIds(request.roleCode());
        if (roleFilteredUserIds != null && roleFilteredUserIds.isEmpty()) {
            return PageResponse.of(List.of(), request.page(), request.size(), 0);
        }

        var query = Wrappers.<UserEntity>lambdaQuery()
            .orderByDesc(UserEntity::getCreatedAt);

        if (StringUtils.hasText(request.keyword())) {
            String keyword = request.keyword().trim();
            query.and(wrapper -> wrapper
                .like(UserEntity::getEmail, keyword)
                .or()
                .like(UserEntity::getFullName, keyword)
            );
        }

        if (StringUtils.hasText(request.status())) {
            query.eq(UserEntity::getStatus, normalizeStatus(request.status()));
        }

        if (roleFilteredUserIds != null) {
            query.in(UserEntity::getId, roleFilteredUserIds);
        }

        Page<UserEntity> page = userMapper.selectPage(new Page<>(request.page(), request.size()), query);
        Map<Long, List<String>> rolesByUserId = loadRoleCodesByUserId(page.getRecords().stream()
            .map(UserEntity::getId)
            .toList());

        List<AdminUserResponse> records = page.getRecords().stream()
            .map(user -> AdminUserResponse.fromUserEntity(
                user,
                rolesByUserId.getOrDefault(user.getId(), List.of())
            ))
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public AdminUserResponse updateUserStatus(
        AuthenticatedUser currentUser,
        Long userId,
        AdminUserStatusUpdateRequest request
    ) {
        validateAdminRole(currentUser);

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "User was not found");
        }

        String normalizedStatus = normalizeStatus(request.status());
        if (currentUser.userId().equals(userId) && "DISABLED".equals(normalizedStatus)) {
            throw new BusinessException(
                "SELF_DISABLE_NOT_ALLOWED",
                "Admin cannot disable the current logged-in account"
            );
        }

        user.setStatus(normalizedStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        Map<Long, List<String>> rolesByUserId = loadRoleCodesByUserId(List.of(userId));
        return AdminUserResponse.fromUserEntity(user, rolesByUserId.getOrDefault(userId, List.of()));
    }

    private List<Long> resolveRoleFilteredUserIds(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }

        RoleEntity role = roleMapper.selectOne(
            Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getCode, roleCode.trim().toUpperCase())
        );
        if (role == null) {
            throw new BusinessException("INVALID_ROLE_CODE", "Invalid role code");
        }

        return userRoleMapper.selectList(
            Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getRoleId, role.getId())
        ).stream()
            .map(UserRoleEntity::getUserId)
            .distinct()
            .toList();
    }

    private Map<Long, List<String>> loadRoleCodesByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
            Wrappers.<UserRoleEntity>lambdaQuery()
                .in(UserRoleEntity::getUserId, userIds)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> roleIds = userRoles.stream()
            .map(UserRoleEntity::getRoleId)
            .distinct()
            .toList();
        Map<Long, RoleEntity> rolesById = roleMapper.selectBatchIds(roleIds).stream()
            .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));

        Map<Long, List<String>> rolesByUserId = new LinkedHashMap<>();
        for (UserRoleEntity userRole : userRoles) {
            RoleEntity role = rolesById.get(userRole.getRoleId());
            if (role == null) {
                continue;
            }
            rolesByUserId.computeIfAbsent(userRole.getUserId(), ignored -> new java.util.ArrayList<>())
                .add(role.getCode());
        }

        rolesByUserId.replaceAll((ignored, roles) -> roles.stream().distinct().sorted().toList());
        return rolesByUserId;
    }

    private String normalizeStatus(String rawStatus) {
        String normalizedStatus = rawStatus.trim().toUpperCase();
        if (!USER_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException("INVALID_USER_STATUS", "Invalid user status");
        }
        return normalizedStatus;
    }

    private void validateAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only admin can manage users");
        }
    }
}

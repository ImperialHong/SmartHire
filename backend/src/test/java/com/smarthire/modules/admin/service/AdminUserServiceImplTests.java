package com.smarthire.modules.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.admin.dto.request.AdminUserListRequest;
import com.smarthire.modules.admin.dto.request.AdminUserStatusUpdateRequest;
import com.smarthire.modules.admin.dto.response.AdminUserResponse;
import com.smarthire.modules.admin.service.impl.AdminUserServiceImpl;
import com.smarthire.modules.auth.entity.RoleEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.entity.UserRoleEntity;
import com.smarthire.modules.auth.mapper.RoleMapper;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.mapper.UserRoleMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void listUsersShouldReturnPagedUsersWithRoles() {
        AuthenticatedUser currentUser = new AuthenticatedUser(99L, "admin@example.com", "Admin", List.of("ADMIN"));

        doAnswer(invocation -> {
            Page<UserEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(
                buildUser(1L, "candidate@example.com", "Candidate User", "ACTIVE"),
                buildUser(2L, "hr@example.com", "HR User", "DISABLED")
            ));
            page.setTotal(2);
            return page;
        }).when(userMapper).selectPage(any(Page.class), any());

        when(userRoleMapper.selectList(any())).thenReturn(List.of(
            buildUserRole(1L, 100L),
            buildUserRole(2L, 101L)
        ));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(
            buildRole(100L, "CANDIDATE"),
            buildRole(101L, "HR")
        ));

        PageResponse<AdminUserResponse> response = adminUserService.listUsers(
            currentUser,
            new AdminUserListRequest(1, 10, null, null, null)
        );

        assertEquals(2L, response.total());
        assertEquals("candidate@example.com", response.records().getFirst().email());
        assertEquals(List.of("CANDIDATE"), response.records().getFirst().roles());
        assertEquals("DISABLED", response.records().get(1).status());
        assertEquals(List.of("HR"), response.records().get(1).roles());
    }

    @Test
    void updateUserStatusShouldRejectSelfDisable() {
        AuthenticatedUser currentUser = new AuthenticatedUser(99L, "admin@example.com", "Admin", List.of("ADMIN"));
        when(userMapper.selectById(99L)).thenReturn(buildUser(99L, "admin@example.com", "Admin", "ACTIVE"));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.updateUserStatus(
                currentUser,
                99L,
                new AdminUserStatusUpdateRequest("DISABLED")
            )
        );

        assertEquals("SELF_DISABLE_NOT_ALLOWED", exception.getCode());
    }

    @Test
    void updateUserStatusShouldReturnUpdatedUser() {
        AuthenticatedUser currentUser = new AuthenticatedUser(99L, "admin@example.com", "Admin", List.of("ADMIN"));
        UserEntity hrUser = buildUser(2L, "hr@example.com", "HR User", "ACTIVE");
        when(userMapper.selectById(2L)).thenReturn(hrUser);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(buildUserRole(2L, 101L)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(buildRole(101L, "HR")));

        AdminUserResponse response = adminUserService.updateUserStatus(
            currentUser,
            2L,
            new AdminUserStatusUpdateRequest("disabled")
        );

        assertEquals(2L, response.id());
        assertEquals("DISABLED", response.status());
        assertEquals(List.of("HR"), response.roles());
    }

    @Test
    void listUsersShouldRejectNonAdminUser() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "hr@example.com", "HR", List.of("HR"));

        assertThrows(
            AccessDeniedException.class,
            () -> adminUserService.listUsers(currentUser, new AdminUserListRequest(1, 10, null, null, null))
        );
        verifyNoInteractions(userMapper, userRoleMapper, roleMapper);
    }

    private UserEntity buildUser(Long id, String email, String fullName, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        return user;
    }

    private UserRoleEntity buildUserRole(Long userId, Long roleId) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private RoleEntity buildRole(Long id, String code) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setCode(code);
        return role;
    }
}

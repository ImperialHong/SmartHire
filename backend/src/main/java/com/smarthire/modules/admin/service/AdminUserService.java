package com.smarthire.modules.admin.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminUserListRequest;
import com.smarthire.modules.admin.dto.request.AdminUserStatusUpdateRequest;
import com.smarthire.modules.admin.dto.response.AdminUserResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;

public interface AdminUserService {

    PageResponse<AdminUserResponse> listUsers(AuthenticatedUser currentUser, AdminUserListRequest request);

    AdminUserResponse updateUserStatus(
        AuthenticatedUser currentUser,
        Long userId,
        AdminUserStatusUpdateRequest request
    );
}

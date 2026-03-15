package com.smarthire.modules.admin.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminUserListRequest;
import com.smarthire.modules.admin.dto.request.AdminUserStatusUpdateRequest;
import com.smarthire.modules.admin.dto.response.AdminUserResponse;
import com.smarthire.modules.admin.service.AdminUserService;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Admin Users", description = "Lightweight admin user management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "List users for admin management")
    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 100, message = "size must be less than or equal to 100") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String roleCode
    ) {
        return ApiResponse.success(adminUserService.listUsers(
            currentUser,
            new AdminUserListRequest(page, size, keyword, status, roleCode)
        ));
    }

    @Operation(summary = "Update a user's status")
    @PatchMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.success(
            "User status updated successfully",
            adminUserService.updateUserStatus(currentUser, id, request)
        );
    }
}

package com.smarthire.modules.admin.dto.response;

import com.smarthire.modules.auth.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    String status,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> roles
) {

    public static AdminUserResponse fromUserEntity(UserEntity user, List<String> roles) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            roles
        );
    }
}

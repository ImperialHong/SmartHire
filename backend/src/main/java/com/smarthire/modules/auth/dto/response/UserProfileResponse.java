package com.smarthire.modules.auth.dto.response;

import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import java.util.List;

public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    List<String> roles
) {

    public static UserProfileResponse fromUserEntity(UserEntity user, List<String> roles) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            roles
        );
    }

    public static UserProfileResponse fromAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        return new UserProfileResponse(
            authenticatedUser.userId(),
            authenticatedUser.email(),
            authenticatedUser.fullName(),
            null,
            authenticatedUser.roles()
        );
    }
}

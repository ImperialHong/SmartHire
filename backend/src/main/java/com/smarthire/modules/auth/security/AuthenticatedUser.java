package com.smarthire.modules.auth.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
    Long userId,
    String email,
    String fullName,
    List<String> roles
) implements Serializable {

    public Collection<? extends GrantedAuthority> authorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }
}

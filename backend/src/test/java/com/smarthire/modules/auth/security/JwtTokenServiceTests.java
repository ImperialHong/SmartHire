package com.smarthire.modules.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTests {

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("smarthire-test-secret-key-at-least-32-bytes");
        jwtProperties.setAccessTokenExpirationSeconds(3600);

        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);
        AuthenticatedUser originalUser = new AuthenticatedUser(
            100L,
            "jane@example.com",
            "Jane Doe",
            List.of("CANDIDATE")
        );

        String token = jwtTokenService.generateToken(originalUser);
        AuthenticatedUser parsedUser = jwtTokenService.parseToken(token);

        assertEquals(originalUser.userId(), parsedUser.userId());
        assertEquals(originalUser.email(), parsedUser.email());
        assertEquals(originalUser.fullName(), parsedUser.fullName());
        assertEquals(originalUser.roles(), parsedUser.roles());
    }
}

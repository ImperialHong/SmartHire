package com.smarthire.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.modules.auth.dto.request.LoginRequest;
import com.smarthire.modules.auth.dto.request.RegisterRequest;
import com.smarthire.modules.auth.dto.response.AuthResponse;
import com.smarthire.modules.auth.entity.RoleEntity;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.entity.UserRoleEntity;
import com.smarthire.modules.auth.mapper.RoleMapper;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.mapper.UserRoleMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.auth.security.JwtProperties;
import com.smarthire.modules.auth.security.JwtTokenService;
import com.smarthire.modules.auth.service.impl.AuthServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("smarthire-test-secret-key-at-least-32-bytes");
        jwtProperties.setAccessTokenExpirationSeconds(3600);

        jwtTokenService = new JwtTokenService(jwtProperties);
        authService = new AuthServiceImpl(
            userMapper,
            roleMapper,
            userRoleMapper,
            passwordEncoder,
            jwtTokenService
        );
    }

    @Test
    void registerShouldCreateCandidateAccountAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
            "jane@example.com",
            "Password123",
            "Jane Doe",
            "123456789"
        );

        RoleEntity candidateRole = new RoleEntity();
        candidateRole.setId(1L);
        candidateRole.setCode("CANDIDATE");
        candidateRole.setName("Candidate");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(roleMapper.selectOne(any())).thenReturn(candidateRole);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        AuthResponse response = authService.register(request);
        AuthenticatedUser parsedUser = jwtTokenService.parseToken(response.accessToken());

        assertFalse(response.accessToken().isBlank());
        assertEquals("jane@example.com", response.user().email());
        assertEquals(List.of("CANDIDATE"), response.user().roles());
        assertEquals(100L, parsedUser.userId());
        assertEquals("jane@example.com", parsedUser.email());
        verify(userRoleMapper).insert(any(UserRoleEntity.class));
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("jane@example.com", "Password123");

        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setEmail("jane@example.com");
        user.setPasswordHash("encoded-password");
        user.setFullName("Jane Doe");
        user.setPhone("123456789");
        user.setStatus("ACTIVE");

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(100L);
        userRole.setRoleId(1L);

        RoleEntity candidateRole = new RoleEntity();
        candidateRole.setId(1L);
        candidateRole.setCode("CANDIDATE");
        candidateRole.setName("Candidate");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(candidateRole));

        AuthResponse response = authService.login(request);
        AuthenticatedUser parsedUser = jwtTokenService.parseToken(response.accessToken());

        assertFalse(response.accessToken().isBlank());
        assertEquals("Jane Doe", response.user().fullName());
        assertEquals(List.of("CANDIDATE"), response.user().roles());
        assertEquals(100L, parsedUser.userId());
        assertEquals("jane@example.com", parsedUser.email());
        verify(userMapper).updateById(any(UserEntity.class));
    }
}

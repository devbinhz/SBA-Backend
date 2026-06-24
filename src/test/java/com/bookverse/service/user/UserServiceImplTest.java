package com.bookverse.service.user;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.entity.User;
import com.bookverse.mapper.UserMapper;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.auth.RefreshTokenService;
import com.bookverse.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(RefreshTokenService.class);
        userService = new UserServiceImpl(userRepository, passwordEncoder, refreshTokenService, new UserMapper(new ModelMapper()));
    }

    @Test
    void changePasswordRevokesAllRefreshTokens() {
        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .passwordHash("old-hash")
                .fullName("User")
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("new-hash");

        userService.changePassword(10L, passwordRequest("OldPass123", "NewPass123"));

        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(10L);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .passwordHash("old-hash")
                .fullName("User")
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(10L, passwordRequest("wrong", "NewPass123")))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(user);
        verify(refreshTokenService, never()).revokeAllUserTokens(10L);
    }

    @Test
    void setEnabledBlocksSelfDisable() {
        assertThatThrownBy(() -> userService.setEnabled(1L, 1L, false))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findById(1L);
    }

    @Test
    void disablingUserRevokesRefreshTokens() {
        User user = User.builder()
                .id(2L)
                .email("customer@example.com")
                .fullName("Customer")
                .enabled(true)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setEnabled(1L, 2L, false);

        verify(refreshTokenService).revokeAllUserTokens(2L);
    }

    private com.bookverse.dto.request.user.ChangePasswordRequestDTO passwordRequest(String currentPassword, String newPassword) {
        com.bookverse.dto.request.user.ChangePasswordRequestDTO request = new com.bookverse.dto.request.user.ChangePasswordRequestDTO();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }
}

package com.bookverse.config;

import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsAdminAccountWhenMissing() {
        when(userRepository.existsByEmailIgnoreCase("admin@bookverse.local")).thenReturn(false);
        when(passwordEncoder.encode("ChangeMe123!")).thenReturn("hashed-password");

        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder,
                new BookverseAdminProperties(" admin@bookverse.local ", "ChangeMe123!", "BookVerse Admin"));

        seeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@bookverse.local");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getFullName()).isEqualTo("BookVerse Admin");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailVerified()).isTrue();
    }

    @Test
    void skipsSeedingWhenAccountAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("admin@bookverse.local")).thenReturn(true);

        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder,
                new BookverseAdminProperties("admin@bookverse.local", "ChangeMe123!", "BookVerse Admin"));

        seeder.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}


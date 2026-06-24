package com.bookverse.service.user.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.user.ChangePasswordRequestDTO;
import com.bookverse.dto.request.user.UpdateProfileRequestDTO;
import com.bookverse.dto.response.user.UserResponseDTO;
import com.bookverse.entity.User;
import com.bookverse.mapper.UserMapper;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.auth.RefreshTokenService;
import com.bookverse.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getProfile(Long userId) {
        return userMapper.toResponse(getUser(userId));
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(Long userId, UpdateProfileRequestDTO request) {
        User user = getUser(userId);
        user.setFullName(request.getFullName().trim());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> listUsers(Pageable pageable) {
        Page<UserResponseDTO> page = userRepository.findAll(pageable).map(userMapper::toResponse);
        return PageResponseDTO.from(page);
    }

    @Override
    @Transactional
    public UserResponseDTO setEnabled(Long adminUserId, Long targetUserId, boolean enabled) {
        if (adminUserId.equals(targetUserId) && !enabled) {
            throw new BadRequestException("Admin cannot disable own account");
        }

        User user = getUser(targetUserId);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        if (!enabled) {
            refreshTokenService.revokeAllUserTokens(saved.getId());
        }
        return userMapper.toResponse(saved);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

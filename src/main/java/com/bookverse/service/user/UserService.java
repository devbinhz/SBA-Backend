package com.bookverse.service.user;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.user.ChangePasswordRequestDTO;
import com.bookverse.dto.request.user.UpdateProfileRequestDTO;
import com.bookverse.dto.response.user.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDTO getProfile(Long userId);

    UserResponseDTO updateProfile(Long userId, UpdateProfileRequestDTO request);

    void changePassword(Long userId, ChangePasswordRequestDTO request);

    PageResponseDTO<UserResponseDTO> listUsers(Pageable pageable);

    UserResponseDTO setEnabled(Long adminUserId, Long targetUserId, boolean enabled);
}

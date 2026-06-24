package com.bookverse.mapper;

import com.bookverse.dto.response.user.UserResponseDTO;
import com.bookverse.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserResponseDTO toResponse(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }
}

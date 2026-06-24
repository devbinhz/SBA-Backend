package com.bookverse.mapper;

import com.bookverse.dto.request.address.AddressRequestDTO;
import com.bookverse.dto.response.address.AddressResponseDTO;
import com.bookverse.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponseDTO toResponse(Address address) {
        return AddressResponseDTO.builder()
                .id(address.getId())
                .recipient(address.getRecipient())
                .phone(address.getPhone())
                .line(address.getLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .isDefault(address.isDefaultAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public void updateEntity(Address address, AddressRequestDTO request) {
        address.setRecipient(request.getRecipient().trim());
        address.setPhone(request.getPhone().trim());
        address.setLine(request.getLine().trim());
        address.setWard(trimToNull(request.getWard()));
        address.setDistrict(trimToNull(request.getDistrict()));
        address.setCity(request.getCity().trim());
        address.setDefaultAddress(request.isDefault());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

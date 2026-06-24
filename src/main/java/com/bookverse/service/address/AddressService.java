package com.bookverse.service.address;

import com.bookverse.dto.request.address.AddressRequestDTO;
import com.bookverse.dto.response.address.AddressResponseDTO;

import java.util.List;

public interface AddressService {

    List<AddressResponseDTO> listAddresses(Long userId);

    AddressResponseDTO createAddress(Long userId, AddressRequestDTO request);

    AddressResponseDTO updateAddress(Long userId, Long addressId, AddressRequestDTO request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponseDTO setDefaultAddress(Long userId, Long addressId);
}

package com.bookverse.service.address.impl;

import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.address.AddressRequestDTO;
import com.bookverse.dto.response.address.AddressResponseDTO;
import com.bookverse.entity.Address;
import com.bookverse.entity.User;
import com.bookverse.mapper.AddressMapper;
import com.bookverse.repository.AddressRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.address.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDTO> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponseDTO createAddress(Long userId, AddressRequestDTO request) {
        User user = getUser(userId);
        boolean shouldBeDefault = request.isDefault() || !addressRepository.existsByUserId(userId);
        if (shouldBeDefault) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = Address.builder()
                .user(user)
                .build();
        addressMapper.updateEntity(address, request);
        address.setDefaultAddress(shouldBeDefault);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponseDTO updateAddress(Long userId, Long addressId, AddressRequestDTO request) {
        Address address = getOwnedAddress(userId, addressId);
        if (request.isDefault()) {
            addressRepository.clearDefaultByUserIdExcept(userId, addressId);
        }

        addressMapper.updateEntity(address, request);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = getOwnedAddress(userId, addressId);
        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponseDTO setDefaultAddress(Long userId, Long addressId) {
        Address address = getOwnedAddress(userId, addressId);
        addressRepository.clearDefaultByUserId(userId);
        address.setDefaultAddress(true);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Address getOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}

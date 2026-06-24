package com.bookverse.service.address;

import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.address.AddressRequestDTO;
import com.bookverse.entity.Address;
import com.bookverse.entity.User;
import com.bookverse.mapper.AddressMapper;
import com.bookverse.repository.AddressRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.address.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    private AddressRepository addressRepository;
    private UserRepository userRepository;
    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        addressRepository = mock(AddressRepository.class);
        userRepository = mock(UserRepository.class);
        addressService = new AddressServiceImpl(addressRepository, userRepository, new AddressMapper());
    }

    @Test
    void firstAddressBecomesDefaultEvenWhenRequestDoesNotAskForDefault() {
        User user = User.builder().id(1L).email("user@example.com").fullName("User").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.existsByUserId(1L)).thenReturn(false);
        when(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = addressService.createAddress(1L, addressRequest(false));

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).clearDefaultByUserId(1L);
    }

    @Test
    void setDefaultAddressChecksOwnerAndClearsExistingDefault() {
        User user = User.builder().id(1L).email("user@example.com").fullName("User").build();
        Address address = Address.builder()
                .id(5L)
                .user(user)
                .recipient("Recipient")
                .phone("0900000000")
                .line("123 Street")
                .city("Ho Chi Minh")
                .defaultAddress(false)
                .build();
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);

        var response = addressService.setDefaultAddress(1L, 5L);

        verify(addressRepository).clearDefaultByUserId(1L);
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void updateAddressRejectsForeignAddress() {
        when(addressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(1L, 99L, addressRequest(false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private AddressRequestDTO addressRequest(boolean isDefault) {
        AddressRequestDTO request = new AddressRequestDTO();
        request.setRecipient(" Recipient ");
        request.setPhone("0900000000");
        request.setLine(" 123 Street ");
        request.setCity(" Ho Chi Minh ");
        request.setDefault(isDefault);
        return request;
    }
}

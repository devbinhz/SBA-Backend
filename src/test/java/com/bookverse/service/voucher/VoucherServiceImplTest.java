package com.bookverse.service.voucher;

import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import com.bookverse.enums.DiscountType;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.mapper.VoucherMapper;
import com.bookverse.repository.UserRepository;
import com.bookverse.repository.UserVoucherRepository;
import com.bookverse.repository.VoucherRepository;
import com.bookverse.service.voucher.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class VoucherServiceImplTest {

    private VoucherRepository voucherRepository;
    private UserVoucherRepository userVoucherRepository;
    private UserRepository userRepository;
    private VoucherServiceImpl voucherService;

    @BeforeEach
    void setUp() {
        voucherRepository = mock(VoucherRepository.class);
        userVoucherRepository = mock(UserVoucherRepository.class);
        userRepository = mock(UserRepository.class);
        voucherService = new VoucherServiceImpl(
                voucherRepository,
                userVoucherRepository,
                userRepository,
                new VoucherMapper()
        );
    }

    @Test
    void getMyVouchersReturnsOnlyRepositoryEligibleVouchers() {
        var pageable = PageRequest.of(0, 20);
        UserVoucher userVoucher = UserVoucher.builder()
                .id(11L)
                .voucher(voucher(2L, "T2", DiscountType.FIXED, 20_000L, 200_000L))
                .code("T2-DEMO-0001")
                .status(VoucherStatus.UNUSED)
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .build();
        when(userVoucherRepository.findByUserIdAndStatusAndExpiresAtGreaterThan(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(VoucherStatus.UNUSED),
                any(Instant.class),
                org.mockito.ArgumentMatchers.eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(userVoucher), pageable, 1));

        var result = voucherService.getMyVouchers(7L, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().getCode()).isEqualTo("T2-DEMO-0001");
        assertThat(result.items().getFirst().getDiscountValue()).isEqualTo(20_000L);
    }

    @Test
    void awardVoucherCreatesUnusedVoucherWithFortyEightHourExpiry() {
        User user = User.builder().id(7L).email("customer@example.com").build();
        Voucher eligible = voucher(3L, "T3", DiscountType.PERCENTAGE, 10L, 300_000L);
        when(voucherRepository.findByActiveTrueAndTierMinAmountLessThanEqualOrderByTierMinAmountDesc(500_000L))
                .thenReturn(List.of(eligible));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        voucherService.awardVoucherToUser(7L, 500_000L);

        var captor = org.mockito.ArgumentCaptor.forClass(UserVoucher.class);
        verify(userVoucherRepository).save(captor.capture());
        UserVoucher awarded = captor.getValue();
        assertThat(awarded.getUser()).isSameAs(user);
        assertThat(awarded.getVoucher()).isSameAs(eligible);
        assertThat(awarded.getStatus()).isEqualTo(VoucherStatus.UNUSED);
        assertThat(awarded.getCode()).startsWith("T3-");
        assertThat(awarded.getExpiresAt()).isBetween(
                Instant.now().plus(Duration.ofHours(47)),
                Instant.now().plus(Duration.ofHours(49))
        );
    }

    @Test
    void awardVoucherDoesNothingWhenNoRuleIsEligible() {
        when(voucherRepository.findByActiveTrueAndTierMinAmountLessThanEqualOrderByTierMinAmountDesc(50_000L))
                .thenReturn(List.of());

        voucherService.awardVoucherToUser(7L, 50_000L);

        verify(userRepository, never()).findById(any());
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void awardVoucherDoesNotCreateOrphanWhenUserIsMissing() {
        Voucher eligible = voucher(1L, "T1", DiscountType.FIXED, 10_000L, 100_000L);
        when(voucherRepository.findByActiveTrueAndTierMinAmountLessThanEqualOrderByTierMinAmountDesc(100_000L))
                .thenReturn(List.of(eligible));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        voucherService.awardVoucherToUser(99L, 100_000L);

        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void updateVoucherChangesTheExistingRule() {
        Voucher existing = voucher(1L, "OLD", DiscountType.FIXED, 10_000L, 100_000L);
        VoucherUpdateRequestDTO request = new VoucherUpdateRequestDTO();
        request.setName("Updated reward");
        request.setCodePrefix("NEW");
        request.setDiscountType(DiscountType.PERCENTAGE);
        request.setDiscountValue(15L);
        request.setTierMinAmount(500_000L);
        request.setActive(true);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherRepository.save(existing)).thenReturn(existing);

        var result = voucherService.updateVoucherConfig(1L, request);

        assertThat(result.getName()).isEqualTo("Updated reward");
        assertThat(result.getCodePrefix()).isEqualTo("NEW");
        assertThat(result.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(result.getDiscountValue()).isEqualTo(15L);
        assertThat(result.getTierMinAmount()).isEqualTo(500_000L);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void disableVoucherSoftDeletesTheRuleAndRejectsUnknownId() {
        Voucher existing = voucher(1L, "T1", DiscountType.FIXED, 10_000L, 100_000L);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));

        voucherService.deleteVoucherConfig(1L);

        assertThat(existing.isActive()).isFalse();
        verify(voucherRepository).save(existing);

        when(voucherRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> voucherService.deleteVoucherConfig(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Voucher voucher(Long id, String prefix, DiscountType type, Long value, Long minimum) {
        return Voucher.builder()
                .id(id)
                .name(prefix + " reward")
                .codePrefix(prefix)
                .discountType(type)
                .discountValue(value)
                .tierMinAmount(minimum)
                .active(true)
                .build();
    }
}

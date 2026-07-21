package com.bookverse.service.voucher;

import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.entity.Campaign;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import com.bookverse.enums.CampaignStatus;
import com.bookverse.enums.CampaignType;
import com.bookverse.enums.DiscountType;
import com.bookverse.enums.UserVoucherStatus;
import com.bookverse.enums.UserVoucherStatus;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.enums.UserVoucherStatus;
import com.bookverse.mapper.VoucherMapper;
import com.bookverse.repository.CampaignRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.repository.UserVoucherRepository;
import com.bookverse.repository.VoucherRepository;
import com.bookverse.service.voucher.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoucherServiceImplTest {

    private VoucherRepository voucherRepository;
    private UserVoucherRepository userVoucherRepository;
    private UserRepository userRepository;
    private CampaignRepository campaignRepository;
    private VoucherServiceImpl voucherService;

    @BeforeEach
    void setUp() {
        voucherRepository = mock(VoucherRepository.class);
        userVoucherRepository = mock(UserVoucherRepository.class);
        userRepository = mock(UserRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        voucherService = new VoucherServiceImpl(
                voucherRepository,
                userVoucherRepository,
                userRepository,
                campaignRepository,
                new VoucherMapper()
        );
    }

    @Test
    void claimVoucher_Success() {
        User user = User.builder().id(1L).build();
        Voucher voucher = Voucher.builder()
                .id(1L)
                .status(VoucherStatus.ACTIVE)
                .startTime(Instant.now().minusSeconds(3600))
                .endTime(Instant.now().plusSeconds(3600))
                .claimedQuantity(0)
                .totalQuantity(10)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.existsByUserIdAndVoucherId(1L, 1L)).thenReturn(false);
        when(voucherRepository.findWithLockById(1L)).thenReturn(Optional.of(voucher));
        
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(invocation -> {
            UserVoucher uv = invocation.getArgument(0);
            uv.setId(100L);
            return uv;
        });

        var result = voucherService.claimVoucher(1L, 1L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(voucher.getClaimedQuantity()).isEqualTo(1);
        verify(voucherRepository).save(voucher);
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    void claimVoucher_ThrowsException_IfFullyClaimed() {
        User user = User.builder().id(1L).build();
        Voucher voucher = Voucher.builder()
                .id(1L)
                .status(VoucherStatus.ACTIVE)
                .startTime(Instant.now().minusSeconds(3600))
                .endTime(Instant.now().plusSeconds(3600))
                .claimedQuantity(10)
                .totalQuantity(10)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.existsByUserIdAndVoucherId(1L, 1L)).thenReturn(false);
        when(voucherRepository.findWithLockById(1L)).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.claimVoucher(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher is fully claimed");
    }

    @Test
    void grantWelcomeVoucher_Success() {
        User user = User.builder().id(1L).build();
        Campaign campaign = Campaign.builder().id(1L).build();
        Voucher voucher = Voucher.builder()
                .id(1L)
                .status(VoucherStatus.ACTIVE)
                .claimedQuantity(0)
                .totalQuantity(100)
                .endTime(Instant.now().plusSeconds(3600))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(campaignRepository.findActiveAutoDistributedCampaigns(eq(CampaignType.WELCOME_GIFT), eq(CampaignStatus.ACTIVE), any()))
                .thenReturn(List.of(campaign));
        when(voucherRepository.findAvailableVouchersForCampaign(eq(1L), eq(VoucherStatus.ACTIVE), any()))
                .thenReturn(List.of(voucher));
        when(userVoucherRepository.existsByUserIdAndVoucherId(1L, 1L)).thenReturn(false);
        when(voucherRepository.findWithLockById(1L)).thenReturn(Optional.of(voucher));

        voucherService.grantWelcomeVoucher(1L);

        verify(voucherRepository).save(voucher);
        verify(userVoucherRepository).save(any(UserVoucher.class));
        assertThat(voucher.getClaimedQuantity()).isEqualTo(1);
    }
}

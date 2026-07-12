package com.bookverse.repository;

import com.bookverse.entity.UserVoucher;
import com.bookverse.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    Page<UserVoucher> findByUserIdAndStatusAndExpiresAtGreaterThan(Long userId, VoucherStatus status, Instant now, Pageable pageable);

    Optional<UserVoucher> findByIdAndUserId(Long id, Long userId);
}

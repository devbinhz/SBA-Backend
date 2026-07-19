package com.bookverse.repository;

import com.bookverse.entity.UserVoucher;
import com.bookverse.enums.VoucherStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    Page<UserVoucher> findByUserIdAndStatusAndExpiresAtGreaterThan(Long userId, VoucherStatus status, Instant now, Pageable pageable);

    Optional<UserVoucher> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uv from UserVoucher uv where uv.id = :id and uv.user.id = :userId")
    Optional<UserVoucher> findWithLockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}

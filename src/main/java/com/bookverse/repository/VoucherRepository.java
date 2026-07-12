package com.bookverse.repository;

import com.bookverse.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByActiveTrue();
    List<Voucher> findByActiveTrueAndTierMinAmountLessThanEqualOrderByTierMinAmountDesc(Long tierMinAmount);
    org.springframework.data.domain.Page<Voucher> findByActive(boolean active, org.springframework.data.domain.Pageable pageable);
}

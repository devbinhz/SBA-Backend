package com.bookverse.repository;

import com.bookverse.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.user.id = :userId AND a.defaultAddress = true")
    int clearDefaultByUserId(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.user.id = :userId AND a.id <> :addressId AND a.defaultAddress = true")
    int clearDefaultByUserIdExcept(Long userId, Long addressId);
}

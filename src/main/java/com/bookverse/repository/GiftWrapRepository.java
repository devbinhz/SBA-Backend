package com.bookverse.repository;

import com.bookverse.entity.GiftWrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftWrapRepository extends JpaRepository<GiftWrap, Long> {

    List<GiftWrap> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<GiftWrap> findAllByOrderByDisplayOrderAscIdAsc();
}

package com.bookverse.service.book.impl;

import com.bookverse.repository.OrderItemRepository;
import com.bookverse.service.book.BookOwnershipService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookOwnershipServiceImpl implements BookOwnershipService {

    private final OrderItemRepository orderItemRepository;

    public BookOwnershipServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasUserPurchasedBooks(Long userId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return false;
        }
        List<Long> uniqueBookIds = bookIds.stream().distinct().toList();
        long count = orderItemRepository.countPurchasedByUserAndBooks(userId, uniqueBookIds);
        return count == uniqueBookIds.size();
    }
}

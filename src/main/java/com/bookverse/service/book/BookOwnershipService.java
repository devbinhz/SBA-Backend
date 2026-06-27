package com.bookverse.service.book;

import java.util.List;

public interface BookOwnershipService {
    boolean hasUserPurchasedBooks(Long userId, List<Long> bookIds);
}

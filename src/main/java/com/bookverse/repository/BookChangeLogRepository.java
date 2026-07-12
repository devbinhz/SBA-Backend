package com.bookverse.repository;

import com.bookverse.entity.BookChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookChangeLogRepository extends JpaRepository<BookChangeLog, Long> {
    Page<BookChangeLog> findByBookId(Long bookId, Pageable pageable);
}

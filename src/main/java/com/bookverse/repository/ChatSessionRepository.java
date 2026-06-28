package com.bookverse.repository;

import com.bookverse.entity.ChatSession;
import com.bookverse.enums.AiRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdAndSessionTypeOrderByUpdatedAtDesc(Long userId, AiRequestType sessionType);

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}

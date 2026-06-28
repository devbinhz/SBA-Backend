package com.bookverse.repository;

import com.bookverse.entity.AiUsageLog;
import com.bookverse.enums.AiRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("SELECT l FROM AiUsageLog l WHERE (:userId IS NULL OR l.user.id = :userId) AND (:requestType IS NULL OR l.requestType = :requestType)")
    Page<AiUsageLog> findLogs(
            @Param("userId") Long userId,
            @Param("requestType") AiRequestType requestType,
            Pageable pageable
    );

    @Query("SELECT l.user.id as userId, l.user.fullName as fullName, l.user.email as email, " +
           "COUNT(l) as requestCount, SUM(l.promptTokens) as totalPromptTokens, " +
           "SUM(l.completionTokens) as totalCompletionTokens, SUM(l.totalTokens) as totalTokens, " +
           "SUM(l.durationMs) as totalDurationMs, MAX(l.createdAt) as lastUsedAt " +
           "FROM AiUsageLog l GROUP BY l.user.id, l.user.fullName, l.user.email")
    List<UserAiUsageProjection> getUserUsageSummary();

    @Query("SELECT CAST(l.createdAt AS date) as date, COUNT(l) as requestCount, SUM(l.totalTokens) as totalTokens " +
           "FROM AiUsageLog l GROUP BY CAST(l.createdAt AS date) ORDER BY CAST(l.createdAt AS date) ASC")
    List<DailyAiUsageProjection> getDailyUsageSummary();
}

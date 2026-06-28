package com.bookverse.repository;

import java.time.Instant;

public interface UserAiUsageProjection {
    Long getUserId();
    String getFullName();
    String getEmail();
    Long getRequestCount();
    Long getTotalPromptTokens();
    Long getTotalCompletionTokens();
    Long getTotalTokens();
    Long getTotalDurationMs();
    Instant getLastUsedAt();
}

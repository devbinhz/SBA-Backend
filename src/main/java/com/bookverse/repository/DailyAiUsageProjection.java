package com.bookverse.repository;

import java.sql.Date;

public interface DailyAiUsageProjection {
    Date getDate();
    Long getRequestCount();
    Long getTotalTokens();
}

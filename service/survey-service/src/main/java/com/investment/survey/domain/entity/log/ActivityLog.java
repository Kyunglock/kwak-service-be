package com.investment.survey.domain.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감사/활동 로그 (invdb.tbl_activity_log 공유).
 * 필드 순서 = 컬럼 순서 (MyBatis 생성자 자동 매핑).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    private Long logId;
    private String userId;
    private String actionType;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime regDt;
}

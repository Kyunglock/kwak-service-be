package com.investment.portal.domain.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감사/활동 로그 (tbl_activity_log)
 *
 * 필드 순서 = tbl_activity_log 컬럼 순서 (MyBatis 생성자 자동 매핑용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    private Long logId;             // 로그ID
    private String userId;          // 사용자ID (익명/시스템은 null)
    private String actionType;      // 액션 유형 (LOGIN, TRADE_BUY ...)
    private String targetType;      // 대상 유형 (PORTFOLIO, SURVEY ...)
    private String targetId;        // 대상 식별자
    private String detail;          // 부가 컨텍스트 요약
    private String ip;              // 요청 IP
    private String userAgent;       // User-Agent
    private LocalDateTime regDt;    // 등록일시
}

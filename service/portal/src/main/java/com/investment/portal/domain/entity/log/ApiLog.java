package com.investment.portal.domain.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 요청 로그 (tbl_api_log) — 공격 시도 탐지/운영 감사용
 *
 * 필드 순서 = tbl_api_log 컬럼 순서 (MyBatis 생성자 자동 매핑용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog {

    private Long logId;             // 로그ID
    private String userId;          // 사용자ID (미인증은 null)
    private String ip;              // 클라이언트 IP
    private String method;          // HTTP 메서드
    private String url;             // URI + 쿼리스트링
    private String requestBody;     // 요청 본문 (절단/마스킹)
    private Integer status;         // 응답 상태코드
    private String userAgent;       // User-Agent
    private LocalDateTime regDt;    // 등록일시
}

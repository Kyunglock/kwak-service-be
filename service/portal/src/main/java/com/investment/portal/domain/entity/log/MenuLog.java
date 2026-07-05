package com.investment.portal.domain.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메뉴 이동 로그 (tbl_menu_log) — 사용 패턴 분석용
 *
 * 필드 순서 = tbl_menu_log 컬럼 순서 (MyBatis 생성자 자동 매핑용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuLog {

    private Long logId;             // 로그ID
    private String userId;          // 사용자ID (공개 페이지는 null)
    private String sessionId;       // FE 세션 UUID
    private String menuCd;          // 이동한 메뉴(탭)
    private String prevMenuCd;      // 직전 메뉴 (최초 진입은 null)
    private LocalDateTime regDt;    // 등록일시
}

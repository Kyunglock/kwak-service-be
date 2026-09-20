package com.investment.portal.application.service.trade;

import com.investment.portal.application.dto.trade.TradeCaptureTextRequest;
import com.investment.portal.application.dto.trade.TradeConfirmRequest;
import com.investment.portal.application.dto.trade.TradeConfirmResponse;
import com.investment.portal.application.dto.trade.TradeDraftResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 자연어 문장이나 증권사 화면 캡처에서 매매 기록을 뽑아 초안을 만들고, 사용자 확정 후 저장한다.
 *
 * <p>추출 결과가 곧바로 저장되지 않는 것이 이 기능의 핵심 제약이다.
 * LLM은 숫자를 잘못 읽고, 사용자 돈이 걸린 기록은 되돌리기 어렵다.
 */
public interface TradeCaptureService {

    /** 문장에서 매매 기록 초안 생성 (저장하지 않음). */
    TradeDraftResponse captureText(String userId, TradeCaptureTextRequest request);

    /** 화면 캡처 이미지에서 매매 기록 초안 생성 (저장하지 않음). */
    TradeDraftResponse captureImage(String userId, Long portfolioId, MultipartFile image);

    /** 사용자가 확인·수정한 초안을 실제 거래내역으로 저장. */
    TradeConfirmResponse confirm(String userId, TradeConfirmRequest request);
}

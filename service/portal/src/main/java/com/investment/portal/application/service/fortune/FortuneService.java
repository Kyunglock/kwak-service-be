package com.investment.portal.application.service.fortune;

import com.investment.portal.application.dto.fortune.FortuneResponse;

public interface FortuneService {

    /**
     * (정식 티커, KST 오늘)의 운세를 반환. 캐시 미스면 로컬 LLM으로 동기 생성 후 저장.
     *
     * @throws UnsupportedTickerException 형식 위반 또는 미등록 티커
     * @throws FortuneUnavailableException LLM 호출 실패/무응답
     */
    FortuneResponse getFortune(String rawTicker);
}

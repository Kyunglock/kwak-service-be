package com.investment.portal.application.dto.login.kakao;

import java.util.Map;

import com.investment.portal.application.dto.login.OAuth2Response;
import com.investment.portal.domain.enums.SocialProvider;

public class KakaoResponse implements OAuth2Response{
    
    private final Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute) {
        this.attribute = (Map<String, Object>) attribute.get("response");
    }

    @Override
    public String getProvider() {
        return SocialProvider.KAKAO.name();
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }

    @Override
    public String getNickName() {
        return attribute.get("nickname").toString();
    }
}

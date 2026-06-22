package com.investment.portal.application.service.login.standard;

import com.investment.portal.application.dto.login.standard.StandardLoginRequest;
import com.investment.portal.application.service.login.LoginResponse;

public interface StandardAuthService {

    /**
     * 일반 로그인
     */
    LoginResponse login(StandardLoginRequest request);
}

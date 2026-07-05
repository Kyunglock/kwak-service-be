package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.MenuLogRequest;

public interface MenuLogService {

    void record(String userId, MenuLogRequest request);
}

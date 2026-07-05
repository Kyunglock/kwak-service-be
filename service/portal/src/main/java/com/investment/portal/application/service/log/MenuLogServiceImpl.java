package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.MenuLogRequest;
import com.investment.portal.domain.entity.log.MenuLog;
import com.investment.portal.domain.repository.log.MenuLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuLogServiceImpl implements MenuLogService {

    private final MenuLogMapper menuLogMapper;

    @Override
    public void record(String userId, MenuLogRequest request) {
        menuLogMapper.insert(MenuLog.builder()
                .userId(userId)
                .sessionId(request.sessionId())
                .menuCd(request.menuCd())
                .prevMenuCd(request.prevMenuCd())
                .build());
    }
}

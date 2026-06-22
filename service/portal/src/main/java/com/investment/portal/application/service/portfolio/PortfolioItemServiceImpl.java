package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.portfolio.item.*;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioItemServiceImpl implements PortfolioItemService {

    private final PortfolioItemMapper portfolioItemMapper;

    @Override
    public PortfolioItemResponse getItem(Long itemId) {
        PortfolioItem item = portfolioItemMapper.findByItemId(itemId);
        if (item == null) {
            return null;
        }
        return toResponse(item);
    }

    @Override
    public List<PortfolioItemResponse> getItemsByPortfolioId(Long portfolioId) {
        return portfolioItemMapper.findByPortfolioId(portfolioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PortfolioItemResponse> searchItems(PortfolioItemSearchRequest request) {
        return portfolioItemMapper.search(request).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PortfolioItemResponse addItem(PortfolioItemAddRequest request) {
        PortfolioItem item = PortfolioItem.builder()
                .portfolioId(request.portfolioId())
                .stockCd(request.stockCd())
                .holdQty(request.holdQty())
                .buyPrice(request.buyPrice())
                .buyDt(request.buyDt())
                .currency(request.currency() != null ? request.currency() : "USD")
                .memo(request.memo())
                .build();

        portfolioItemMapper.insert(item);
        log.info("[PortfolioItem] 종목 등록 완료 - itemId: {}, stockCd: {}", item.getItemId(), item.getStockCd());

        return getItem(item.getItemId());
    }

    @Override
    public PortfolioItemResponse modifyItem(PortfolioItemModRequest request) {
        PortfolioItem existing = portfolioItemMapper.findByItemId(request.itemId());
        if (existing == null) {
            throw new IllegalArgumentException("해당 항목을 찾을 수 없습니다: " + request.itemId());
        }

        PortfolioItem item = PortfolioItem.builder()
                .itemId(request.itemId())
                .holdQty(request.holdQty())
                .buyPrice(request.buyPrice())
                .buyDt(request.buyDt())
                .currency(request.currency())
                .memo(request.memo())
                .build();

        portfolioItemMapper.update(item);
        log.info("[PortfolioItem] 종목 수정 완료 - itemId: {}", request.itemId());

        return getItem(request.itemId());
    }

    @Override
    public void removeItem(Long itemId) {
        PortfolioItem existing = portfolioItemMapper.findByItemId(itemId);
        if (existing == null) {
            throw new IllegalArgumentException("해당 항목을 찾을 수 없습니다: " + itemId);
        }

        portfolioItemMapper.delete(itemId);
        log.info("[PortfolioItem] 종목 삭제 완료 - itemId: {}", itemId);
    }

    private PortfolioItemResponse toResponse(PortfolioItem item) {
        return new PortfolioItemResponse(
                item.getItemId(),
                item.getPortfolioId(),
                item.getStockCd(),
                item.getHoldQty(),
                item.getBuyPrice(),
                item.getBuyDt(),
                item.getBuyAmount(),
                item.getCurrency(),
                item.getMemo(),
                item.getUseYn(),
                item.getRegDt(),
                item.getUpdDt()
        );
    }
}

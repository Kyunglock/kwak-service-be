package com.investment.portal.domain.repository.stock;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CompaniesMapper {

    /**
     * tbl_companies에 등록된 전체 티커 목록 조회
     */
    List<String> findAllTickers();
}

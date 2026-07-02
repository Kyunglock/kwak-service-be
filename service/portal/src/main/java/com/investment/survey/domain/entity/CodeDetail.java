package com.investment.survey.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeDetail {

    private Long codeDetailId;
    private Long codeGroupId;
    private String codeValue;
    private String codeName;
    private String codeDesc;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}

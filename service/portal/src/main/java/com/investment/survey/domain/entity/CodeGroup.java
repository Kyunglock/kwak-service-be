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
public class CodeGroup {

    private Long codeGroupId;
    private String groupCode;
    private String groupName;
    private String description;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}

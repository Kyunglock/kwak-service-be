package com.investment.portal.application.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    String userId;

    String nickname;

    String role;
}

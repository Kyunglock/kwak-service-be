package kwak.common.infrastructure.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    private String userId;
    private String nickname;
    private String email;
    private String profileImgUrl;
}

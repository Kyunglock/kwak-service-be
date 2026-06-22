package kwak.common.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RokResponse<T> {
    
    /**
     * 요청 성공 여부
     */
    private boolean success;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 실제 응답 데이터 (제네릭)
     */
    private T data;
    
    /**
     * 응답 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
}

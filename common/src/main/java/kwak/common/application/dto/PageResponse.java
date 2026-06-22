package kwak.common.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    /**
     * 실제 데이터 목록
     */
    private List<T> content;
    
    /**
     * 현재 페이지 번호 (1부터 시작)
     */
    private int currentPage;
    
    /**
     * 페이지 크기
     */
    private int pageSize;
    
    /**
     * 전체 데이터 개수
     */
    private long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private int totalPages;
    
    /**
     * 첫 페이지 여부
     */
    private boolean first;
    
    /**
     * 마지막 페이지 여부
     */
    private boolean last;
    
    /**
     * 비어있는지 여부
     */
    private boolean empty;
    
    /**
     * PageResponse 생성 헬퍼 메서드
     */
    public static <T> PageResponse<T> of(List<T> content, int currentPage, int pageSize, int totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        
        return PageResponse.<T>builder()
            .content(content)
            .currentPage(currentPage)
            .pageSize(pageSize)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(currentPage == 1)
            .last(currentPage >= totalPages)
            .empty(content.isEmpty())
            .build();
    }
}

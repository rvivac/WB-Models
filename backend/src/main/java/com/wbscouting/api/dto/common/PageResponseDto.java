package com.wbscouting.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseDto<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Data nao processa no mvnw 3.6.3 Render)
    // ============================================================
    public List<T> getContent() { return content; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLast() { return isLast; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS
    // ============================================================
    public void setContent(List<T> content) { this.content = content; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setLast(boolean last) { isLast = last; }

    // ============================================================
    // 🔥 BUILDER MANUAL GENERICO FALLBACK (compativel com tipo T)
    // ============================================================
    public static <T> PageResponseDtoBuilder<T> builder() { return new PageResponseDtoBuilder<>(); }

    public static class PageResponseDtoBuilder<T> {
        private final PageResponseDto<T> d = new PageResponseDto<>();
        public PageResponseDtoBuilder<T> content(List<T> v) { d.setContent(v); return this; }
        public PageResponseDtoBuilder<T> pageNumber(int v) { d.setPageNumber(v); return this; }
        public PageResponseDtoBuilder<T> pageSize(int v) { d.setPageSize(v); return this; }
        public PageResponseDtoBuilder<T> totalElements(long v) { d.setTotalElements(v); return this; }
        public PageResponseDtoBuilder<T> totalPages(int v) { d.setTotalPages(v); return this; }
        public PageResponseDtoBuilder<T> isLast(boolean v) { d.setLast(v); return this; }
        public PageResponseDto<T> build() { return d; }
    }

    public static <T> PageResponseDto<T> from(Page<T> page) {
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}

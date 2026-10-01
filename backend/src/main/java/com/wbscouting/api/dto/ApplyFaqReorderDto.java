package com.wbscouting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyFaqReorderDto {

    private List<ReorderItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderItem {
        private UUID id;
        private Integer displayOrder;
    }
}

package com.rassini.graphite_client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierMigrationResponse {
    private boolean success;
    private String message;
    private Integer receivedErpIds;
    private Integer uniqueErpIds;
    private Integer foundErpIds;
    private Integer notFoundErpIds;
    private Integer foundRecords;
    private Integer exportedRecords;
    private Integer updatedRecords;
    private Integer fallbackRecords;
    private Integer generatedFiles;
    private Long executionTimeMs;
    private List<String> notFoundList;
    private TruncatedListDto<String> fallbackErpIds;
    private List<MultiRecordDto> multiRecordErpIds;
    private TruncatedListDto<String> files;
    private TruncatedListDto<String> updatedErpIds;
    private TruncatedListDto<String> exportedErpIds;
    private MigrationSummary summary;
    private SupplierMigrationResponse partialSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MigrationSummary {
        private String status;
        private String message;
        private String executionDate;
        private Long executionTimeMs;
    }
}

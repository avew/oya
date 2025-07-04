package io.github.avew.oya.dto;

import io.github.avew.oya.entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResult {
    private DocumentChunk documentChunk;
    private Double vectorSimilarity;
    private Double textRank;
    private Double hybridScore;
    private String searchMethod;
}

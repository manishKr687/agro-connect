package com.agroconnect.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchSuggestionResponse {
    private Long demandId;
    private String demandCropName;
    private Double demandQuantity;
    private Long harvestId;
    private String harvestCropName;
    private Double harvestQuantity;
    private Double score;
    private String reason;
}

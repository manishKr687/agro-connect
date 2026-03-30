package com.agroconnect.dto;

import lombok.Value;

/**
 * Aggregated public view of open demand for a single crop.
 * Used by the public dashboard — no retailer PII exposed.
 */
@Value
public class CropDemandSummary {

    String cropName;
    Double totalQuantity;
}

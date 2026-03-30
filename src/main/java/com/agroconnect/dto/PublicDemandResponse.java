package com.agroconnect.dto;

import com.agroconnect.model.Demand;
import lombok.Value;

import java.time.LocalDate;

/**
 * Public-safe projection of a demand — no retailer PII, price, or status exposed.
 */
@Value
public class PublicDemandResponse {

    Long id;
    String cropName;
    Double quantity;
    LocalDate requiredDate;

    public static PublicDemandResponse from(Demand demand) {
        return new PublicDemandResponse(
                demand.getId(),
                demand.getCropName(),
                demand.getQuantity(),
                demand.getRequiredDate()
        );
    }
}

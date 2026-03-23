package com.agroconnect.service;

import com.agroconnect.dto.MatchSuggestionResponse;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.HarvestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final HarvestRepository harvestRepository;
    private final DemandRepository demandRepository;
    private final AccessControlService accessControlService;

    public List<MatchSuggestionResponse> getTopSuggestionsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);

        List<Harvest> availableHarvests = harvestRepository.findAll().stream()
                .filter(harvest -> harvest.getStatus() == Harvest.Status.AVAILABLE)
                .toList();

        return demandRepository.findAll().stream()
                .filter(demand -> demand.getStatus() == Demand.Status.OPEN)
                .map(demand -> buildTopSuggestion(demand, availableHarvests))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(MatchSuggestionResponse::getScore).reversed())
                .toList();
    }

    private Optional<MatchSuggestionResponse> buildTopSuggestion(Demand demand, List<Harvest> harvests) {
        return harvests.stream()
                .filter(harvest -> isCandidate(harvest, demand))
                .map(harvest -> toSuggestion(harvest, demand))
                .max(Comparator.comparing(MatchSuggestionResponse::getScore));
    }

    private boolean isCandidate(Harvest harvest, Demand demand) {
        return harvest.getCropName() != null
                && demand.getCropName() != null
                && harvest.getCropName().trim().equalsIgnoreCase(demand.getCropName().trim())
                && harvest.getQuantity() != null
                && demand.getQuantity() != null
                && harvest.getQuantity() >= demand.getQuantity()
                && harvest.getHarvestDate() != null
                && demand.getRequiredDate() != null
                && !harvest.getHarvestDate().isAfter(demand.getRequiredDate());
    }

    private MatchSuggestionResponse toSuggestion(Harvest harvest, Demand demand) {
        double score = calculateScore(harvest, demand);

        return MatchSuggestionResponse.builder()
                .demandId(demand.getId())
                .demandCropName(demand.getCropName())
                .demandQuantity(demand.getQuantity())
                .harvestId(harvest.getId())
                .harvestCropName(harvest.getCropName())
                .harvestQuantity(harvest.getQuantity())
                .score(Math.round(score * 100.0) / 100.0)
                .reason(buildReason(harvest, demand))
                .build();
    }

    private double calculateScore(Harvest harvest, Demand demand) {
        double cropScore = 1.0;
        double quantityScore = safeRatio(demand.getQuantity(), harvest.getQuantity());

        long daysOld = ChronoUnit.DAYS.between(harvest.getHarvestDate(), LocalDate.now());
        double freshnessScore = 1.0 - Math.min(Math.max(daysOld, 0) / 7.0, 1.0);

        double priceScore = 0.5;
        if (harvest.getExpectedPrice() != null && demand.getTargetPrice() != null && demand.getTargetPrice() > 0) {
            double priceDifference = Math.abs(harvest.getExpectedPrice() - demand.getTargetPrice());
            priceScore = 1.0 - Math.min(priceDifference / demand.getTargetPrice(), 1.0);
        }

        long daysUntilRequired = ChronoUnit.DAYS.between(LocalDate.now(), demand.getRequiredDate());
        double urgencyScore = 1.0 - Math.min(Math.max(daysUntilRequired, 0) / 7.0, 1.0);

        return (45 * cropScore)
                + (25 * quantityScore)
                + (15 * freshnessScore)
                + (10 * priceScore)
                + (5 * urgencyScore);
    }

    private double safeRatio(Double numerator, Double denominator) {
        if (numerator == null || denominator == null || denominator <= 0) {
            return 0;
        }
        return Math.min(numerator / denominator, 1.0);
    }

    private String buildReason(Harvest harvest, Demand demand) {
        String crop = demand.getCropName() == null ? "crop" : demand.getCropName().toLowerCase(Locale.ROOT);
        return "Exact " + crop + " match, quantity covers demand, harvest date fits required date";
    }
}

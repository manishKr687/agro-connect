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

/**
 * Scores available harvests against open demands and returns the best match per demand.
 *
 * <p>The scoring formula weights five factors (total = 100 points):
 * <ul>
 *   <li>45 pts — crop name match (case-insensitive exact match, required to be a candidate)</li>
 *   <li>25 pts — quantity ratio (demand quantity / harvest quantity, capped at 1.0)</li>
 *   <li>15 pts — freshness (decays linearly over 7 days since harvest date)</li>
 *   <li>10 pts — price alignment (how close harvest price is to the demand's target price)</li>
 *   <li> 5 pts — urgency (how soon the demand's required date is, within 7 days)</li>
 * </ul>
 *
 * <p>A harvest is only considered a candidate if it:
 * <ul>
 *   <li>matches the demand's crop name (case-insensitive)</li>
 *   <li>has quantity &ge; demand quantity</li>
 *   <li>has harvest date &le; demand required date</li>
 * </ul>
 *
 * <p>Only the top-scoring harvest per demand is returned. Results are sorted by score descending.
 * Only {@code AVAILABLE} harvests and {@code OPEN} demands are fetched — no in-memory filtering.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final HarvestRepository harvestRepository;
    private final DemandRepository demandRepository;
    private final AccessControlService accessControlService;

    /**
     * Returns the best harvest match for each open demand, sorted by score descending.
     *
     * @param adminId ID of the requesting admin (verified by {@link AccessControlService})
     * @return list of top suggestions; empty if no valid matches exist
     */
    public List<MatchSuggestionResponse> getTopSuggestionsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);

        List<Harvest> availableHarvests = harvestRepository.findByStatus(Harvest.Status.AVAILABLE);

        return demandRepository.findByStatus(Demand.Status.OPEN).stream()
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

    /** Returns true if the harvest satisfies all hard requirements to be matched against the demand. */
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

    /** Returns numerator/denominator capped at 1.0, or 0 if either value is null/zero. */
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

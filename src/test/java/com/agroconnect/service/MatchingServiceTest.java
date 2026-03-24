package com.agroconnect.service;

import com.agroconnect.dto.MatchSuggestionResponse;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.HarvestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock HarvestRepository harvestRepository;
    @Mock DemandRepository demandRepository;
    @Mock AccessControlService accessControlService;

    @InjectMocks MatchingService matchingService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(accessControlService.requireAdmin(any())).thenReturn(admin);
    }

    private Harvest availableHarvest(String cropName, double quantity, LocalDate harvestDate) {
        return Harvest.builder()
                .id(1L)
                .cropName(cropName)
                .quantity(quantity)
                .harvestDate(harvestDate)
                .expectedPrice(100.0)
                .status(Harvest.Status.AVAILABLE)
                .build();
    }

    private Demand openDemand(String cropName, double quantity, LocalDate requiredDate) {
        return Demand.builder()
                .id(1L)
                .cropName(cropName)
                .quantity(quantity)
                .requiredDate(requiredDate)
                .targetPrice(100.0)
                .status(Demand.Status.OPEN)
                .build();
    }

    @Test
    void getTopSuggestions_cropNameMismatch_returnsEmpty() {
        Harvest harvest = availableHarvest("Tomato", 100.0, LocalDate.now());
        Demand demand = openDemand("Potato", 50.0, LocalDate.now().plusDays(2));

        when(harvestRepository.findByStatus(Harvest.Status.AVAILABLE)).thenReturn(List.of(harvest));
        when(demandRepository.findByStatus(Demand.Status.OPEN)).thenReturn(List.of(demand));

        List<MatchSuggestionResponse> result = matchingService.getTopSuggestionsForAdmin(admin.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void getTopSuggestions_harvestQuantityLessThanDemand_returnsEmpty() {
        Harvest harvest = availableHarvest("Tomato", 30.0, LocalDate.now());
        Demand demand = openDemand("Tomato", 50.0, LocalDate.now().plusDays(2));

        when(harvestRepository.findByStatus(Harvest.Status.AVAILABLE)).thenReturn(List.of(harvest));
        when(demandRepository.findByStatus(Demand.Status.OPEN)).thenReturn(List.of(demand));

        List<MatchSuggestionResponse> result = matchingService.getTopSuggestionsForAdmin(admin.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void getTopSuggestions_harvestDateAfterRequiredDate_returnsEmpty() {
        Harvest harvest = availableHarvest("Tomato", 100.0, LocalDate.now().plusDays(5));
        Demand demand = openDemand("Tomato", 50.0, LocalDate.now().plusDays(2));

        when(harvestRepository.findByStatus(Harvest.Status.AVAILABLE)).thenReturn(List.of(harvest));
        when(demandRepository.findByStatus(Demand.Status.OPEN)).thenReturn(List.of(demand));

        List<MatchSuggestionResponse> result = matchingService.getTopSuggestionsForAdmin(admin.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void getTopSuggestions_caseInsensitiveCropMatch_returnsSuggestion() {
        Harvest harvest = availableHarvest("TOMATO", 100.0, LocalDate.now());
        Demand demand = openDemand("tomato", 50.0, LocalDate.now().plusDays(2));

        when(harvestRepository.findByStatus(Harvest.Status.AVAILABLE)).thenReturn(List.of(harvest));
        when(demandRepository.findByStatus(Demand.Status.OPEN)).thenReturn(List.of(demand));

        List<MatchSuggestionResponse> result = matchingService.getTopSuggestionsForAdmin(admin.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHarvestId()).isEqualTo(harvest.getId());
        assertThat(result.get(0).getDemandId()).isEqualTo(demand.getId());
    }

    @Test
    void getTopSuggestions_exactQuantityCoverageScoresHigherThanOversupply() {
        LocalDate requiredDate = LocalDate.now().plusDays(2);

        Harvest exactHarvest = Harvest.builder()
                .id(1L).cropName("Tomato").quantity(50.0)
                .harvestDate(LocalDate.now()).expectedPrice(100.0)
                .status(Harvest.Status.AVAILABLE).build();

        Harvest oversupplyHarvest = Harvest.builder()
                .id(2L).cropName("Tomato").quantity(200.0)
                .harvestDate(LocalDate.now()).expectedPrice(100.0)
                .status(Harvest.Status.AVAILABLE).build();

        Demand demand = openDemand("Tomato", 50.0, requiredDate);

        when(harvestRepository.findByStatus(Harvest.Status.AVAILABLE)).thenReturn(List.of(exactHarvest, oversupplyHarvest));
        when(demandRepository.findByStatus(Demand.Status.OPEN)).thenReturn(List.of(demand));

        List<MatchSuggestionResponse> result = matchingService.getTopSuggestionsForAdmin(admin.getId());

        // Only the top suggestion per demand is returned — it should be the exact match
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHarvestId()).isEqualTo(exactHarvest.getId());
    }
}

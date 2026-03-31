package com.agroconnect.controller;

import com.agroconnect.dto.CropDemandSummary;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.service.DemandEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Unauthenticated endpoints for the public market dashboard.
 *
 * <p>Exposes accumulated open demand per crop — no individual demand rows,
 * no retailer PII. Permitted without authentication in
 * {@link com.agroconnect.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicDashboardController {

    private final DemandRepository demandRepository;
    private final DemandEventService demandEventService;

    /** Snapshot endpoint — returns aggregated open demand per crop. */
    @GetMapping("/demands")
    public List<CropDemandSummary> getOpenDemandSummaries() {
        return demandRepository.findOpenDemandSummaries();
    }

    /**
     * SSE stream endpoint. Rate-limited by {@link com.agroconnect.security.SseRateLimitingFilter}
     * (connection rate) and by {@link DemandEventService} (concurrent connections per IP).
     */
    @GetMapping(value = "/demands/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDemands(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        SseEmitter emitter = demandEventService.subscribe(clientIp);

        List<CropDemandSummary> snapshot = demandRepository.findOpenDemandSummaries();
        demandEventService.broadcastTo(emitter, snapshot);

        return emitter;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.agroconnect.service;

import com.agroconnect.dto.CropDemandSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active SSE connections for the public demand dashboard.
 *
 * <p>Clients connect via {@code GET /api/public/demands/stream}. When any open demand
 * changes (created or deleted), {@link #broadcast} pushes the updated list to all
 * connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandEventService {

    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new SSE client. Sends an initial keep-alive comment immediately
     * so the browser knows the connection is open.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — browser reconnects on close

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send an initial comment to flush headers to the browser immediately
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Sends the current demand list to a single emitter (used for the initial snapshot
     * on connection before the first broadcast event).
     */
    public void broadcastTo(SseEmitter emitter, List<CropDemandSummary> demands) {
        try {
            String json = objectMapper.writeValueAsString(demands);
            emitter.send(SseEmitter.event().name("demands").data(json));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    /**
     * Pushes the current list of open demands to all connected SSE clients.
     * Dead emitters are removed on send failure.
     */
    public void broadcast(List<CropDemandSummary> demands) {
        if (emitters.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(demands);
        } catch (IOException e) {
            log.error("Failed to serialize demands for SSE broadcast", e);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("demands").data(json));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}

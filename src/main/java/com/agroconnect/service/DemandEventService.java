package com.agroconnect.service;

import com.agroconnect.dto.CropDemandSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages active SSE connections for the public demand dashboard.
 *
 * <p>Enforces a per-IP concurrent connection limit to prevent a single client from
 * exhausting server resources by opening many persistent connections.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.sse.max-connections-per-ip} — max concurrent SSE connections per IP (default: 3)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandEventService {

    @Value("${app.sse.max-connections-per-ip:3}")
    private int maxConnectionsPerIp;

    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Tracks the number of active connections per IP address. */
    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE client for the given IP.
     *
     * @throws ResponseStatusException 429 if the IP has reached the concurrent connection limit
     */
    public SseEmitter subscribe(String clientIp) {
        AtomicInteger count = connectionCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));

        if (count.get() >= maxConnectionsPerIp) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many concurrent SSE connections from this IP.");
        }

        count.incrementAndGet();
        SseEmitter emitter = new SseEmitter(0L); // no timeout — browser reconnects on close

        Runnable onClose = () -> {
            emitters.remove(emitter);
            int remaining = connectionCounts.getOrDefault(clientIp, new AtomicInteger(0)).decrementAndGet();
            if (remaining <= 0) connectionCounts.remove(clientIp);
        };

        emitters.add(emitter);
        emitter.onCompletion(onClose);
        emitter.onTimeout(onClose);
        emitter.onError(e -> onClose.run());

        // Flush headers to the browser immediately
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            onClose.run();
        }

        return emitter;
    }

    /**
     * Sends the current demand list to a single emitter (initial snapshot on connect).
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

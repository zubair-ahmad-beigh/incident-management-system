package com.ims.backend.controller;

import com.ims.backend.dto.SignalRequest;
import com.ims.backend.service.SignalIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Signal ingestion controller.
 *
 * POST /api/signals – accepts high-throughput signal payloads.
 * Returns 202 Accepted (not 201 Created) because processing is asynchronous.
 */
@Slf4j
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalIngestionService ingestionService;

    @PostMapping
    public Mono<ResponseEntity<Map<String, String>>> ingest(
            @Valid @RequestBody SignalRequest request,
            ServerWebExchange exchange) {

        // Extract remote IP for rate limiting
        String remoteIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        return ingestionService.ingest(request, remoteIp)
                .map(signalId -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(Map.of(
                                "status",   "accepted",
                                "signalId", signalId,
                                "message",  "Signal queued for async processing"
                        ))
                );
    }
}

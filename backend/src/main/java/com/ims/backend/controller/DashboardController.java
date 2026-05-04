package com.ims.backend.controller;

import com.ims.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Operational dashboard controller.
 *
 * GET /api/dashboard/summary – real-time aggregate counts across all stores.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getSummary() {
        return dashboardService.getSummary();
    }
}

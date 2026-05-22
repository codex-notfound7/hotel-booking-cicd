package com.project.soa.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/analytics")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
@Tag(name = "Analytics", description = "System usage reports — MANAGER and ADMIN")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(
            summary = "System analytics report",
            description = "Returns aggregate stats: total users, hotels, bookings by status, " +
                    "revenue, revenue by hotel, top hotels by booking volume, bookings per month, and review averages.")
    public AnalyticsReportDto getReport() {
        return analyticsService.generateReport();
    }
}
package com.vishnu.ticketsystem.controller;

import com.vishnu.ticketsystem.model.Ticket;
import com.vishnu.ticketsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TicketRepository ticketRepository;

    @GetMapping
    public Map<String, Object> getAnalytics() {
        List<Ticket> all = ticketRepository.findAll();
        long total = all.size();
        long processed = ticketRepository.countByAiProcessedTrue();
        long fromCache = ticketRepository.countByFromCacheTrue();

        Map<String, Long> byCategory = all.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(Ticket::getCategory, Collectors.counting()));

        double cacheHitRate = processed == 0 ? 0 : (fromCache * 100.0 / processed);

        return Map.of(
                "totalTickets", total,
                "aiProcessedTickets", processed,
                "cacheHits", fromCache,
                "cacheHitRatePercent", Math.round(cacheHitRate * 100.0) / 100.0,
                "ticketsByCategory", byCategory
        );
    }
}

package com.vishnu.ticketsystem.repository;

import com.vishnu.ticketsystem.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCategory(String category);
    List<Ticket> findByStatus(Ticket.TicketStatus status);
    long countByFromCacheTrue();
    long countByAiProcessedTrue();
}

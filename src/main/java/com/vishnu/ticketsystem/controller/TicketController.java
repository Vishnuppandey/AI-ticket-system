package com.vishnu.ticketsystem.controller;

import com.vishnu.ticketsystem.dto.TicketRequest;
import com.vishnu.ticketsystem.kafka.TicketEventProducer;
import com.vishnu.ticketsystem.model.Ticket;
import com.vishnu.ticketsystem.repository.TicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketEventProducer ticketEventProducer;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setUserEmail(request.getUserEmail());

        Ticket saved = ticketRepository.save(ticket);

        // Publish event so classification happens asynchronously -
        // this call returns immediately regardless of AI latency
        ticketEventProducer.publishTicketCreated(saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @GetMapping("/category/{category}")
    public List<Ticket> getByCategory(@PathVariable String category) {
        return ticketRepository.findByCategory(category.toUpperCase());
    }
}

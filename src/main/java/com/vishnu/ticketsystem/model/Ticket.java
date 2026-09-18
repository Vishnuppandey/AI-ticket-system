package com.vishnu.ticketsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    // Set manually by user when creating the ticket (optional)
    private String userEmail;

    // ---- Fields filled in by the AI classification step ----
    private String category;      // BILLING, TECHNICAL, ACCOUNT, OTHER
    private String priority;      // LOW, MEDIUM, HIGH
    @Column(length = 2000)
    private String suggestedResponse;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.PENDING_CLASSIFICATION;

    private boolean aiProcessed = false;
    private boolean fromCache = false; // true if classification came from Redis cache

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime classifiedAt;

    public enum TicketStatus {
        PENDING_CLASSIFICATION,
        CLASSIFIED,
        IN_PROGRESS,
        RESOLVED,
        FAILED_CLASSIFICATION
    }
}

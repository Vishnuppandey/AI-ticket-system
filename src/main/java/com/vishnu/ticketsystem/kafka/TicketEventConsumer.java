package com.vishnu.ticketsystem.kafka;

import com.vishnu.ticketsystem.model.Ticket;
import com.vishnu.ticketsystem.repository.TicketRepository;
import com.vishnu.ticketsystem.service.AiClassificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final TicketRepository ticketRepository;
    private final AiClassificationService aiClassificationService;

    @KafkaListener(topics = "ticket-created", groupId = "ticket-classification-group")
    public void consume(String ticketIdStr) {
        Long ticketId = Long.valueOf(ticketIdStr);
        log.info("Consumed ticket-created event for ticket id={}", ticketId);

        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            try {
                aiClassificationService.classifyAndUpdate(ticket);
            } catch (Exception e) {
                log.error("AI classification failed for ticket id={}", ticketId, e);
                ticket.setStatus(Ticket.TicketStatus.FAILED_CLASSIFICATION);
                ticketRepository.save(ticket);
            }
        });
    }
}

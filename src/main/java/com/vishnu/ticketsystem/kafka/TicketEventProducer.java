package com.vishnu.ticketsystem.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketEventProducer {

    private static final String TOPIC = "ticket-created";

    private final KafkaTemplate<Long, String> kafkaTemplate;

    public void publishTicketCreated(Long ticketId) {
        // value can just be the ticket id as a string; keep it simple for a v1 build
        kafkaTemplate.send(TOPIC, ticketId, String.valueOf(ticketId));
    }
}

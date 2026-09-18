package com.vishnu.ticketsystem.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiClassificationResult implements Serializable {
    private String category;
    private String priority;
    private String suggestedResponse;
}

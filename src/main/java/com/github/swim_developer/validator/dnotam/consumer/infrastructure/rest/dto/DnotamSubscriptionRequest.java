package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest.dto;

import com.github.swim_developer.validator.core.domain.model.QualityOfService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Digital NOTAM subscription request")
@SuppressWarnings("deprecation")
public record DnotamSubscriptionRequest(
        @NotNull
        @Schema(description = "Business topic name", required = true, example = "DigitalNOTAMService")
        String topic,

        @Schema(description = "Quality of service for AMQP delivery", example = "AT_LEAST_ONCE")
        QualityOfService qos,

        @Schema(description = "Whether the subscriber queue is durable", example = "true")
        Boolean durable,

        @Pattern(regexp = "^(DNOTAM-[\\w\\-]+)?$")
        @Schema(description = "Optional AMQP queue name; generated when omitted", example = "DNOTAM-client01-550e8400-e29b-41d4-a716-446655440000")
        String queueName,

        @Schema(description = "Event scenario codes to filter (e.g. RWY.CLS)", example = "[\"RWY.CLS\", \"SAA.ACT\"]")
        List<String> eventScenario,

        @Schema(description = "ICAO airport or heliport identifiers", example = "[\"EADD\", \"EHAM\"]")
        List<String> airportHeliport,

        @Schema(description = "Airspace identifiers (e.g. FIR)", example = "[\"EAAD\", \"EHAA\"]")
        List<String> airspace,

        @Schema(description = "NOTAM series letter", example = "A")
        String eventSeries,

        @Schema(description = "Information publisher", example = "EUROCONTROL")
        String publisher,

        @Schema(description = "Data provider", example = "EAD")
        String provider,

        @Schema(description = "Human-readable subscription purpose", example = "Runway closure monitoring for Donlon")
        String description,

        @Schema(description = "Optional operator comment", example = "CP1 validation run")
        String comment
) {}

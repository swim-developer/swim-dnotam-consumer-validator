package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.EventFileMetadata;
import io.quarkus.qute.TemplateExtension;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TemplateExtension
public class EventFileMetadataExtensions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventFileMetadataExtensions() {
    }

    @TemplateExtension
    public static String airportsJson(EventFileMetadata file) {
        return toJson(file.getAirports());
    }

    @TemplateExtension
    public static String airspacesJson(EventFileMetadata file) {
        return toJson(file.getAirspaces());
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}

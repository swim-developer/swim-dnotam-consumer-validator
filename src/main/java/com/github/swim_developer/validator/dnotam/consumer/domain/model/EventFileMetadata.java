package com.github.swim_developer.validator.dnotam.consumer.domain.model;


import java.util.Set;

public class EventFileMetadata {

    private final String filename;
    private final String eventScenario;
    private final Set<String> airports;
    private final Set<String> airspaces;
    private final String eventSeries;
    private final String publisher;
    private final String provider;

    public EventFileMetadata(String filename, String eventScenario, Set<String> airports, Set<String> airspaces,
                             String eventSeries, String publisher, String provider) {
        this.filename = filename;
        this.eventScenario = eventScenario;
        this.airports = airports;
        this.airspaces = airspaces;
        this.eventSeries = eventSeries;
        this.publisher = publisher;
        this.provider = provider;
    }

    public String getFilename() {
        return filename;
    }

    public String getEventScenario() {
        return eventScenario;
    }

    public Set<String> getAirports() {
        return airports;
    }

    public Set<String> getAirspaces() {
        return airspaces;
    }

    public String getEventSeries() {
        return eventSeries;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getProvider() {
        return provider;
    }

}

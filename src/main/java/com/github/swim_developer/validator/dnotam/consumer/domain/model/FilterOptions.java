package com.github.swim_developer.validator.dnotam.consumer.domain.model;


import java.util.List;

public class FilterOptions {

    private final List<String> eventScenarios;
    private final List<String> airports;
    private final List<String> airspaces;
    private final List<String> series;
    private final List<String> publishers;
    private final List<String> providers;

    public FilterOptions(List<String> eventScenarios, List<String> airports, List<String> airspaces,
                         List<String> series, List<String> publishers, List<String> providers) {
        this.eventScenarios = eventScenarios;
        this.airports = airports;
        this.airspaces = airspaces;
        this.series = series;
        this.publishers = publishers;
        this.providers = providers;
    }

    public List<String> getEventScenarios() {
        return eventScenarios;
    }

    public List<String> getAirports() {
        return airports;
    }

    public List<String> getAirspaces() {
        return airspaces;
    }

    public List<String> getSeries() {
        return series;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public List<String> getProviders() {
        return providers;
    }
}

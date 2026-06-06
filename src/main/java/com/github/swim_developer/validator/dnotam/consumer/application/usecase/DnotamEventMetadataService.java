package com.github.swim_developer.validator.dnotam.consumer.application.usecase;

import com.github.swim_developer.validator.core.domain.util.XmlPatternExtractor;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.FilterOptions;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.github.swim_developer.validator.dnotam.consumer.domain.port.in.DnotamEventMetadataPort;

@Slf4j
@ApplicationScoped
public class DnotamEventMetadataService implements DnotamEventMetadataPort {

    static final Pattern SCENARIO_PATTERN = Pattern.compile("<event:scenario>([^<]+)</event:scenario>");
    static final Pattern AIRPORT_PATTERN = Pattern.compile("<event:location>([A-Z]{4})</event:location>");
    static final Pattern SERIES_PATTERN = Pattern.compile("<event:series>([A-Z])</event:series>");
    static final Pattern PUBLISHER_PATTERN = Pattern.compile("<event:publisher[^>]*xlink:title=\"([^\"]+)\"");
    static final Pattern PROVIDER_PATTERN = Pattern.compile("<event:provider[^>]*xlink:title=\"([^\"]+)\"");
    static final Pattern AIRSPACE_FIR_PATTERN = Pattern.compile("<event:affectedFIR>([A-Z]{4})</event:affectedFIR>");

    @ConfigProperty(name = "event.generator.events.path", defaultValue = "/opt/events")
    String eventsPath;

    @CacheResult(cacheName = "event-metadata")
    public List<EventFileMetadata> getAllEventMetadata() {
        Path dir = Paths.get(eventsPath);
        if (!Files.isDirectory(dir)) {
            log.warn("Events directory not found: {}", eventsPath);
            return List.of();
        }
        try {
            return Files.list(dir)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(this::extractMetadata)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public FilterOptions getFilterOptions() {
        List<EventFileMetadata> all = getAllEventMetadata();
        Set<String> scenarios = new LinkedHashSet<>();
        Set<String> airports = new LinkedHashSet<>();
        Set<String> airspaces = new LinkedHashSet<>();
        Set<String> series = new LinkedHashSet<>();
        Set<String> publishers = new LinkedHashSet<>();
        Set<String> providers = new LinkedHashSet<>();
        for (EventFileMetadata m : all) {
            if (m.getEventScenario() != null && !m.getEventScenario().isBlank()) {
                scenarios.add(m.getEventScenario());
            }
            airports.addAll(m.getAirports());
            airspaces.addAll(m.getAirspaces());
            if (m.getEventSeries() != null && !m.getEventSeries().isBlank()) {
                series.add(m.getEventSeries());
            }
            if (m.getPublisher() != null && !m.getPublisher().isBlank()) {
                publishers.add(m.getPublisher());
            }
            if (m.getProvider() != null && !m.getProvider().isBlank()) {
                providers.add(m.getProvider());
            }
        }
        return new FilterOptions(
                scenarios.stream().sorted().toList(),
                airports.stream().sorted().toList(),
                airspaces.stream().sorted().toList(),
                series.stream().sorted().toList(),
                publishers.stream().sorted().toList(),
                providers.stream().sorted().toList()
        );
    }

    private EventFileMetadata extractMetadata(Path file) {
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String scenario = XmlPatternExtractor.extractFirst(SCENARIO_PATTERN, content).orElse(null);
        Set<String> airports = new HashSet<>(XmlPatternExtractor.extractAll(AIRPORT_PATTERN, content));
        Set<String> airspaces = new LinkedHashSet<>(XmlPatternExtractor.extractAll(AIRSPACE_FIR_PATTERN, content));
        String series = XmlPatternExtractor.extractFirst(SERIES_PATTERN, content).orElse(null);
        String publisher = XmlPatternExtractor.extractFirst(PUBLISHER_PATTERN, content).orElse(null);
        String provider = XmlPatternExtractor.extractFirst(PROVIDER_PATTERN, content).orElse(null);
        return new EventFileMetadata(
                file.getFileName().toString(),
                scenario,
                airports,
                airspaces,
                series,
                publisher,
                provider
        );
    }
}

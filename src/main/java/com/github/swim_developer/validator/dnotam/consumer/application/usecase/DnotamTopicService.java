package com.github.swim_developer.validator.dnotam.consumer.application.usecase;

import com.github.swim_developer.validator.core.domain.model.TopicData;
import com.github.swim_developer.validator.core.domain.model.TopicDetails;
import com.github.swim_developer.validator.core.domain.model.TopicSummary;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.github.swim_developer.validator.dnotam.consumer.domain.port.in.DnotamTopicPort;

@ApplicationScoped
public class DnotamTopicService implements DnotamTopicPort {

    private static final String SCOPE_IFR_AIRPORTS = "IFR Airports";
    private static final String SCOPE_CP1_AIRPORTS = "CP1 Airports";
    private static final String USE_CASE_TAKE_OFF_LANDING = "Take-off Landing";

    private static final Map<String, TopicData> TOPIC_CATALOG = new LinkedHashMap<>();

    static {
        TOPIC_CATALOG.put("RUNWAY_CLOSURE", new TopicData(
                "Runway Closure",
                "Temporary or permanent closure of runways",
                "Temporary or permanent closure of runways affecting take-off and landing operations",
                List.of("RunwayDirection", "Runway"),
                List.of(SCOPE_IFR_AIRPORTS, SCOPE_CP1_AIRPORTS),
                List.of("ePIB", USE_CASE_TAKE_OFF_LANDING)
        ));
        TOPIC_CATALOG.put("TAXIWAY_CLOSURE", new TopicData(
                "Taxiway Closure",
                "Closure of taxiways affecting ground movement",
                "Closure of taxiways affecting ground movement and taxi routes at aerodromes",
                List.of("Taxiway", "TaxiwayElement"),
                List.of(SCOPE_IFR_AIRPORTS, SCOPE_CP1_AIRPORTS),
                List.of("ePIB", USE_CASE_TAKE_OFF_LANDING)
        ));
        TOPIC_CATALOG.put("STAND_STATUS", new TopicData(
                "Stand Status",
                "Aircraft stand availability and limitations",
                "Changes to aircraft stand availability, reservation, or operational status",
                List.of("AircraftStand", "ApronElement"),
                List.of(SCOPE_IFR_AIRPORTS),
                List.of("ePIB")
        ));
        TOPIC_CATALOG.put("OBSTACLE_NEW", new TopicData(
                "Obstacle New",
                "New obstacles affecting flight operations",
                "Temporary or permanent obstacles that may affect departure, arrival, or circuit areas",
                List.of("VerticalStructure", "ObstacleArea"),
                List.of(SCOPE_IFR_AIRPORTS, SCOPE_CP1_AIRPORTS),
                List.of("ePIB", USE_CASE_TAKE_OFF_LANDING)
        ));
        TOPIC_CATALOG.put("NAVAID_UNSERVICEABLE", new TopicData(
                "Navaid Unserviceable",
                "Navigation aid partial or total unavailability",
                "VOR, DME, ILS, NDB, or other navaid components reported unserviceable",
                List.of("NavaidEquipment", "NavigationSystemCheckpoint"),
                List.of(SCOPE_IFR_AIRPORTS, "En-route"),
                List.of("ePIB", USE_CASE_TAKE_OFF_LANDING)
        ));
        TOPIC_CATALOG.put("AIRSPACE_ACTIVATION", new TopicData(
                "ATS Airspace Activation",
                "Activation or change of ATS airspace",
                "Activation, deactivation, or change of limits for controlled or special-use airspace",
                List.of("Airspace", "AirspaceVolume"),
                List.of("ANSP", "IFR Operations"),
                List.of("Airspace Status VFR", "Flow Management")
        ));
    }

    public List<TopicSummary> getAllTopics() {
        return TOPIC_CATALOG.entrySet().stream()
                .map(e -> new TopicSummary(e.getKey(), e.getValue().title(), e.getValue().summaryDescription()))
                .toList();
    }

    public Optional<TopicDetails> getTopicDetails(String topicId) {
        TopicData data = TOPIC_CATALOG.get(topicId);
        if (data == null) {
            return Optional.empty();
        }
        String scenario = switch (topicId) {
            case "RUNWAY_CLOSURE" -> "RWY.CLS";
            case "TAXIWAY_CLOSURE" -> "TWY.CLS";
            case "STAND_STATUS" -> "STAND.LIM";
            case "OBSTACLE_NEW" -> "OBS.NEW";
            case "NAVAID_UNSERVICEABLE" -> "NAV.UNS";
            case "AIRSPACE_ACTIVATION" -> "SAA.ACT";
            default -> topicId;
        };
        return Optional.of(new TopicDetails(
                topicId,
                data.title(),
                data.detailedDescription(),
                scenario,
                data.features(),
                data.mandatoryFor(),
                data.useCases()
        ));
    }
}

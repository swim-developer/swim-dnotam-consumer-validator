package com.github.swim_developer.validator.dnotam.consumer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DNOTAM Event Metadata Validation")
class EventMetadataCountValidationTest {

    private static final Path EVENTS_PATH = Paths.get("src/main/resources/aixm-events");
    private static final Pattern SCENARIO_PATTERN = Pattern.compile("<event:scenario>([^<]+)</event:scenario>");

    private static final Set<String> EXPECTED_SCENARIOS = Set.of(
            "AD.CLS", "AD.LIM", "APN.CLS", "NAV.UNS", "OBS.NEW",
            "RWY.CLS", "RWY.LIM", "SAA.ACT", "SAA.NEW", "SFC.CON",
            "STAND.CLS", "STAND.LIM", "WLD.HZD"
    );

    @Test
    @DisplayName("All expected DNOTAM scenarios should have at least one event file")
    void allExpectedScenariosShouldHaveAtLeastOneEventFile() throws IOException {
        Map<String, Integer> scenarioCounts = countScenariosFromFiles();

        assertThat(scenarioCounts.keySet())
                .as("All expected DNOTAM scenarios must be represented")
                .containsAll(EXPECTED_SCENARIOS);

        scenarioCounts.forEach((scenario, count) ->
                assertThat(count)
                        .as("Scenario %s should have at least 1 event file", scenario)
                        .isGreaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Event files folder should not be empty")
    void eventFilesFolderShouldNotBeEmpty() throws IOException {
        long totalFiles = Files.list(EVENTS_PATH)
                .filter(p -> p.toString().endsWith(".xml"))
                .count();

        assertThat(totalFiles)
                .as("aixm-events folder should contain XML files")
                .isGreaterThan(0);
    }

    private Map<String, Integer> countScenariosFromFiles() throws IOException {
        Map<String, Integer> counts = new HashMap<>();

        List<Path> xmlFiles = Files.list(EVENTS_PATH)
                .filter(p -> p.toString().endsWith(".xml"))
                .toList();

        for (Path file : xmlFiles) {
            String content = Files.readString(file);
            Matcher matcher = SCENARIO_PATTERN.matcher(content);
            if (matcher.find()) {
                counts.merge(matcher.group(1), 1, Integer::sum);
            }
        }

        return counts;
    }
}

package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.github.swim_developer.validator.consumer.domain.port.in.ManageSubscriptionPort;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.dnotam.consumer.domain.port.in.DnotamEventMetadataPort;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("/ui")
@Produces(MediaType.TEXT_HTML)
public class DnotamUiResource {

    private static final String DATA_KEY_ACTIVE = "active";
    private static final String DATA_KEY_BROKER_CONNECTED = "brokerConnected";
    private static final String DATA_KEY_BROKER_INFO = "brokerInfo";
    private static final String DATA_KEY_SCHEDULER_ENABLED = "schedulerEnabled";
    private static final String DATA_KEY_VERSION = "version";

    private final Template dashboard;
    private final Template subscriptions;
    private final Template topics;
    private final Template events;
    private final Template load;
    private final Template scenarios;
    private final Template sendEvent;
    private final ManageSubscriptionPort subscriptionService;
    private final EventGeneratorPort eventGeneratorService;
    private final DnotamEventMetadataPort eventMetadataService;
    private final String eventGeneratorSchedule;
    private final String eventsPath;
    private final String appVersion;

    @Inject
    public DnotamUiResource(
            Template dashboard,
            Template subscriptions,
            Template topics,
            Template events,
            Template load,
            Template scenarios,
            @io.quarkus.qute.Location("send-event.html") Template sendEvent,
            ManageSubscriptionPort subscriptionService,
            EventGeneratorPort eventGeneratorService,
            DnotamEventMetadataPort eventMetadataService,
            @ConfigProperty(name = "event.generator.schedule", defaultValue = "0 */1 * * * ?") String eventGeneratorSchedule,
            @ConfigProperty(name = "event.generator.events.path", defaultValue = "/opt/events") String eventsPath,
            @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0") String appVersion) {
        this.dashboard = dashboard;
        this.subscriptions = subscriptions;
        this.topics = topics;
        this.events = events;
        this.load = load;
        this.scenarios = scenarios;
        this.sendEvent = sendEvent;
        this.subscriptionService = subscriptionService;
        this.eventGeneratorService = eventGeneratorService;
        this.eventMetadataService = eventMetadataService;
        this.eventGeneratorSchedule = eventGeneratorSchedule;
        this.eventsPath = eventsPath;
        this.appVersion = appVersion;
    }

    @GET
    public TemplateInstance index() {
        return dashboard
                .data(DATA_KEY_ACTIVE, "dashboard")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data("activeSubscriptions", subscriptionService.countActive())
                .data("totalSubscriptions", subscriptionService.countAll())
                .data("eventFilesCount", countEventFiles())
                .data("eventGeneratorEnabled", eventGeneratorService.isSchedulerEnabled())
                .data("eventGeneratorSchedule", eventGeneratorSchedule)
                .data("eventsPath", eventsPath)
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/subscriptions")
    public TemplateInstance subscriptionsPage() {
        return subscriptions
                .data(DATA_KEY_ACTIVE, "subscriptions")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data("subscriptions", subscriptionService.listAll())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/topics")
    public TemplateInstance topicsPage() {
        return topics
                .data(DATA_KEY_ACTIVE, "topics")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/events")
    public TemplateInstance eventsPage() {
        return events
                .data(DATA_KEY_ACTIVE, "events")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data("files", eventMetadataService.getAllEventMetadata())
                .data("filterOptions", eventMetadataService.getFilterOptions())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/load")
    public TemplateInstance loadPage() {
        return load
                .data(DATA_KEY_ACTIVE, "load")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/scenarios")
    public TemplateInstance scenariosPage() {
        return scenarios
                .data(DATA_KEY_ACTIVE, "scenarios")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/send-event")
    public TemplateInstance sendEventPage() {
        return sendEvent
                .data(DATA_KEY_ACTIVE, "send-event")
                .data(DATA_KEY_BROKER_CONNECTED, eventGeneratorService.isAmqpConnected())
                .data(DATA_KEY_BROKER_INFO, eventGeneratorService.getBrokerInfo())
                .data(DATA_KEY_SCHEDULER_ENABLED, eventGeneratorService.isSchedulerEnabled())
                .data(DATA_KEY_VERSION, appVersion);
    }

    @GET
    @Path("/events/preview")
    @Produces(MediaType.TEXT_HTML)
    public String previewEventFile(@QueryParam("file") String filename) {
        try {
            java.nio.file.Path filePath = Paths.get(eventsPath, filename);
            if (!Files.exists(filePath)) {
                return "<div class=\"alert alert-error\">File not found</div>";
            }
            String content = Files.readString(filePath);
            String escaped = content
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            return "<pre><code class=\"language-xml\">" + escaped + "</code></pre>" +
                   "<script>Prism.highlightAll();</script>";
        } catch (IOException e) {
            return "<div class=\"alert alert-error\">Error reading file: " + e.getMessage() + "</div>";
        }
    }

    private long countEventFiles() {
        try {
            java.nio.file.Path dir = Paths.get(eventsPath);
            if (!Files.exists(dir)) {
                return 0;
            }
            try (var files = Files.list(dir)) {
                return files
                        .filter(p -> p.toString().endsWith(".xml"))
                        .count();
            }
        } catch (IOException e) {
            return 0;
        }
    }
}

package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.github.swim_developer.validator.consumer.domain.port.out.SubscriptionRepository;
import com.github.swim_developer.validator.core.domain.model.SubscriptionStatus;
import com.github.swim_developer.validator.core.infrastructure.console.ConsoleEvent;
import com.github.swim_developer.validator.core.infrastructure.console.ConsoleNotificationService;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.core.infrastructure.fault.FaultInjectionService;
import com.github.swim_developer.validator.core.infrastructure.fault.FaultRequest;
import com.github.swim_developer.validator.consumer.domain.port.in.HeartbeatPort;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminControlResource {

    private static final String HTML_DIV_CLOSE = "</div>";
    private static final String HTML_DIV_TRIGGER_SUCCESS_OPEN = "<div class=\"trigger-success\">";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final EventGeneratorPort eventGeneratorService;
    private final SubscriptionRepository subscriptionRepository;
    private final ConsoleNotificationService consoleNotificationService;
    private final HeartbeatPort heartbeatPublisher;
    private final FaultInjectionService faultInjectionService;

    @Inject
    public AdminControlResource(
            EventGeneratorPort eventGeneratorService,
            SubscriptionRepository subscriptionRepository,
            ConsoleNotificationService consoleNotificationService,
            HeartbeatPort heartbeatPublisher,
            FaultInjectionService faultInjectionService) {
        this.eventGeneratorService = eventGeneratorService;
        this.subscriptionRepository = subscriptionRepository;
        this.consoleNotificationService = consoleNotificationService;
        this.heartbeatPublisher = heartbeatPublisher;
        this.faultInjectionService = faultInjectionService;
    }

    @Path("/scheduler/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSchedulerStatus() {
        return "{\"enabled\":" + eventGeneratorService.isSchedulerEnabled() + "}";
    }

    @Path("/scheduler/toggle")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String toggleScheduler() {
        boolean newState = eventGeneratorService.toggleScheduler();
        String stateText = newState ? "enabled" : "disabled";
        String stateClass = newState ? "trigger-success" : "trigger-error";
        consoleNotificationService.info("Scheduler " + stateText);
        return "<div class=\"" + stateClass + "\">Scheduler " + stateText + HTML_DIV_CLOSE;
    }

    @Path("/status/amqp")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getAmqpStatusFragment() {
        boolean connected = eventGeneratorService.isAmqpConnected();
        String info = eventGeneratorService.getBrokerInfo();
        String indicatorClass = connected ? "connected" : "disconnected";
        return "<span class=\"status-indicator " + indicatorClass + "\"></span><span>AMQP: " + info + "</span>";
    }

    @Path("/status/scheduler")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getSchedulerStatusFragment() {
        boolean enabled = eventGeneratorService.isSchedulerEnabled();
        String checked = enabled ? "checked" : "";
        return "<span class=\"switch-label\">Auto Events</span>" +
                "<label class=\"switch\">" +
                "<input type=\"checkbox\" id=\"scheduler-toggle\" " + checked + " hx-post=\"/admin/scheduler/toggle\" hx-swap=\"none\">" +
                "<span class=\"switch-slider\"></span>" +
                "</label>";
    }

    @Path("/status/stats")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getStatsFragment() {
        long active = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE).size();
        long total = subscriptionRepository.count();
        return "<div class=\"stat-card\"><div class=\"stat-value\">" + active + "</div><div class=\"stat-label\">Active Subscriptions</div></div>" +
                "<div class=\"stat-card\"><div class=\"stat-value\">" + total + "</div><div class=\"stat-label\">Total Subscriptions</div></div>";
    }

    @Path("/status/system")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getSystemStatusFragment() {
        boolean brokerConnected = eventGeneratorService.isAmqpConnected();
        String brokerInfo = eventGeneratorService.getBrokerInfo();
        boolean schedulerEnabled = eventGeneratorService.isSchedulerEnabled();

        String brokerBadge = brokerConnected ? "badge-success" : "badge-danger";
        String brokerText = brokerConnected ? "Connected" : "Disconnected";
        String schedulerBadge = schedulerEnabled ? "badge-success" : "badge-warning";
        String schedulerText = schedulerEnabled ? "Enabled" : "Disabled";

        return "<tr><td style=\"width: 200px;\">AMQP Broker</td>" +
                "<td><span class=\"badge " + brokerBadge + "\">" + brokerText + "</span></td>" +
                "<td>" + brokerInfo + "</td></tr>" +
                "<tr><td>Event Generator</td>" +
                "<td><span class=\"badge " + schedulerBadge + "\">" + schedulerText + "</span></td>" +
                "<td>Runtime status (toggle in footer)</td></tr>";
    }

    @Path("/heartbeat/stop")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String stopHeartbeat() {
        heartbeatPublisher.stop();
        consoleNotificationService.warning("Heartbeat STOPPED - simulating provider down");
        return "<div class=\"trigger-error\">" + timestamp() + " Heartbeat publisher stopped" + HTML_DIV_CLOSE;
    }

    @Path("/heartbeat/start")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String startHeartbeat() {
        heartbeatPublisher.start();
        consoleNotificationService.success("Heartbeat STARTED - provider recovered");
        return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " Heartbeat publisher started" + HTML_DIV_CLOSE;
    }

    @Path("/heartbeat/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getHeartbeatStatus() {
        return "{\"running\":" + heartbeatPublisher.isRunning() + "}";
    }

    @Path("/faults/inject")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String injectFault(FaultRequest request) {
        try {
            String faultId = faultInjectionService.addFault(
                    request.pathPattern(), request.httpMethod(), request.httpStatus(),
                    request.delayMs(), request.dropRate(), request.durationSeconds()
            );
            consoleNotificationService.warning("Fault injected: " + faultId + " - " + request.pathPattern());
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " Fault injected: " + faultId + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to inject fault", e);
            consoleNotificationService.error("Fault injection failed: " + e.getMessage());
            return "<div class=\"trigger-error\">" + timestamp() + " ✗ " + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/faults/clear")
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String clearFaults() {
        faultInjectionService.clearAll();
        consoleNotificationService.info("All fault injections cleared");
        return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " All faults cleared" + HTML_DIV_CLOSE;
    }

    @Path("/faults/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String listFaults() {
        var faults = faultInjectionService.listActiveFaults();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < faults.size(); i++) {
            var f = faults.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                    .append("\"id\":\"").append(f.id()).append("\",")
                    .append("\"pathPattern\":\"").append(f.pathPattern()).append("\",")
                    .append("\"httpMethod\":\"").append(f.httpMethod()).append("\",")
                    .append("\"httpStatus\":").append(f.httpStatus()).append(",")
                    .append("\"delayMs\":").append(f.delayMs()).append(",")
                    .append("\"dropRate\":").append(f.dropRate()).append(",")
                    .append("\"expiresAt\":\"").append(f.expiresAt()).append("\",")
                    .append("\"expired\":").append(f.expired())
                    .append("}");
        }
        json.append("]");
        return json.toString();
    }

    @Path("/console/stream")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> consoleStream() {
        return consoleNotificationService.getStream()
                .onItem().transform(ConsoleEvent::toJson);
    }

    private String timestamp() {
        return "<span class=\"timestamp\">[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "]</span>";
    }
}

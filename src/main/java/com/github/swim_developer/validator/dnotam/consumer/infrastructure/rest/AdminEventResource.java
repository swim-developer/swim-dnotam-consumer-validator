package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.github.swim_developer.validator.consumer.domain.port.out.SubscriptionRepository;
import com.github.swim_developer.validator.core.infrastructure.console.ConsoleNotificationService;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.consumer.domain.port.in.XmlFileCachePort;
import com.github.swim_developer.validator.consumer.domain.port.in.LoadTestPort;
import com.github.swim_developer.validator.consumer.domain.model.ScenarioPreview;
import com.github.swim_developer.validator.consumer.domain.port.in.ScenarioPreviewPort;
import com.github.swim_developer.validator.core.infrastructure.util.XmlDateRandomizer;
import com.github.swim_developer.validator.dnotam.consumer.infrastructure.util.XmlIdRandomizer;
import com.github.swim_developer.validator.core.infrastructure.util.XmlLocationRandomizer;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
public class AdminEventResource {

    private static final String XML_AMP = "&amp;";
    private static final String HTML_DIV_CLOSE = "</div>";
    private static final String HTML_DIV_TRIGGER_ERROR_OPEN = "<div class=\"trigger-error\">";
    private static final String HTML_DIV_TRIGGER_SUCCESS_OPEN = "<div class=\"trigger-success\">";
    private static final String HTML_DIV_SEND_ERROR_OPEN = "<div class=\"send-error\">";
    private static final String LITERAL_SUBSCRIPTIONS = " subscription(s)";
    private static final String HTML_DIV_TRIGGER_ERROR_ERROR_PREFIX = "<div class=\"trigger-error\">Error: ";
    private static final String HTML_MSG_FAILURE_PREFIX = " ✗ ";
    private static final String NO_ACTIVE_SUBSCRIPTIONS_END = " No active subscriptions</div>";
    private static final String BREAK_MARKER = ScenarioPreview.BREAK_MARKER
            .replace("&", XML_AMP).replace("<", "&lt;").replace(">", "&gt;");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final EventGeneratorPort eventGeneratorService;
    private final XmlFileCachePort xmlEventFileLoader;
    private final LoadTestPort loadTestService;
    private final ScenarioPreviewPort scenarioService;
    private final SubscriptionRepository subscriptionRepository;
    private final ConsoleNotificationService consoleNotificationService;
    private final XmlDateRandomizer xmlDateRandomizer;
    private final XmlIdRandomizer xmlIdRandomizer;
    private final XmlLocationRandomizer xmlLocationRandomizer;

    @Inject
    public AdminEventResource(
            EventGeneratorPort eventGeneratorService,
            XmlFileCachePort xmlEventFileLoader,
            LoadTestPort loadTestService,
            ScenarioPreviewPort scenarioService,
            SubscriptionRepository subscriptionRepository,
            ConsoleNotificationService consoleNotificationService,
            XmlDateRandomizer xmlDateRandomizer,
            XmlIdRandomizer xmlIdRandomizer,
            XmlLocationRandomizer xmlLocationRandomizer) {
        this.eventGeneratorService = eventGeneratorService;
        this.xmlEventFileLoader = xmlEventFileLoader;
        this.loadTestService = loadTestService;
        this.scenarioService = scenarioService;
        this.subscriptionRepository = subscriptionRepository;
        this.consoleNotificationService = consoleNotificationService;
        this.xmlDateRandomizer = xmlDateRandomizer;
        this.xmlIdRandomizer = xmlIdRandomizer;
        this.xmlLocationRandomizer = xmlLocationRandomizer;
    }

    @Path("/refresh-cache")
    @DELETE
    public void refreshCache() {
        xmlEventFileLoader.clearXmlCache();
    }

    @Path("/load")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> executeLoad(@QueryParam("duration") @DefaultValue("10s") String duration) {
        return loadTestService.executeLoad(duration)
                .onItem().transform(status -> status + "\n");
    }

    @Path("/trigger")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String triggerSpecificEvent(
            @QueryParam("file") String filename,
            @QueryParam("count") @DefaultValue("1") int messageCount,
            @QueryParam("randomizeDates") @DefaultValue("false") boolean randomizeDates,
            @QueryParam("useCurrentUtc") @DefaultValue("false") boolean useCurrentUtc,
            @QueryParam("randomizeLocation") @DefaultValue("false") boolean randomizeLocation,
            @QueryParam("randomizeId") @DefaultValue("true") boolean randomizeId) {
        try {
            int effectiveCount = Math.clamp(messageCount, 1, 10000);
            String baseXml = xmlEventFileLoader.readEventFile(filename);
            int totalSent = 0;
            int subscriptionCount = 0;

            for (int i = 0; i < effectiveCount; i++) {
                String xml = baseXml;
                if (randomizeId) xml = xmlIdRandomizer.randomizeIds(xml);
                if (useCurrentUtc) xml = xmlDateRandomizer.applyCurrentUtcDates(xml);
                else if (randomizeDates) xml = xmlDateRandomizer.randomizeDates(xml);
                if (randomizeLocation) xml = xmlLocationRandomizer.randomizeLocation(xml);
                subscriptionCount = eventGeneratorService.sendScenarioEvent(xml, filename);
                if (subscriptionCount > 0) totalSent++;
            }

            if (subscriptionCount == 0) {
                consoleNotificationService.warning("Trigger: No active subscriptions");
                return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + " No active subscriptions to receive the event" + HTML_DIV_CLOSE;
            }

            String flags = buildFlagsLabel(randomizeDates, useCurrentUtc, randomizeLocation, randomizeId);
            String message = effectiveCount == 1
                    ? "✓ Event sent to " + subscriptionCount + LITERAL_SUBSCRIPTIONS + flags
                    : "✓ " + totalSent + " message(s) sent to " + subscriptionCount + LITERAL_SUBSCRIPTIONS + flags;

            consoleNotificationService.success("Trigger: " + filename + " → " + totalSent + "x" + subscriptionCount + LITERAL_SUBSCRIPTIONS + flags);
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " " + message + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to trigger event", e);
            consoleNotificationService.error("Trigger failed: " + e.getMessage());
            return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/malformed")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String triggerMalformedEvent() {
        try {
            int count = eventGeneratorService.sendScenarioEvent(scenarioService.getMalformedXmlToSend(), "malformed");
            if (count == 0) return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + NO_ACTIVE_SUBSCRIPTIONS_END;
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " ✓ Malformed event sent to " + count + LITERAL_SUBSCRIPTIONS + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to trigger malformed event", e);
            return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/duplicate")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String triggerDuplicateEvent() {
        try {
            String xml = scenarioService.getDuplicateXmlToSend();
            eventGeneratorService.sendDuplicateScenarioEvent(xml);
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " ✓ First message sent. Duplicate will be sent in 10 seconds..." + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to trigger duplicate event", e);
            return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/multiple")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String triggerMultipleMessages() {
        try {
            int count = eventGeneratorService.sendScenarioEvent(scenarioService.getMultipleMessagesToSend(), "multiple");
            if (count == 0) return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + NO_ACTIVE_SUBSCRIPTIONS_END;
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " ✓ Multiple messages sent to " + count + LITERAL_SUBSCRIPTIONS + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to trigger multiple messages", e);
            return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/multiple-error")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public String triggerMultipleMessagesWithError() {
        try {
            int count = eventGeneratorService.sendScenarioEvent(scenarioService.getMultipleMessagesWithErrorToSend(), "multiple-error");
            if (count == 0) return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + NO_ACTIVE_SUBSCRIPTIONS_END;
            return HTML_DIV_TRIGGER_SUCCESS_OPEN + timestamp() + " ✓ Multiple messages (1 with error) sent to " + count + LITERAL_SUBSCRIPTIONS + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to trigger multiple messages with error", e);
            return HTML_DIV_TRIGGER_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/send-custom-event")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_HTML)
    public String sendCustomEvent(String xmlContent) {
        try {
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return HTML_DIV_SEND_ERROR_OPEN + timestamp() + " XML content cannot be empty" + HTML_DIV_CLOSE;
            }
            int count = eventGeneratorService.sendScenarioEvent(xmlContent, "custom");
            if (count == 0) {
                return HTML_DIV_SEND_ERROR_OPEN + timestamp() + " No active subscriptions to receive the event" + HTML_DIV_CLOSE;
            }
            return "<div class=\"send-success\">" + timestamp() + " ✓ Event sent to " + count + LITERAL_SUBSCRIPTIONS + HTML_DIV_CLOSE;
        } catch (Exception e) {
            log.error("Failed to send custom event", e);
            return HTML_DIV_SEND_ERROR_OPEN + timestamp() + HTML_MSG_FAILURE_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/malformed/preview")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String previewMalformedEvent() {
        try {
            ScenarioPreview preview = scenarioService.getMalformedPreview();
            return formatXmlPreviewWithBreakMarker(preview.xml());
        } catch (Exception e) {
            return HTML_DIV_TRIGGER_ERROR_ERROR_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/duplicate/preview")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String previewDuplicateEvent() {
        try {
            ScenarioPreview preview = scenarioService.getDuplicatePreview();
            return formatXmlPreview(preview.xml());
        } catch (Exception e) {
            return HTML_DIV_TRIGGER_ERROR_ERROR_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/multiple/preview")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String previewMultipleMessages() {
        try {
            ScenarioPreview preview = scenarioService.getMultipleMessagesPreview();
            return formatXmlPreview(preview.xml());
        } catch (Exception e) {
            return HTML_DIV_TRIGGER_ERROR_ERROR_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/scenario/multiple-error/preview")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String previewMultipleMessagesWithError() {
        try {
            ScenarioPreview preview = scenarioService.getMultipleMessagesWithErrorPreview();
            return formatXmlPreviewWithBreakMarker(preview.xml());
        } catch (Exception e) {
            return HTML_DIV_TRIGGER_ERROR_ERROR_PREFIX + e.getMessage() + HTML_DIV_CLOSE;
        }
    }

    @Path("/reset-subscriptions")
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public String resetSubscriptions() {
        long count = subscriptionRepository.count();
        subscriptionRepository.deleteAll();
        log.info("Reset all subscriptions, cleared {} subscription(s)", count);
        consoleNotificationService.warning("Reset: Cleared " + count + LITERAL_SUBSCRIPTIONS);
        return HTML_DIV_TRIGGER_SUCCESS_OPEN + "✓ Cleared " + count + LITERAL_SUBSCRIPTIONS + HTML_DIV_CLOSE;
    }

    private String buildFlagsLabel(boolean randomizeDates, boolean useCurrentUtc, boolean randomizeLocation, boolean randomizeId) {
        if (!randomizeDates && !useCurrentUtc && !randomizeLocation && !randomizeId) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" [");
        boolean hasFlag = false;
        if (randomizeId) {
            sb.append("ID randomized");
            hasFlag = true;
        }
        if (useCurrentUtc) {
            if (hasFlag) {
                sb.append("+");
            }
            sb.append("current UTC");
            hasFlag = true;
        } else if (randomizeDates) {
            if (hasFlag) {
                sb.append("+");
            }
            sb.append("dates randomized");
            hasFlag = true;
        }
        if (randomizeLocation) {
            if (hasFlag) {
                sb.append("+");
            }
            sb.append("location randomized");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatXmlPreview(String xml) {
        String escaped = xml.replace("&", XML_AMP).replace("<", "&lt;").replace(">", "&gt;");
        return "<pre><code class=\"language-xml\">" + escaped + "</code></pre><script>Prism.highlightAll();</script>";
    }

    private String formatXmlPreviewWithBreakMarker(String xml) {
        String escaped = xml.replace("&", XML_AMP).replace("<", "&lt;").replace(">", "&gt;");
        String highlighted = escaped.replace(BREAK_MARKER, "<span class=\"error-highlight\">" + BREAK_MARKER + "</span>");
        return "<pre><code class=\"language-xml\">" + highlighted + "</code></pre><script>Prism.highlightAll();</script>";
    }

    private String timestamp() {
        return "<span class=\"timestamp\">[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "]</span>";
    }
}

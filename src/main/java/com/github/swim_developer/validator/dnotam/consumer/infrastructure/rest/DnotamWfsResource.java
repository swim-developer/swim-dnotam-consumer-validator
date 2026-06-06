package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.github.swim_developer.validator.core.infrastructure.rest.dto.ErrorResponse;
import com.github.swim_developer.validator.core.infrastructure.rest.dto.TopicList;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.dnotam.consumer.domain.port.in.DnotamTopicPort;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Slf4j
@Path("/swim/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
public class DnotamWfsResource {

    private final DnotamTopicPort topicService;
    private final EventGeneratorPort eventGeneratorService;

    @Inject
    public DnotamWfsResource(DnotamTopicPort topicService, EventGeneratorPort eventGeneratorService) {
        this.topicService = topicService;
        this.eventGeneratorService = eventGeneratorService;
    }

    @GET
    @Path("/topics")
    @Tag(name = "Topics")
    @Operation(summary = "Get list of available topics", description = "Returns Digital NOTAM event scenario topics available for subscription.")
    @APIResponse(responseCode = "200", description = "List of topics",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TopicList.class)))
    public Response getTopics() {
        return Response.ok(new TopicList(topicService.getAllTopics())).build();
    }

    @GET
    @Path("/topics/{topicId}")
    @Tag(name = "Topics")
    @Operation(summary = "Get topic details")
    @APIResponse(responseCode = "200", description = "Topic details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = com.github.swim_developer.validator.core.domain.model.TopicDetails.class)))
    @APIResponse(responseCode = "404", description = "Not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getTopicDetails(
            @PathParam("topicId") @Parameter(description = "Topic identifier", required = true, example = "RUNWAY_CLOSURE") String topicId) {
        return topicService.getTopicDetails(topicId)
                .<Response>map(details -> Response.ok(details).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NOT_FOUND", "Topic not found", "Topic ID: " + topicId))
                        .build());
    }

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_XML)
    @Tag(name = "Request Interface (WFS)")
    @Operation(summary = "Request Digital NOTAMs (WFS GetFeature)", description = "OGC Web Feature Service 2.0 interface for querying current Digital NOTAMs. Returns AIXM 5.1.1 Basic Message (XML).")
    @APIResponse(responseCode = "200", description = "AIXM 5.1.1 Basic Message",
            content = @Content(mediaType = MediaType.APPLICATION_XML))
    public Response getFeature(
            @QueryParam("typeName") @Parameter(description = "Feature type (event:Event)", required = true) String typeName,
            @QueryParam("filter") @Parameter(description = "OGC Filter Encoding 2.0 expression") String filter,
            @QueryParam("validTime") @Parameter(description = "Validity time (WFS-TE)", schema = @Schema(format = "date-time")) String validTime) {
        String aixmMessage = """
                <?xml version="1.0" encoding="UTF-8"?>
                <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1/message"
                                         xmlns:event="http://www.aixm.aero/schema/5.1/event"
                                         xmlns:aixm="http://www.aixm.aero/schema/5.1"
                                         xmlns:gml="http://www.opengis.net/gml/3.2">
                    <message:hasMember>
                        <event:Event gml:id="EVENT_STUB_001">
                            <event:timeSlice>
                                <event:scenario>RWY.CLS</event:scenario>
                            </event:timeSlice>
                        </event:Event>
                    </message:hasMember>
                </message:AIXMBasicMessage>
                """;
        return Response.ok(aixmMessage).build();
    }

    @GET
    @Path("/trigger-event")
    @Produces(MediaType.APPLICATION_XML)
    @Tag(name = "Event Generator")
    @Operation(summary = "Manually trigger event generation", description = "Publishes a sample AIXM event to all active subscriptions.")
    @APIResponse(responseCode = "200", description = "Event generated",
            content = @Content(mediaType = MediaType.APPLICATION_XML))
    @APIResponse(responseCode = "500", description = "Failed to generate event",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response triggerEvent() {
        try {
            return eventGeneratorService.generateAndSendEventManually()
                    .<Response>map(xml -> Response.ok(xml).build())
                    .orElseGet(() -> Response.noContent().build());
        } catch (Exception e) {
            log.error("Failed to trigger event", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("EVENT_GENERATION_FAILED", "Failed to generate event", e.getMessage()))
                    .build();
        }
    }
}

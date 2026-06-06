package com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest;

import com.github.swim_developer.validator.core.infrastructure.rest.dto.ErrorResponse;
import com.github.swim_developer.validator.core.infrastructure.rest.dto.SubscriptionList;
import com.github.swim_developer.validator.core.domain.model.SubscriptionResponse;
import com.github.swim_developer.validator.core.infrastructure.rest.dto.SubscriptionStatusUpdate;
import com.github.swim_developer.validator.consumer.domain.model.CreateSubscriptionCommand;
import com.github.swim_developer.validator.consumer.domain.port.in.ManageSubscriptionPort;
import com.github.swim_developer.validator.dnotam.consumer.infrastructure.rest.dto.DnotamSubscriptionRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Slf4j
@Path("/swim/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
public class DnotamSubscriptionResource {

    private final ManageSubscriptionPort subscriptionService;

    @Inject
    public DnotamSubscriptionResource(ManageSubscriptionPort subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @POST
    @Path("/subscriptions")
    @Tag(name = "Subscriptions", description = "Subscription lifecycle management")
    @Operation(
            operationId = "subscribe",
            summary = "Create a new subscription",
            description = "Creates a subscription for Digital NOTAM event distribution. Default status is PAUSED until activated."
    )
    @APIResponse(
            responseCode = "201",
            description = "Subscription created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))
    )
    public Response createSubscription(
            @Valid @RequestBody(
                    description = "Subscription request payload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = DnotamSubscriptionRequest.class),
                            examples = {
                                    @ExampleObject(name = "simpleSubscription", summary = "Basic subscription",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"eventScenario\": [\"RWY.CLS\"], \"airportHeliport\": [\"EADD\"], \"description\": \"Runway closure events for Donlon Airport\"}"),
                                    @ExampleObject(name = "reuseExistingQueue", summary = "Add filters to existing queue",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"queueName\": \"DNOTAM-client01-550e8400-e29b-41d4-a716-446655440000\", \"eventScenario\": [\"SAA.ACT\"], \"airspace\": [\"EAAD\"]}"),
                                    @ExampleObject(name = "fullSubscription", summary = "Complete subscription with all fields",
                                            value = "{\"topic\": \"DigitalNOTAMService\", \"qos\": \"EXACTLY_ONCE\", \"durable\": true, \"eventScenario\": [\"RWY.CLS\", \"SAA.ACT\"], \"airportHeliport\": [\"EHAM\"], \"publisher\": \"EUROCONTROL\"}")
                            }
                    )
            ) DnotamSubscriptionRequest request) {
        SubscriptionResponse subscription = subscriptionService.createSubscription(
                new CreateSubscriptionCommand(
                        request.topic(), request.queueName(), request.qos(), request.durable(),
                        request.eventScenario(), request.airportHeliport(), request.airspace(),
                        request.eventSeries(), request.publisher(), request.description(), request.comment()
                ));
        return Response.status(Response.Status.CREATED).entity(subscription).build();
    }

    @GET
    @Path("/subscriptions")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "getSubscriptions", summary = "Get list of subscriptions")
    @APIResponse(responseCode = "200", description = "List of subscriptions",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionList.class)))
    public Response getSubscriptions(
            @QueryParam("queueName") @Parameter(description = "Filter by queue name") String queueName,
            @QueryParam("subscriptionStatus") @Parameter(description = "Filter by status: ACTIVE, PAUSED, SUSPENDED, TERMINATED, DELETED, INVALID") String subscriptionStatusParam) {
        com.github.swim_developer.validator.core.domain.model.SubscriptionStatus status = null;
        if (subscriptionStatusParam != null) {
            try {
                status = com.github.swim_developer.validator.core.domain.model.SubscriptionStatus.valueOf(subscriptionStatusParam);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid subscription status parameter: {}", subscriptionStatusParam);
            }
        }
        SubscriptionList list = new SubscriptionList(subscriptionService.listSubscriptions(queueName, status));
        return Response.ok(list).build();
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "updateSubscriptionStatus", summary = "Update subscription status")
    @APIResponse(responseCode = "200", description = "Updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response updateSubscriptionStatus(
            @PathParam("subscriptionId") @Parameter(description = "Subscription UUID", required = true) String subscriptionId,
            @Valid @RequestBody(
                    description = "Status update",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SubscriptionStatusUpdate.class),
                            examples = {
                                    @ExampleObject(name = "activate", value = "{\"subscriptionStatus\": \"ACTIVE\"}"),
                                    @ExampleObject(name = "pause", value = "{\"subscriptionStatus\": \"PAUSED\"}"),
                                    @ExampleObject(name = "cancel", value = "{\"subscriptionStatus\": \"DELETED\"}")
                            }
                    )
            ) SubscriptionStatusUpdate update) {
        SubscriptionResponse subscription = subscriptionService.updateSubscriptionStatus(subscriptionId, update.subscriptionStatus());
        return Response.ok(subscription).build();
    }

    @GET
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "getSubscription", summary = "Get subscription details")
    @APIResponse(responseCode = "200", description = "Subscription details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response getSubscriptionDetails(
            @PathParam("subscriptionId") @Parameter(description = "Subscription UUID", required = true) String subscriptionId) {
        return subscriptionService.getSubscriptionDetails(subscriptionId)
                .map(subscription -> Response.ok(subscription).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NOT_FOUND", "Subscription not found", "No subscription exists with id: " + subscriptionId))
                        .build());
    }

    @DELETE
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "unsubscribe", summary = "Delete subscription")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "Not found")
    public Response deleteSubscription(
            @PathParam("subscriptionId") @Parameter(description = "Subscription UUID", required = true) String subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}/renew")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "renewSubscription", summary = "Renew subscription", description = "Extends expiration by 30 days.")
    @APIResponse(responseCode = "200", description = "Renewed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response renewSubscription(
            @PathParam("subscriptionId") @Parameter(description = "Subscription UUID", required = true) String subscriptionId) {
        return subscriptionService.renewSubscription(subscriptionId)
                .map(subscription -> Response.ok(subscription).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NOT_FOUND", "Subscription not found", "No subscription exists with id: " + subscriptionId))
                        .build());
    }
}

package com.github.swim_developer.validator.dnotam.consumer.infrastructure.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "Digital NOTAM (DNOTAM) Subscription and Request Service",
                version = "1.0.0",
                description = """
                        ## Service Abstract

                        The Digital NOTAM Subscription and Request Service allows the service consumer to obtain aeronautical
                        information in accordance with the Digital NOTAM specification. The information conforms to AIXM 5.1.1
                        event scenarios such as runway closures, taxiway closures, and airspace activation.

                        The service consumer may subscribe to the service, specifying the event scenarios of interest.
                        It is also possible to send a direct request to retrieve current Digital NOTAM features via
                        the WFS request interface. The information returned is in the form of an **AIXM 5.1.1** message.

                        ## Operational Context

                        Stakeholders in air traffic management require timely aeronautical data concerning the establishment,
                        condition, or change in any aeronautical facility, service, procedure, or hazard.

                        ## Service Interfaces

                        1. **Subscription Interface** (WS-Light/REST) - Manage subscriptions via REST API
                        2. **Distribution Interface** (AMQP 1.0) - Receive real-time event notifications via message broker
                        3. **Request Interface** (WFS 2.0) - Query current state using OGC Web Feature Service
                        """,
                contact = @Contact(
                        name = "EUROCONTROL",
                        url = "https://eur-registry.swim.aero",
                        email = "swim@eurocontrol.int"
                )
        ),
        servers = {
                @Server(url = "https://swim-dnotam-consumer-validator.apps.ocp4.masales.cloud/swim/v1", description = "Consumer Validator (Development/Testing)"),
                @Server(url = "https://eead.eurocontrol.int/swim/v1", description = "eEAD Production (Example)")
        },
        tags = {
                @Tag(name = "Subscriptions", description = "Subscription lifecycle management"),
                @Tag(name = "Topics", description = "Event scenario catalog"),
                @Tag(name = "Request Interface (WFS)", description = "WFS GetFeature for AIXM Digital NOTAM data")
        }
)
@SecurityScheme(
        securitySchemeName = "mTLS",
        type = SecuritySchemeType.MUTUALTLS,
        description = "Mutual TLS authentication using EACP (European Aviation Common PKI) certificates. All API calls require a valid X.509 client certificate issued by the EACP."
)
public class DnotamOpenApiConfiguration extends Application {
}

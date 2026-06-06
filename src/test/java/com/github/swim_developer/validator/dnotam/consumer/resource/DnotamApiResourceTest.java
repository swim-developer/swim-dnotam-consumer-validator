package com.github.swim_developer.validator.dnotam.consumer.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DnotamApiResourceTest {

    private static String createdSubscriptionId;

    @Test
    @Order(1)
    void shouldCreateSubscription() {
        createdSubscriptionId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "topic": "DigitalNOTAMService",
                            "eventScenario": ["RWY.CLS"],
                            "airportHeliport": ["EADD"],
                            "description": "Test subscription"
                        }
                        """)
                .when()
                .post("/swim/v1/subscriptions")
                .then()
                .statusCode(201)
                .body("subscription_id", notNullValue())
                .body("subscription_status", equalTo("PAUSED"))
                .body("topic", equalTo("DigitalNOTAMService"))
                .body("queue", startsWith("DNOTAM-"))
                .body("qos", equalTo("AT_LEAST_ONCE"))
                .body("durable", equalTo(true))
                .extract()
                .path("subscription_id");

        assertThat(createdSubscriptionId).isNotNull();
    }

    @Test
    @Order(2)
    void shouldListSubscriptions() {
        given()
                .when()
                .get("/swim/v1/subscriptions")
                .then()
                .statusCode(200)
                .body("subscriptions", notNullValue())
                .body("subscriptions.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(3)
    void shouldGetSubscriptionDetails() {
        given()
                .pathParam("subscriptionId", createdSubscriptionId)
                .when()
                .get("/swim/v1/subscriptions/{subscriptionId}")
                .then()
                .statusCode(200)
                .body("subscription_id", equalTo(createdSubscriptionId))
                .body("subscription_status", equalTo("PAUSED"))
                .body("queue", startsWith("DNOTAM-"))
                .body("description", equalTo("Test subscription"));
    }

    @Test
    @Order(4)
    void shouldUpdateSubscriptionStatus() {
        given()
                .contentType(ContentType.JSON)
                .pathParam("subscriptionId", createdSubscriptionId)
                .body("""
                        {
                            "subscription_status": "ACTIVE"
                        }
                        """)
                .when()
                .put("/swim/v1/subscriptions/{subscriptionId}")
                .then()
                .statusCode(200)
                .body("subscription_id", equalTo(createdSubscriptionId))
                .body("subscription_status", equalTo("ACTIVE"));
    }

    @Test
    @Order(5)
    void shouldListAllSubscriptions() {
        given()
                .when()
                .get("/swim/v1/subscriptions")
                .then()
                .statusCode(200)
                .body("subscriptions", notNullValue())
                .body("subscriptions.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(6)
    void shouldDeleteSubscription() {
        given()
                .pathParam("subscriptionId", createdSubscriptionId)
                .when()
                .delete("/swim/v1/subscriptions/{subscriptionId}")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(7)
    void shouldReturnNotFoundForDeletedSubscription() {
        given()
                .pathParam("subscriptionId", createdSubscriptionId)
                .when()
                .get("/swim/v1/subscriptions/{subscriptionId}")
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Subscription not found"));
    }

    @Test
    @Order(8)
    void shouldListTopics() {
        given()
                .when()
                .get("/swim/v1/topics")
                .then()
                .statusCode(200)
                .body("topics", notNullValue())
                .body("topics.size()", equalTo(6))
                .body("topics[0].topicId", notNullValue())
                .body("topics[0].title", notNullValue())
                .body("topics[0].description", notNullValue());
    }

    @Test
    @Order(9)
    void shouldGetTopicDetails() {
        given()
                .pathParam("topicId", "RUNWAY_CLOSURE")
                .when()
                .get("/swim/v1/topics/{topicId}")
                .then()
                .statusCode(200)
                .body("topicId", equalTo("RUNWAY_CLOSURE"))
                .body("title", equalTo("Runway Closure"))
                .body("eventScenario", equalTo("RWY.CLS"))
                .body("features", hasSize(2))
                .body("mandatoryFor", hasSize(2))
                .body("useCase", hasSize(2));
    }

    @Test
    @Order(10)
    void shouldReturnNotFoundForInvalidTopic() {
        given()
                .pathParam("topicId", "INVALID_TOPIC")
                .when()
                .get("/swim/v1/topics/{topicId}")
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Topic not found"));
    }

    @Test
    @Order(11)
    void shouldReturnExistingSubscriptionOnDuplicate() {
        String firstId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "topic": "DigitalNOTAMService",
                            "eventScenario": ["OBS.NEW"],
                            "airportHeliport": ["EHAM"],
                            "description": "Dedup test"
                        }
                        """)
                .when()
                .post("/swim/v1/subscriptions")
                .then()
                .statusCode(201)
                .extract()
                .path("subscription_id");

        String secondId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "topic": "DigitalNOTAMService",
                            "eventScenario": ["OBS.NEW"],
                            "airportHeliport": ["EHAM"],
                            "description": "Duplicate attempt"
                        }
                        """)
                .when()
                .post("/swim/v1/subscriptions")
                .then()
                .statusCode(201)
                .extract()
                .path("subscription_id");

        assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    @Order(12)
    void shouldCreateNewSubscriptionWhenFiltersAreDifferent() {
        String firstId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "topic": "DigitalNOTAMService",
                            "eventScenario": ["NAV.UNS"],
                            "airportHeliport": ["EGLL"],
                            "description": "Different filter test"
                        }
                        """)
                .when()
                .post("/swim/v1/subscriptions")
                .then()
                .statusCode(201)
                .extract()
                .path("subscription_id");

        String secondId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "topic": "DigitalNOTAMService",
                            "eventScenario": ["NAV.UNS"],
                            "airportHeliport": ["LFPG"],
                            "description": "Different airport"
                        }
                        """)
                .when()
                .post("/swim/v1/subscriptions")
                .then()
                .statusCode(201)
                .extract()
                .path("subscription_id");

        assertThat(secondId).isNotEqualTo(firstId);
    }
}

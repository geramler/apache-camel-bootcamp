package com.bootcamp.camel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bootcamp.camel.model.Order;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderRouteTest {

    @Inject
    ProducerTemplate producer;

    @Test
    void validOrderIsProcessed() {
        String r = producer.requestBody("direct:orders", new Order("1", "Ana", 100, "REGULAR"), String.class);
        assertEquals("Orden 1 procesada", r);
    }

    @Test
    void invalidOrderGoesToDeadLetter() {
        String r = producer.requestBody("direct:orders", new Order("2", "Beto", -5, "VIP"), String.class);
        assertEquals("ERROR: orden enviada a dead letter", r);
    }

    @Test
    void restEndpointWorks() {
        given().when().get("/api/hello/Ana").then().statusCode(200).body(containsString("Hola Ana"));
    }
}

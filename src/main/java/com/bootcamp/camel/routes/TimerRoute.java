package com.bootcamp.camel.routes;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

/**
 * 1) Consumer (timer) -> Processor/transform -> Producer (log).
 * Concepto: Route = from(endpoint).steps.to(endpoint).
 */
@ApplicationScoped
public class TimerRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("timer:hello?period=10000&repeatCount=3")
            .routeId("timer-route")
            .setBody(simple("Hola Camel! tick #${exchangeProperty.CamelTimerCounter}"))
            .log("${body}");
    }
}

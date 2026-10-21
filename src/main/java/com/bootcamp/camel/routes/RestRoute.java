package com.bootcamp.camel.routes;

import jakarta.enterprise.context.ApplicationScoped;
import com.bootcamp.camel.model.Order;
import org.apache.camel.builder.RouteBuilder;

/** 3) REST DSL: expone HTTP y delega en direct:orders. JSON <-> POJO automatico. */
@ApplicationScoped
public class RestRoute extends RouteBuilder {
    @Override
    public void configure() {
        rest("/api")
            .post("/orders").type(Order.class).consumes("application/json").produces("text/plain")
                .to("direct:orders")
            .get("/hello/{name}").produces("text/plain")
                .to("direct:hello");

        from("direct:hello").routeId("hello")
            .setBody(simple("Hola ${header.name} desde Camel"));
    }
}

package com.bootcamp.camel.routes;

import jakarta.enterprise.context.ApplicationScoped;
import com.bootcamp.camel.model.Order;
import org.apache.camel.builder.RouteBuilder;

/**
 * 2) EIPs: Content-Based Router (choice), Bean, Multicast, Splitter,
 *    Error handling (onException + dead letter), SEDA (async).
 * Entrada: direct:orders (llamada sincrona desde REST).
 */
@ApplicationScoped
public class OrderRoute extends RouteBuilder {
    @Override
    public void configure() {
        // Manejo de errores global de esta ruta: reintenta 2 veces y manda a DLQ
        onException(IllegalArgumentException.class)
            .maximumRedeliveries(2).redeliveryDelay(200)
            .handled(true)
            .log("Error: ${exception.message}")
            .to("direct:dead-letter");

        from("direct:orders").routeId("orders-route")
            .log("Recibida orden ${body}")
            .process(ex -> {
                Order o = ex.getMessage().getBody(Order.class);
                if (o.amount() <= 0) throw new IllegalArgumentException("Monto invalido en orden " + o.id());
            })
            // Content-Based Router
            .choice()
                .when(simple("${body.type} == 'VIP'"))
                    .to("direct:vip")
                .when(simple("${body.type} == 'BULK'"))
                    .to("direct:bulk")
                .otherwise()
                    .to("direct:regular")
            .end()
            // Multicast: misma orden a varios destinos
            .multicast()
                .to("seda:audit", "seda:notify")
            .end()
            .setBody(simple("Orden ${body.id} procesada"));

        from("direct:vip").routeId("vip")
            .setHeader("discount", constant(0.2))
            .log("VIP -> descuento ${header.discount}");

        from("direct:regular").routeId("regular")
            .setHeader("total").method("pricingService", "applyTax(${body.amount})")
            .log("Regular -> total con impuesto ${header.total}");

        from("direct:bulk").routeId("bulk")
            .log("BULK -> a procesamiento por lotes");

        // SEDA = cola en memoria, consumidores en otro hilo (asincrono)
        from("seda:audit").routeId("audit")
            .log("[AUDIT] thread=${threadName} body=${body}");
        from("seda:notify").routeId("notify")
            .log("[NOTIFY] thread=${threadName} cliente=${body.customer}");

        from("direct:dead-letter").routeId("dead-letter")
            .log("[DLQ] mensaje fallido: ${body}")
            .setBody(constant("ERROR: orden enviada a dead letter"));
    }
}

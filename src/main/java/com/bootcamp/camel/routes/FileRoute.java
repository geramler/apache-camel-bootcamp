package com.bootcamp.camel.routes;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

/**
 * 4) Integracion por archivos: deja un .csv en ./data/inbox y Camel lo lee,
 *    lo divide por linea (Splitter), transforma y escribe en ./data/outbox.
 *    Los procesados se mueven a ./data/inbox/.done
 */
@ApplicationScoped
public class FileRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("file:data/inbox?include=.*\\.csv&move=.done")
            .routeId("file-route")
            .log("Archivo ${header.CamelFileName}")
            .split(body().tokenize("\n")).streaming()
                .filter(simple("${body.trim()} != ''"))
                .transform(simple("${body.trim().toUpperCase()}"))
                .to("seda:lines")
            .end();

        from("seda:lines").routeId("lines-writer")
            .log("Linea: ${body}")
            .process(ex -> ex.getMessage().setBody(ex.getMessage().getBody(String.class) + System.lineSeparator()))
            .to("file:data/outbox?fileName=result.txt&fileExist=Append");
    }
}

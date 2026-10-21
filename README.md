# Apache Camel Bootcamp (Quarkus)

Ejecutar: `./gradlew quarkusDev` (dev mode con hot reload) — Tests: `./gradlew test`

## Conceptos clave (para la entrevista)
- **Route**: `from(consumer) -> pasos -> to(producer)`; definido con `RouteBuilder` (Java DSL).
- **CamelContext**: runtime que contiene rutas, componentes y type converters.
- **Exchange**: mensaje en vuelo (In/Out message, headers, properties, body).
- **Endpoint/Component**: URI `timer:`, `file:`, `direct:`, `seda:`, `platform-http`...
- **Processor / Bean**: logica custom. **EIPs**: choice (Content-Based Router), split, multicast, filter.
- **direct** = sincrono mismo hilo; **seda** = cola en memoria asincrona.
- **onException**: reintentos, handled, dead letter.
- **REST DSL** + binding JSON con Jackson.

## Rutas (ver `routes/`)
| Clase | Muestra |
|---|---|
| TimerRoute | consumer timer, setBody, log |
| OrderRoute | choice, bean, multicast, seda, onException/DLQ |
| RestRoute | REST DSL -> direct |
| FileRoute | file consumer, splitter, transform, append a archivo |

## Probar
```
curl localhost:8080/api/hello/Gerardo
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"1","customer":"Ana","amount":100,"type":"VIP"}'          # VIP
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"2","customer":"Beto","amount":100,"type":"REGULAR"}'     # regular + impuesto
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"3","customer":"Cy","amount":-1,"type":"VIP"}'            # error -> DLQ
mkdir -p data/inbox && printf 'uno\ndos\ntres\n' > data/inbox/a.csv    # file route -> data/outbox/result.txt
```

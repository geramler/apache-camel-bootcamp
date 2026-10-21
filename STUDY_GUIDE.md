# Guía de estudio: Apache Camel en 4 días

Esta guía explica **qué es cada cosa, por qué existe y cómo se ve en este proyecto**.
Cada día sigue el mismo ritmo: **idea general → conceptos explicados → código comentado → práctica → preguntas de entrevista con respuesta modelo**.

Stack: Quarkus 3.27 + Camel Quarkus (Java 17, Gradle).

Mapa del proyecto (`src/main/java/com/bootcamp/camel/`):

```
model/Order.java                POJO (record) que viaja por las rutas
service/PricingService.java     bean CDI con lógica de negocio
routes/TimerRoute.java          Día 1: la ruta más simple
routes/OrderRoute.java          Día 2 y 3: EIPs y manejo de errores
routes/RestRoute.java           Día 3: API HTTP
routes/FileRoute.java           Día 3: integración por archivos
```

---

## Día 1 – ¿Qué es Camel y cómo se ve una ruta?

### La idea general
En una empresa real, los sistemas hablan distinto: uno expone REST, otro deja archivos CSV, otro usa colas JMS o Kafka, otro una base de datos.
Sin una herramienta, terminas escribiendo mucho código "pegamento" (clientes HTTP, lectores de archivos, reintentos, conversiones...) repetido y difícil de mantener.

**Apache Camel es un framework de integración**: te da un vocabulario común (rutas, endpoints, mensajes), más de 300 *componentes* ya hechos para conectar sistemas, e implementa los **Enterprise Integration Patterns (EIP)**, un catálogo de soluciones clásicas (enrutar por contenido, dividir mensajes, reintentar, etc.).

> Frase para la entrevista: *"Camel no es un message broker ni un ESB; es una librería que corre dentro de tu aplicación y orquesta cómo se mueven y transforman los mensajes entre sistemas."*

### Conceptos, explicados

**1. Route (ruta).** Es un flujo: *de dónde viene un mensaje, qué le pasa, a dónde va*.
```java
from("timer:hello?period=10000")   // origen (Consumer)
    .setBody(...)                    // pasos intermedios
    .log("${body}");                 // paso final
```
Se lee de arriba hacia abajo, como una tubería (pipeline).

**2. Endpoint y URI.** Un endpoint es "un punto de conexión" y se escribe como URI: `componente:ruta?opciones`.
- `timer:hello?period=10000` → componente `timer`, nombre `hello`, dispara cada 10 s.
- `file:data/inbox` → componente `file`, directorio `data/inbox`.
- `direct:orders` → componente `direct`, nombre `orders`.

**3. Component.** Es la "fábrica" de endpoints de un tipo. En Quarkus agregas la extensión (`camel-quarkus-timer`, `camel-quarkus-file`...) en `build.gradle.kts` y el componente queda disponible. Por eso ese archivo tiene tantas dependencias `camel-quarkus-*`.

**4. Consumer vs Producer.** El `from(...)` es un **consumer**: *espera o recibe* mensajes (un timer que dispara, un archivo que aparece, una petición HTTP). Un `to(...)` es un **producer**: *envía* el mensaje a otro endpoint.

**5. Exchange y Message.** Cada vez que algo entra a una ruta, Camel crea un **Exchange** (el "sobre" que viaja por la ruta). Contiene:
- **Message** con **body** (el contenido) y **headers** (metadatos clave/valor que viajan con el mensaje, ej. `CamelFileName`).
- **Properties**: datos que viven solo mientras dura ese exchange (ej. `CamelTimerCounter`); no se envían a sistemas externos.
- **Exception**: si algo falló.

Regla práctica: *headers = información sobre el mensaje que otros pueden necesitar; properties = estado interno de Camel/la ruta.*

**6. CamelContext.** Es el runtime: guarda todas las rutas, componentes y convertidores de tipos, y controla su ciclo de vida (start/stop). Con Camel Quarkus se crea y arranca solo; no lo instancias tú.

**7. Simple language.** Mini-lenguaje de expresiones de Camel: `${body}`, `${header.name}`, `${exchangeProperty.x}`, `${body.type} == 'VIP'`. Se usa en `setBody(simple(...))`, `choice().when(simple(...))`, `log(...)`.

**8. Type converters.** Camel convierte tipos automáticamente (`String` ⇄ `byte[]`, JSON ⇄ POJO...). Por eso `getBody(String.class)` "simplemente funciona".

### Cómo se conecta con Quarkus (CDI)
Cada ruta es una clase `@ApplicationScoped` (bean CDI) que extiende `RouteBuilder` e implementa `configure()`. Quarkus la detecta en tiempo de build, Camel la registra y la arranca. Dentro de una ruta puedes invocar beans CDI por su nombre (`@Named("pricingService")`).

**Quarkus vs Spring Boot (pregunta frecuente).** Quarkus hace la inyección de dependencias (CDI) y el escaneo de rutas **en tiempo de build**, por lo que arranca en milisegundos, consume menos memoria y puede compilarse a binario nativo con GraalVM. Spring Boot lo resuelve en tiempo de ejecución con reflexión. Las rutas de Camel son idénticas en ambos; solo cambian las anotaciones (`@ApplicationScoped`/`@Inject` en vez de `@Component`/`@Autowired`) y las dependencias (`camel-quarkus-*` en vez de `camel-*-starter`).

### Código a leer (en este orden)
1. No hay clase `main`: Quarkus arranca solo y Camel Quarkus crea el `CamelContext`. Todo se configura en `application.properties`.
2. `TimerRoute.java`:
   ```java
   from("timer:hello?period=10000&repeatCount=3")   // cada 10 s, 3 veces
       .routeId("timer-route")                       // nombre legible para logs/monitoreo
       .setBody(simple("Hola Camel! tick #${exchangeProperty.CamelTimerCounter}"))
       .log("${body}");
   ```
   Qué pasa: el timer crea un Exchange vacío → `setBody` le pone un texto (leyendo una property que el timer puso) → `log` lo imprime.
3. `build.gradle.kts` y `application.properties`.

### Práctica
1. `./gradlew quarkusDev`. Busca en el log el mensaje de rutas iniciadas y los 3 `Hola Camel!`.
2. Cambia `period` a `2000` y `repeatCount` a `5`; reinicia y verifica.
3. Agrega `.setHeader("origen", constant("timer"))` antes del log y muestra `${header.origen}`.
4. Pregúntate: ¿de dónde sale `CamelTimerCounter`? (lo pone el componente timer como property del exchange).

### Preguntas de entrevista
- **¿Qué es Camel y cuándo NO lo usarías?** Es un framework de integración basado en EIPs. No lo usaría para una app con una sola integración simple: añade abstracción innecesaria.
- **¿Diferencia entre Component, Endpoint y Route?** Component = fábrica (`timer`); Endpoint = instancia configurada (`timer:hello?period=10000`); Route = flujo que une endpoints con pasos intermedios.
- **¿Qué hay dentro de un Exchange?** Message (body + headers), properties y, opcionalmente, una excepción. Headers viajan con el mensaje; properties solo viven durante el exchange.

---

## Día 2 – Enrutamiento y patrones (EIPs) con `OrderRoute`

### La idea general
Los **EIPs** son recetas probadas para problemas comunes de integración. Camel los expone como métodos del DSL. Hoy entiendes **Content-Based Router**, **Multicast** y **Bean/Processor**, más la diferencia crítica entre `direct` y `seda`.

### Conceptos, explicados

**1. Content-Based Router (`choice`).** "Decide a dónde mandar el mensaje según lo que contiene". Es el `if / else if / else` de Camel:
```java
.choice()
    .when(simple("${body.type} == 'VIP'"))    .to("direct:vip")
    .when(simple("${body.type} == 'BULK'"))   .to("direct:bulk")
    .otherwise()                              .to("direct:regular")
.end()
```
Aquí el body ya es un `Order` (lo convirtió Jackson en la capa REST), por eso `${body.type}` accede a su campo.

**2. Processor y Bean.** Cuando el DSL no alcanza, ejecutas tu código:
- `.process(exchange -> {...})` – lambda inline. En el proyecto valida que `amount > 0` y lanza `IllegalArgumentException` si no.
- `.setHeader("total").method("pricingService", "applyTax(${body.amount})")` – llama un bean CDI. `PricingService.applyTax` multiplica por 1.16 (IVA); con 100 da `116.0`.

Buena práctica: **la lógica de negocio va en beans testeables; la ruta solo orquesta.**

**3. Multicast.** Envía una *copia* del mismo mensaje a varios destinos:
```java
.multicast().to("seda:audit", "seda:notify").end()
```
Útil cuando varias áreas reaccionan al mismo evento (auditoría, notificación, analítica). No confundir con:
- **Pipeline** (comportamiento por defecto de `.to().to()`): el resultado de un paso es la entrada del siguiente.
- **Recipient List**: como multicast, pero los destinos se calculan dinámicamente en tiempo de ejecución.

**4. `direct` vs `seda` (muy preguntado).**

| | `direct:` | `seda:` |
|---|---|---|
| Ejecución | síncrona, **mismo hilo** | asíncrona, **otro hilo** |
| Cola | no hay | `BlockingQueue` en memoria |
| ¿El que envía espera? | sí, hasta terminar | no, continúa de inmediato |
| Uso típico | dividir una ruta grande en sub-rutas reutilizables | desacoplar y paralelizar trabajo |
| Riesgo | bloquea al llamante | lo encolado se pierde si la app cae (memoria, no disco) |

Para persistencia/distribución real se usa un broker: Kafka, ActiveMQ, RabbitMQ.

**5. `routeId` y sub-rutas.** Cada `from(...)` define una ruta propia. `direct:vip`, `direct:regular`, etc. son rutas independientes que se "llaman" por nombre, como funciones. Facilita leer, testear y reutilizar.

### El flujo completo de `direct:orders`
```
direct:orders
   │ log "Recibida orden ..."
   │ process: valida amount > 0 (si no → IllegalArgumentException → Día 3)
   │ choice ── VIP     → direct:vip      (header discount=0.2)
   │        ── BULK    → direct:bulk     (solo log)
   │        └─ regular → direct:regular  (calcula total con impuesto en header "total")
   │ multicast ──→ seda:audit   (otro hilo: log [AUDIT])
   │           └─→ seda:notify  (otro hilo: log [NOTIFY])
   └ setBody("Orden <id> procesada")  ← respuesta final
```
Observa que `choice` *no* cambia el body: los sub-flujos solo agregan headers o hacen log, y el body final lo fija `setBody` al terminar.

### Práctica (con `quarkusDev` corriendo)
```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"1","customer":"Ana","amount":100,"type":"VIP"}'
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"2","customer":"Beto","amount":100,"type":"REGULAR"}'
```
1. En los logs verifica: `VIP -> descuento 0.2` para la 1 y `Regular -> total con impuesto 116.0` para la 2.
2. Compara el nombre del hilo de `[AUDIT]` y `[NOTIFY]` con el del log "Recibida orden": son distintos (SEDA).
3. Cambia `seda:audit` por `direct:audit` (y el `from` correspondiente): ahora corre en el mismo hilo y bloquea.
4. Agrega una rama `'EXPORT'` al `choice`.

### Preguntas de entrevista
- **¿Cómo enrutarías según el contenido?** `choice().when(predicado).to(...)` — es el Content-Based Router.
- **¿Diferencia entre direct y seda?** Síncrono en el mismo hilo vs. cola en memoria con hilos separados. Para durabilidad usaría un broker.
- **¿Qué pasa si una cola SEDA se llena?** Según su configuración (`size`, `blockWhenFull`) bloquea al productor o lanza excepción; es una forma de *backpressure*.
- **¿Dónde pones la lógica de negocio?** En beans CDI testeables, no incrustada en la ruta.

---

## Día 3 – REST, errores y procesamiento de datos

### La idea general
Hoy ves cómo entra un mensaje desde el exterior (HTTP y archivos), qué pasa cuando algo falla, y cómo procesar colecciones grandes.

### Conceptos, explicados

**1. REST DSL.** Define endpoints HTTP de forma declarativa y *delega* a una ruta:
```java
rest("/api")
    .post("/orders").type(Order.class).consumes("application/json").produces("text/plain")
        .to("direct:orders")
    .get("/hello/{name}").produces("text/plain").to("direct:hello");
```
- `type(Order.class)` + `camel.rest.binding-mode=json`: Camel usa Jackson para convertir el JSON del request a un `Order` antes de entrar a la ruta.
- `{name}` pasa a ser el header `name` (`${header.name}`).
- Camel usa el servidor HTTP de Quarkus (Vert.x) mediante `camel-quarkus-platform-http`; no necesita Jetty aparte.
- **Por qué delegar a `direct:`**: separa la "puerta de entrada" (HTTP) de la lógica, que así se prueba sin HTTP (ver Día 4).
- `camel.rest.inline-routes=false` (en `application.properties`): mantiene la parte REST y `direct:orders` como rutas separadas. Sin esto, Camel las fusiona y `direct:orders` no existe como consumidor para los tests.

**2. Manejo de errores con `onException`.**
```java
onException(IllegalArgumentException.class)
    .maximumRedeliveries(2).redeliveryDelay(200)   // reintenta 2 veces, 200 ms entre intentos
    .handled(true)                                  // "yo me encargo": el error NO llega al llamante
    .log("Error: ${exception.message}")
    .to("direct:dead-letter");                      // destino final del mensaje fallido
```
Qué ocurre con la orden de monto negativo:
1. El `process` lanza `IllegalArgumentException`.
2. Camel reintenta (2 veces, 200 ms de espera).
3. Al agotarse, ejecuta el bloque `onException`: loguea y envía a `direct:dead-letter`.
4. Como `handled(true)`, el resultado de esa ruta (`"ERROR: orden enviada a dead letter"`) se convierte en la respuesta HTTP, en vez de un error 500.

Conceptos relacionados:
- **Dead Letter Channel**: destino de los mensajes que no se pudieron procesar, para revisarlos o reprocesarlos (en producción: una cola/tabla, no solo un log).
- **`handled(true)` vs `continued(true)`**: handled corta el flujo normal y responde con la ruta de error; continued ignora el error y *sigue* con el siguiente paso.
- **Alternativas**: `doTry/doCatch/doFinally` para manejo local (como try/catch) y `errorHandler(deadLetterChannel(...))` como política global.

**3. Splitter y Filter (`FileRoute`).**
```java
from("file:data/inbox?include=.*\\.csv&move=.done")
    .split(body().tokenize("\n")).streaming()     // un mensaje por línea, sin cargar todo en memoria
        .filter(simple("${body.trim()} != ''"))     // descarta líneas vacías (y evita el \r de CSV de Windows)
        .transform(simple("${body.trim().toUpperCase()}"))
        .to("seda:lines")
    .end();
```
- **Splitter EIP**: divide un mensaje grande en muchos pequeños, procesados individualmente.
- **`.streaming()`**: lectura perezosa; clave para archivos enormes (uno de 5 GB no se carga completo).
- **Filter EIP**: deja pasar solo los mensajes que cumplen la condición.
- **File component**: *sondea* el directorio. `move=.done` mueve el archivo ya procesado a `.done` para no reprocesarlo. En producción conviene además `idempotentConsumer` para no duplicar.
- La segunda ruta, `seda:lines`, recibe cada línea y la **agrega** (`fileExist=Append`) a `data/outbox/result.txt`. La cola SEDA desacopla lectura y escritura.

### Práctica
```bash
curl localhost:8080/api/hello/Gerardo                                   # REST simple
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"id":"3","customer":"Cy","amount":-1,"type":"VIP"}'              # error → DLQ
mkdir -p data/inbox && printf 'uno\ndos\ntres\n' > data/inbox/a.csv
sleep 3; cat data/outbox/result.txt; ls -a data/inbox                    # UNO DOS TRES y carpeta .done
```
1. En el log busca los reintentos y luego `[DLQ] mensaje fallido`.
2. Cambia `handled(true)` a `false` y repite el curl: ahora recibes un error HTTP.
3. Agrega `.parallelProcessing()` al `split` y observa que las líneas se procesan en hilos distintos.
4. Pon una línea vacía en el CSV y comprueba que el filter la descarta.

### Preguntas de entrevista
- **¿Cómo evitas perder mensajes?** Dead letter channel, reintentos, colas persistentes (broker), transacciones y consumidores idempotentes.
- **¿Qué hace `handled(true)`?** Marca la excepción como manejada: no se propaga al llamante y el resultado de la ruta de error es la respuesta.
- **¿Cómo procesarías un archivo muy grande?** `split` con `streaming()` (y opcionalmente `parallelProcessing`), sin cargar todo en memoria.
- **¿Por qué el REST DSL delega a `direct:`?** Para separar transporte de lógica y probar la lógica sin HTTP.

---

## Día 4 – Testing, operación y simulacro de entrevista

### La idea general
Un tech lead decide **cómo se prueban, cómo se observan y cuándo conviene usar Camel**. Hoy cierras esos puntos y practicas explicar el sistema completo.

### Conceptos, explicados

**1. Testing de rutas.** Mira `OrderRouteTest`:
```java
@QuarkusTest
class OrderRouteTest {
    @Inject ProducerTemplate producer;

    @Test void validOrderIsProcessed() {
        String r = producer.requestBody("direct:orders", new Order("1","Ana",100,"REGULAR"), String.class);
        assertEquals("Orden 1 procesada", r);
    }
}
```
- **`ProducerTemplate`** es el "cliente" de Camel: envía un mensaje a cualquier endpoint y devuelve el resultado.
- Se invoca `direct:orders` directamente: se prueba la lógica sin HTTP ni red, rápido. Esa es la ventaja de la separación del Día 3.
- Para pruebas avanzadas: `camel-mock` (verificar mensajes recibidos), `AdviceWith` (reemplazar endpoints reales por mocks) y `NotifyBuilder` (esperar a que termine el procesamiento asíncrono).

**2. Observabilidad.**
- `routeId` en cada ruta: aparece en logs, JMX y métricas.
- `.log()` con expresiones Simple para trazas; `${threadName}` para depurar concurrencia.
- Métricas con Micrometer (`quarkus-micrometer` + `camel-quarkus-micrometer`), health checks con SmallRye Health y trazabilidad con OpenTelemetry.

**3. Rendimiento y escalado.**
- `seda?concurrentConsumers=N`: más hilos consumiendo la cola.
- `split().parallelProcessing()`: paralelismo dentro de una ruta.
- Para escalar entre instancias o sobrevivir caídas: reemplazar SEDA por Kafka/JMS/RabbitMQ.
- Cuidar backpressure: tamaño de cola, `blockWhenFull`, timeouts.

**4. ¿Cuándo usar Camel y cuándo no?**
- **Sí**: muchas integraciones heterogéneas (HTTP, archivos, colas, BD, SaaS), patrones EIP, conectores listos.
- **No**: una sola integración trivial (un cliente HTTP basta); streaming de alto volumen con estado (Kafka Streams/Flink); o cuando la complejidad no se justifica.
- **Comparativas**: Spring Integration (similar, ecosistema Spring); Mule (ESB comercial, más pesado); Kafka Connect (conectores específicos de Kafka).

### Práctica
1. `./gradlew test` y verifica que pasan los 3 tests (2 de rutas con `ProducerTemplate` y 1 del endpoint REST con RestAssured).
2. Agrega tests para la rama `BULK` y para VIP (basta comprobar `"Orden X procesada"`) y otro para dead letter con monto 0.
3. **Reto A:** agrega un segundo consumidor de `seda:audit`. ¿Qué ocurre? (sin `multipleConsumers=true` los consumidores *compiten*; con él, hay pub/sub). Investígalo.
4. **Reto B:** escribe las órdenes auditadas en `data/audit.log` con el file component.
5. **Reto C:** explica qué cambiarías para que `seda:notify` sobreviva a un reinicio (cola persistente en un broker).

### Simulacro de entrevista (responde en voz alta, 2 min cada una)

**1. Explica el recorrido de `POST /api/orders` hasta la respuesta.**
> El request llega por el REST DSL (`platform-http`), Jackson convierte el JSON a `Order` y se delega a `direct:orders`. Ahí se valida el monto; un `choice` enruta por tipo (VIP agrega descuento, regular calcula impuesto con un bean CDI, bulk loguea). Luego un `multicast` envía copias a `seda:audit` y `seda:notify`, que corren en otros hilos. Finalmente `setBody` produce "Orden X procesada", que es la respuesta HTTP. Si la validación falla, `onException` reintenta 2 veces y manda el mensaje a `direct:dead-letter`; su resultado se devuelve al cliente porque `handled(true)`.

**2. ¿Cómo escalarías a 10k órdenes/seg?**
> Pasaría el trabajo asíncrono a un broker (Kafka) con particiones, aumentaría consumidores concurrentes, haría los beans sin estado e idempotentes para escalar horizontalmente, y mediría con Micrometer para encontrar el cuello de botella.

**3. ¿Cómo garantizas que no se pierdan ni dupliquen mensajes?**
> Colas persistentes, ack manual/transacciones, dead letter channel, reintentos con backoff e `idempotentConsumer` con un repositorio (BD/Redis) para deduplicar.

**4. ¿Cómo despliegas y monitoreas?**
> App Quarkus (JVM o nativa) en contenedor; Micrometer para métricas por `routeId`, SmallRye Health para health checks; logs estructurados con correlación; alertas sobre tamaño de la DLQ y tasa de fallos.

**5. ¿Camel vs código plano o Kafka Streams?**
> Camel cuando hay muchas fuentes/destinos distintos y patrones de integración; código plano si es una integración simple; Kafka Streams/Flink para procesamiento de flujos con estado y alto volumen.

**6. Dibuja el sistema en una pizarra.**
```
 HTTP ─► RestRoute ─► direct:orders ─► choice ─► vip / bulk / regular
                          │                      └► multicast ─► seda:audit
                          │                                   └► seda:notify
                          └─(error)─► onException ─► direct:dead-letter

 file:inbox ─► FileRoute ─► split/filter/transform ─► seda:lines ─► file:outbox/result.txt

 timer ─► TimerRoute ─► log
```

### Cheat sheet final
| Concepto | Una línea |
|---|---|
| CamelContext | Runtime que aloja rutas, componentes y converters |
| Route | `from(consumer) → pasos → to(producer)` |
| Exchange | Mensaje en vuelo: body, headers, properties, exception |
| Endpoint URI | `componente:ruta?opciones` |
| direct / seda | Síncrono mismo hilo / cola asíncrona en memoria |
| choice | Content-Based Router (if/else) |
| multicast | Copia del mensaje a varios destinos |
| split / filter | Dividir una colección / descartar mensajes |
| bean / process | Ejecutar tu código dentro de la ruta |
| onException | Reintentos + handled + dead letter |
| REST DSL | API HTTP declarativa que delega en `direct:` |
| ProducerTemplate | Cliente para enviar mensajes (ideal en tests) |

### Repaso la víspera de la entrevista
1. Dibuja de memoria el diagrama del simulacro #6.
2. Explica en voz alta direct vs seda y `handled(true)`.
3. Practica la respuesta del simulacro #1 sin mirar el código.
4. Repasa el cheat sheet.

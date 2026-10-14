# EntregaGo — solución del examen parcial

**Curso:** Diseño de Experimentos de Ingeniería de Software (1ASI0732)  
**Caso:** marketplace de pedidos y delivery  
**Stack de la solución:** Java 25, Spring Boot 3.5, JUnit 5, Mockito, Cucumber y Rest Assured.

## Pregunta 1 — UML Class Diagram

El modelo prioriza la confirmación como regla central e incluye clientes, comercios, repartidores y administrador; catálogo, carrito, pedido, pago, reserva de stock, cobertura, cupones, entrega, evidencia, incidencias, sustituciones, calificaciones, políticas y reportes. El diagrama explicita atributos privados, operaciones públicas, interfaces, enumeraciones, multiplicidades y dependencias.

Archivo PlantUML: [`diagrams/entregago-class-diagram.puml`](diagrams/entregago-class-diagram.puml).

La clase elegida para las preguntas 2–4 es `OrderConfirmationService`. Su operación `confirm` sigue esta secuencia:

1. Comprueba que exista el comercio y esté abierto.
2. Comprueba que la dirección pertenezca al cliente y esté cubierta.
3. Comprueba disponibilidad y que todos los productos pertenezcan al comercio.
4. Calcula el precio con descuento, envío e impuesto.
5. Reserva todas las cantidades de forma atómica.
6. Autoriza el pago. Si se rechaza, libera la reserva y no guarda un pedido confirmado.
7. Persiste el pedido confirmado. Si falla la persistencia tras autorizar, revierte la autorización y libera el stock.

La reserva se hace antes de llamar al medio de pago para evitar vender la misma unidad a dos clientes. Una reserva solo queda activa con una confirmación exitosa; ante rechazo de pago se libera antes de responder.

## Pregunta 2 — Unit test & Integration test

La clase bajo prueba es `OrderConfirmationService`: coordina la regla de negocio principal, en lugar de probar una función genérica o la validación de parámetros.

### Pruebas unitarias

Están en [`src/test/java/pe/entregago/application/OrderConfirmationServiceTest.java`](src/test/java/pe/entregago/application/OrderConfirmationServiceTest.java). Usan JUnit 5, Mockito y AssertJ; cada caso aísla las dependencias externas y comprueba una decisión de negocio.

1. `shouldRejectClosedMerchantBeforeReservingStockOrAuthorizingPayment`: un comercio cerrado impide la confirmación y no activa reserva ni pago.
2. `shouldRejectWhenAtomicReservationFindsInsufficientStockWithoutCharging`: la reserva atómica detecta una carrera por stock; el pago no se autoriza ni se guarda un pedido confirmado.
3. `shouldReleaseReservationWhenPaymentAuthorizationIsRejected`: el pago rechazado libera toda la reserva y no confirma el pedido.

### Prueba integral

Está en [`src/test/java/pe/entregago/api/OrderConfirmationIntegrationTest.java`](src/test/java/pe/entregago/api/OrderConfirmationIntegrationTest.java). Con Spring Boot y MockMvc recorre REST → servicio de aplicación → adaptador de inventario y pago de prueba. Comprueba HTTP 201, estado `CONFIRMED`, total calculado y cantidades reservadas.

## Pregunta 3 — Feature, Scenarios, Data Tables & Examples

El feature elegido es la confirmación de pedidos, enlazado con la clase `OrderConfirmationService`. Incluye un escenario exitoso con Data Table de productos, un `Scenario Outline` con rechazo por comercio cerrado, falta de stock, falta de cobertura o pago rechazado, y un segundo `Scenario Outline` con cupones expirados, por debajo del mínimo, de otro comercio o con el límite de usos agotado.

Archivo Cucumber: [`src/test/resources/features/confirmar-pedido.feature`](src/test/resources/features/confirmar-pedido.feature).

Los montos del feature usan una política de prueba explícita: envío de S/ 4.00 e impuesto de 10% sobre los productos después del descuento. No se interpreta ese porcentaje como una tasa tributaria legal.

## Pregunta 4 — Step Definition Implementation

Las definiciones implementadas para probar la REST API están en [`src/test/java/pe/entregago/steps/OrderConfirmationSteps.java`](src/test/java/pe/entregago/steps/OrderConfirmationSteps.java). Preparan el estado con el adaptador de pruebas, envían el pedido por HTTP con Rest Assured y verifican estado, códigos de rechazo, importes y stock reservado. El runner y la configuración Spring/Cucumber están en el mismo paquete.

La API de muestra usa `POST /api/orders/confirm`: responde `201` cuando el pedido se confirma; `409` por comercio cerrado o falta de stock; `422` por dirección fuera de cobertura; y `402` cuando el medio de pago rechaza la autorización.

## Evaluación estimada según la rúbrica

La rúbrica oficial asigna **5 puntos por pregunta**, con un total de **20 puntos**. Para mostrar cada respuesta sobre 20, se multiplica su puntaje de rúbrica por cuatro. Es una estimación del material entregado, no una nota oficial del docente.

| Pregunta | Rúbrica (/5) | Equivalente solicitado (/20) | Sustento resumido |
|---|---:|---:|---|
| 1. UML | 4.50 | **18/20** | Buen alcance del dominio, visibilidades, interfaces, enumeraciones y relaciones; el detalle de persistencia e invariantes puede ampliarse. |
| 2. Pruebas | 4.75 | **19/20** | Tres pruebas unitarias de reglas críticas y una integración de extremo a extremo; la integración cubre el camino exitoso. |
| 3. Gherkin | 4.75 | **19/20** | Feature ligado al servicio, pasos Given–When–Then, Data Table y Examples con casos de aceptación y rechazo. |
| 4. Step Definitions | 4.50 | **18/20** | Implementación Java con Cucumber, Spring y llamadas REST reales; el fixture está en memoria para mantener el ejemplo ejecutable y acotado. |
| **Total** | **18.50/20** | — | Promedio de los equivalentes por pregunta: **18.5/20**. |

## Entorno Nix y ejecución

Desde `codex/`, entrar al shell de Nix e iniciar la API o ejecutar las pruebas:

```bash
nix-shell
mvn spring-boot:run
```

En otra sesión dentro del shell:

```bash
mvn test
```

Para generar la imagen del diagrama:

```bash
plantuml diagrams/entregago-class-diagram.puml
```

El adaptador incluido usa memoria y sirve como implementación didáctica del flujo. En una entrega de producción, las reservas, pagos y persistencia requerirían transacciones y compensaciones durables, además de repositorios con almacenamiento persistente.

El `pom.xml` fija Spring Boot 3.5.16 y Java 25. La documentación oficial de Spring indica que esa línea de Boot es compatible hasta Java 25 y requiere Maven 3.6.3 o posterior ([requisitos](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [sistemas de build](https://docs.spring.io/spring-boot/reference/using/build-systems.html)).

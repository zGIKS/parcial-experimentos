# EntregaGo — examen parcial resuelto

**Curso:** Diseño de Experimentos de Ingeniería de Software (1ASI0732)  
**Caso:** marketplace de pedidos y delivery  
**Stack:** Java 25, Spring Boot 3.5, JUnit 5, Mockito, Cucumber y Rest Assured.

La estructura y el diccionario siguen el capítulo 4 de `projects/IOT/report/chapters/chapter4.md`: clases clasificadas por capa, diagramas separados de cada capa y vistas de dominio unificadas. El modelo usa nombres de dominio consistentes con el código.

## 4.1 Bounded Context y lenguaje ubicuo

Esta solución desarrolla el bounded context **Order Fulfillment** de EntregaGo. Su responsabilidad principal es convertir la solicitud del cliente en un pedido confirmado, reservado y trazable hasta su entrega, rechazo o cancelación. Catálogo, preparación, pago y delivery se modelan con el vocabulario compartido que sigue.

| Término ubicuo | Definición del negocio |
|---|---|
| **Customer (cliente)** | Persona que mantiene direcciones, arma un carrito, confirma pedidos y puede calificarlos después de la entrega. |
| **Merchant (comercio)** | Comercio afiliado con horario, cobertura, catálogo y tiempo estimado de preparación. |
| **Product (producto)** | Artículo de un comercio con precio, disponibilidad y cantidades en mano y reservadas. |
| **Cart (carrito)** | Selección temporal de productos de un solo comercio antes de confirmar el pedido. |
| **Order (pedido)** | Compromiso trazable entre cliente y comercio, con productos, precio, pago, reserva y estado. |
| **Order confirmation (confirmación del pedido)** | Decisión aceptada solo si el comercio está abierto, la dirección está cubierta, los productos están disponibles y el pago se autoriza. |
| **Coverage zone (zona de cobertura)** | Área en la que un comercio ofrece entrega; el cliente debe seleccionar una dirección dentro de ella. |
| **Stock reservation (reserva de stock)** | Retención atómica de las cantidades del pedido para evitar vender unidades ya comprometidas. Se libera ante una falla de confirmación o se consume al completar la venta. |
| **Price breakdown (desglose del precio)** | Subtotal, descuento, envío, impuesto y total calculados antes de autorizar el pago. |
| **Coupon (cupón)** | Beneficio condicionado por vigencia, subtotal mínimo, comercio aplicable, usos disponibles y tope de descuento. |
| **Payment authorization (autorización del pago)** | Resultado aprobado o rechazado devuelto por el medio de pago para el total cotizado. |
| **Order state (estado del pedido)** | Secuencia `CONFIRMED → MERCHANT_ACCEPTED → IN_PREPARATION → READY_FOR_PICKUP → DELIVERED`; cada operación valida el estado previo. |
| **Courier (repartidor)** | Persona que puede tomar entregas si está disponible, opera en la zona, tiene capacidad y no excede entregas simultáneas. |
| **Delivery (entrega)** | Recorrido asignado, recogido, en camino y entregado, con ubicación y marca de tiempo en cada transición. |
| **Delivery evidence (evidencia de entrega)** | Código, fotografía o firma que debe existir para completar la entrega. |
| **Incident (incidencia)** | Situación reportada durante el reparto que lleva la entrega a revisión y evita marcarla como completada. |
| **Substitution proposal (propuesta de sustitución)** | Producto alternativo que no se acepta hasta que el cliente lo apruebe expresamente. |
| **Cancellation policy (política de cancelación)** | Regla que define devolución total, parcial o ninguna según el estado alcanzado por el pedido. |
| **Commission policy (política de comisión)** | Porcentaje vigente aplicado a las ventas del comercio. |
| **Platform admin (administrador)** | Actor que configura cobertura, cupones, comisiones, bloqueos e investigación de incidencias. |

## 4.2 Diccionario de clases por capa

| Clase | Capa | Responsabilidad y atributos principales | Métodos clave | Relaciones / dependencias |
|---|---|---|---|---|
| `Order` | Domain | Raíz de agregado; IDs de cliente, comercio y dirección; líneas, precio, pago, reserva, estado y tiempos. | `confirm`, `acceptByMerchant`, `startPreparation`, `markReadyForPickup`, `cancel`, `sendToReview` | Contiene `OrderLine` y `PriceBreakdown`; referencia cliente, comercio, dirección, autorización y reserva. |
| `Merchant` | Domain | Horarios, zonas, actividad y preparación estimada. | `isOpenAt`, `serves` | Tiene horarios; ofrece productos y cobertura. |
| `Product` | Domain | Comercio, nombre, categoría, precio, stock disponible/reservado y disponibilidad. | `availableQuantity`, `reserve`, `release` | Pertenece a un comercio; se representa en carrito y líneas de pedido. |
| `Coupon` | Domain | Vigencia, mínimo, comercio, límite de usos, tipo, valor y tope. | `evaluate`, `recordUse` | Lo evalúa `OrderPricingPolicy`; puede ser aplicado por un pedido. |
| `StockReservation` | Domain | ID, cantidades retenidas, instante y estado. | `release`, `commit` | La crea `StockReservationRepository`; el pedido guarda su ID. |
| `Delivery`, `Courier`, `Incident`, `DeliveryEvidence` | Domain | Estados de entrega, capacidad/zona, causa de revisión y evidencia capturada. | `markPickedUp`, `markInTransit`, `complete`, `reportIncident`, `isEligible` | Entrega vincula pedido y repartidor; mantiene historial e incidencias. |
| `OrderPricingPolicy` | Domain | Servicio de dominio para calcular subtotal, descuento, envío, impuesto y total. | `quote` | Usa líneas y, si aplica, `Coupon`; es independiente de Spring. |
| `*Repository`, `PaymentGateway` | Domain | Puertos de lectura/escritura de agregados, reservas y autorización externa. | `findById`, `save`, `reserveAll`, `release`, `authorize` | Contratos implementados en Infrastructure. |
| `OrderController` | Interface | Entrada HTTP `POST /api/orders/confirm`; convierte JSON y retorna resultado. | `confirm` | Traduce `ConfirmOrderRequest` a comando de aplicación. |
| `ConfirmOrderRequest`, `OrderConfirmationResource` | Interface | Recursos REST de entrada y salida. | `toCommand`, `from` | No contienen reglas de negocio ni persistencia. |
| `BusinessRuleExceptionHandler` | Interface | Convierte errores de reglas a HTTP 404, 409, 402 o 422. | `handle` | Adapta excepciones de dominio al contrato REST. |
| `ConfirmOrderCommandServiceImpl` | Application | Orquesta disponibilidad, horario, cobertura, cotización, reserva, autorización y guardado. | `handle` | Depende de puertos de Domain; crea y confirma `Order`. |
| `ConfirmOrderCommand`, `ConfirmOrderResult` | Application / Domain contract | Datos tipados de entrada y resultado del caso de uso. | — | Transportan VOs, líneas, precio e IDs. |
| `InMemoryMarketplaceAdapter` | Infrastructure | Adaptador de persistencia en memoria para comercios, direcciones, productos, cupones, reservas, pedidos y entregas. | `findById`, `reserveAll`, `release`, `commit`, `save` | Implementa los puertos de repositorio declarados en Domain. |
| `InMemoryPaymentGateway` | Infrastructure | Adaptador de pago configurable para aprobación/rechazo y reversa demostrativa. | `authorize`, `voidAuthorization` | Implementa `PaymentGateway`. |
| `MarketplaceConfiguration` | Infrastructure | Configura `Clock` y política de precio demostrativa. | `clock`, `orderPricingPolicy` | Crea `StandardOrderPricingPolicy`. |

## 4.3 Diagramas UML (PlantUML)

Los diagramas se separan para que puedan leerse sin comprimir todas las clases en una sola página. `entregago-class-diagram.puml` muestra el mapa de las cuatro capas; `domain-layer.puml` y `domain-fulfillment.puml` son las dos vistas complementarias de Domain que, juntas, cubren el modelo del caso.

1. **Mapa de las cuatro capas:** [diagrams/entregago-class-diagram.puml](diagrams/entregago-class-diagram.puml)
2. **Domain — clientes, catálogo, pedido, precio y pago:** [diagrams/domain-layer.puml](diagrams/domain-layer.puml)
3. **Domain — delivery, repartidores, incidencias y gobernanza:** [diagrams/domain-fulfillment.puml](diagrams/domain-fulfillment.puml)
4. **Interface — REST:** [diagrams/interfaces-layer.puml](diagrams/interfaces-layer.puml)
5. **Application — caso de uso:** [diagrams/application-layer.puml](diagrams/application-layer.puml)
6. **Infrastructure — adaptadores en memoria:** [diagrams/infrastructure-layer.puml](diagrams/infrastructure-layer.puml)

La dependencia de ejecución sigue `Interface → Application → Domain`. Infrastructure implementa puertos que define Domain. Domain no importa Spring, HTTP, JPA ni detalles de almacenamiento.

## Pregunta 1 — UML Class Diagram (5 puntos de la rúbrica)

El diagrama de dominio prioriza `Order` y sus invariantes. Se complementa con clientes, comercios, catálogo, stock, cupones, cobertura, preparación, pago, delivery, evidencias, incidencias, sustituciones, calificaciones, políticas y administración. Los atributos se muestran como privados (`-`), las operaciones como públicas (`+`), y se indican interfaces, enumeraciones, multiplicidades y asociaciones.

El estado del pedido se controla en `Order`; el estado de entrega se controla en `Delivery`. La reserva de inventario se representa explícitamente y el adaptador que la modifica implementa `StockReservationRepository`, no vive dentro de la entidad `Order`.

## Pregunta 2 — Unit test & Integration test (5 puntos de la rúbrica)

La clase elegida es `ConfirmOrderCommandServiceImpl`, que coordina la regla de confirmación, no una función genérica.

### Pruebas unitarias

Implementadas con JUnit 5, Mockito y objetos de dominio reales en [ConfirmOrderCommandServiceImplTest.java](src/test/java/pe/entregago/application/internal/commandservices/ConfirmOrderCommandServiceImplTest.java):

1. `closedMerchantPreventsReservationAndPaymentAuthorization`: el comercio cerrado detiene el caso de uso antes de revisar/reservar inventario o autorizar el pago.
2. `atomicStockReservationRejectsInsufficientInventoryBeforePayment`: una reserva atómica que encuentra stock insuficiente impide cobrar y guardar un pedido confirmado.
3. `rejectedPaymentReleasesReservationAndDoesNotPersistConfirmedOrder`: el rechazo del medio de pago libera toda la reserva y no persiste un pedido confirmado.

Estas pruebas comprueban decisiones del negocio y sus efectos observables entre dependencias.

### Prueba integral

[OrderConfirmationIntegrationTest.java](src/test/java/pe/entregago/interfaces/rest/controllers/OrderConfirmationIntegrationTest.java) recorre MockMvc → controlador REST → caso de uso → adaptadores de memoria y pago. Verifica respuesta HTTP 201, estado `CONFIRMED`, cálculo del total de varios productos, persistencia del pedido y reserva de las cantidades exactas.

## Pregunta 3 — Feature, Scenarios, Data Tables & Examples (5 puntos de la rúbrica)

El feature de [confirm-order.feature](src/test/resources/features/confirm-order.feature) cubre una confirmación con varios productos mediante Data Tables, un `Scenario Outline` con ejemplos de comercio cerrado, falta de stock, dirección fuera de cobertura y pago rechazado, y otro `Scenario Outline` para reglas de cupones (vigencia, mínimo, comercio y límite de usos). El feature prueba la misma capacidad de negocio que `ConfirmOrderCommandServiceImpl`.

La política demostrativa usa costo de envío **S/ 4.00** e impuesto de prueba de **10 %** sobre subtotal menos descuento. El porcentaje solo es una regla del ejercicio, no una tasa tributaria normativa.

## Pregunta 4 — Step Definition Implementation (5 puntos de la rúbrica)

[ConfirmOrderSteps.java](src/test/java/pe/entregago/interfaces/rest/steps/ConfirmOrderSteps.java) implementa los pasos Cucumber como pruebas de sistema de la API REST real con Rest Assured. Las definiciones preparan comercios, direcciones, productos, cupones y pago en los adaptadores de memoria; envían `POST /api/orders/confirm` y verifican HTTP, estado, precio, cupón y stock retenido. La configuración y el runner están en el mismo paquete.

El contrato de éxito devuelve HTTP **201**. Las reglas incumplidas se traducen a **409** (comercio cerrado, stock), **422** (fuera de cobertura) o **402** (pago no autorizado).

## Calificación estimada

La rúbrica entrega 5 puntos por pregunta (20 en total). La calificación solicitada por pregunta sobre 20 se expresa como equivalente `puntaje de rúbrica × 4`; el total del examen se calcula como promedio de los cuatro equivalentes.

| Pregunta | Estimación /20 | Equivalente de rúbrica /5 | Criterio |
|---|---:|---:|---|
| 1. UML | **18/20** | 4.5/5 | Clases de dominio completas y diagramas separados por capa, visibilidad, interfaces, enums y relaciones; quedan algunas capacidades representadas a nivel de modelo sin caso de uso REST propio. |
| 2. Unit + integración | **18/20** | 4.5/5 | Tres pruebas unitarias de reglas críticas y una integración de camino exitoso que verifica reserva/precio; no se ejecutaron en esta entrega. |
| 3. Gherkin | **19/20** | 4.75/5 | Feature vinculado al caso de uso, Data Tables, Examples y escenarios de aceptación/rechazo con datos concretos. |
| 4. Step Definitions | **18/20** | 4.5/5 | Pasos implementados sobre REST real, validando efectos del negocio; no se ejecutaron en esta entrega. |
| **Nota estimada del examen** | **18.25/20** | **18.25/20** | Promedio de las cuatro estimaciones. |

Es una autoevaluación orientativa con base en la rúbrica proporcionada, no una nota oficial. El proyecto contiene código de prueba, pero no se ejecutó `mvn test` en este trabajo.

## Ejecución con Nix

Desde `codex/`:

```bash
nix-shell
mvn spring-boot:run
```

Para renderizar los diagramas PlantUML:

```bash
plantuml diagrams/entregago-class-diagram.puml diagrams/domain-layer.puml diagrams/domain-fulfillment.puml diagrams/interfaces-layer.puml diagrams/application-layer.puml diagrams/infrastructure-layer.puml
```

Para ejecutar las pruebas bajo el shell:

```bash
mvn test
```

Los datos en memoria y el adaptador de pago son implementaciones didácticas para ejercitar las cuatro capas. La compensación de stock y autorización ilustra el flujo del caso de uso; una versión productiva usaría adaptadores transaccionales y una coordinación durable de compensaciones.

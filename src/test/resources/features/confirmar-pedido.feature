# language: es
Característica: Confirmar un pedido de EntregaGo
  Para evitar cobros incorrectos y ventas sin inventario
  Como cliente de EntregaGo
  Quiero confirmar pedidos sujetos a las reglas del comercio, cobertura, stock y pago

  Escenario: Pedido confirmado con condiciones comerciales aprobadas
    Dado el comercio "M-01" está "abierto"
    Y la dirección "A-01" del cliente "C-01" tiene cobertura "dentro" respecto del comercio
    Y la política de precios aplica envío de "4.00" PEN e impuesto de 10 por ciento
    Y el catálogo contiene estos productos:
      | producto | nombre       | categoría | precio | stock | cantidad solicitada |
      | P-01     | Hamburguesa  | Comida    | 20.00  | 5     | 1                   |
    Y el cupón "SAVE10" ofrece 10 por ciento con máximo de "5.00" PEN
    Y el medio de pago "TARJETA" responde "APROBADO"
    Cuando el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Entonces la API responde con HTTP 201
    Y el pedido queda en estado "CONFIRMED"
    Y el descuento calculado es "2.00" PEN
    Y el total calculado es "23.80" PEN
    Y la reserva de "P-01" es 1 unidades

  Esquema del escenario: El pedido se rechaza si falla una regla de confirmación
    Dado el comercio "M-01" está "<estadoComercio>"
    Y la dirección "A-01" del cliente "C-01" tiene cobertura "<cobertura>" respecto del comercio
    Y el producto "P-01" tiene stock <stock> unidades y se solicita <cantidad> unidad
    Y el medio de pago "TARJETA" responde "<pago>"
    Cuando el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Entonces la API responde con HTTP <http>
    Y la API rechaza con código "<codigo>"
    Y la reserva de "P-01" es 0 unidades

    Ejemplos:
      | estadoComercio | cobertura | stock | cantidad | pago       | http | codigo                  |
      | cerrado        | dentro    | 5     | 2        | APROBADO   | 409  | MERCHANT_CLOSED         |
      | abierto        | dentro    | 1     | 2        | APROBADO   | 409  | INSUFFICIENT_STOCK      |
      | abierto        | fuera     | 5     | 2        | APROBADO   | 422  | ADDRESS_OUT_OF_COVERAGE |
      | abierto        | dentro    | 5     | 2        | RECHAZADO  | 402  | PAYMENT_NOT_AUTHORIZED  |

  Esquema del escenario: El cupón se aplica solo cuando cumple sus reglas
    Dado el comercio "M-01" está "abierto"
    Y la dirección "A-01" del cliente "C-01" tiene cobertura "dentro" respecto del comercio
    Y el producto "P-01" tiene stock 5 unidades y se solicita 1 unidad
    Y el cupón "SAVE10" está en condición "<condicion>"
    Y el medio de pago "TARJETA" responde "APROBADO"
    Cuando el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Entonces la API responde con HTTP 201
    Y el pedido queda en estado "CONFIRMED"
    Y el descuento calculado es "<descuento>" PEN
    Y el motivo de rechazo del cupón es "<motivo>"

    Ejemplos:
      | condicion      | descuento | motivo                  |
      | VIGENTE        | 1.00      | NONE                    |
      | VENCIDO        | 0.00      | EXPIRED                 |
      | MONTO_MINIMO   | 0.00      | MINIMUM_NOT_MET         |
      | OTRO_COMERCIO  | 0.00      | MERCHANT_NOT_APPLICABLE |
      | LIMITE_USOS    | 0.00      | USAGE_LIMIT_REACHED     |

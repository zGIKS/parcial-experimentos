# language: en
Feature: Confirmación de pedidos en EntregaGo
  Para aceptar solo pedidos que se puedan preparar y entregar
  Como cliente
  El sistema confirma el pedido cuando se cumplen las reglas comerciales

  Scenario: Pedido confirmado con condiciones comerciales aprobadas
    Given el comercio "M-01" está "abierto"
    And la dirección "A-01" del cliente "C-01" tiene cobertura "cubierta" respecto del comercio
    And la política de precios aplica envío de 4.00 PEN e impuesto de 10 por ciento
    And el catálogo contiene estos productos:
      | productId | sku    | name       | unitPrice | stock | quantity |
      | P-01      | CAF-01 | Café       | 10.00     | 8     | 2        |
      | P-02      | BOL-02 | Pan        | 5.00      | 8     | 1        |
    And el cupón "BIENVENIDA" ofrece 10 por ciento con máximo de 3.00 PEN
    And el medio de pago "PM-OK" responde "APPROVED"
    When el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Then la API responde con HTTP 201
    And el pedido queda en estado "CONFIRMED"
    And el descuento calculado es 2.50 PEN
    And el total calculado es 28.75 PEN
    And la reserva de "P-01" es 2 unidades

  Scenario Outline: Pedido rechazado cuando una condición de confirmación no se cumple
    Given el comercio "M-01" está "<merchantState>"
    And la dirección "A-01" del cliente "C-01" tiene cobertura "<coverageState>" respecto del comercio
    And el producto "P-01" tiene stock <stock> unidades y se solicita 1 unidad
    And el medio de pago "PM-01" responde "<paymentResult>"
    When el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Then la API responde con HTTP <httpStatus>
    And la API rechaza con código "<reason>"
    And la reserva de "P-01" es 0 unidades

    Examples:
      | merchantState | coverageState | stock | paymentResult | httpStatus | reason                  |
      | cerrado       | cubierta      | 5     | APPROVED      | 409        | MERCHANT_CLOSED         |
      | abierto       | cubierta      | 0     | APPROVED      | 409        | INSUFFICIENT_STOCK      |
      | abierto       | fuera         | 5     | APPROVED      | 422        | OUT_OF_COVERAGE         |
      | abierto       | cubierta      | 5     | DECLINED      | 402        | PAYMENT_NOT_AUTHORIZED  |

  Scenario Outline: Cupón no aplicable conserva el precio sin descuento
    Given el comercio "M-01" está "abierto"
    And la dirección "A-01" del cliente "C-01" tiene cobertura "cubierta" respecto del comercio
    And el producto "P-01" tiene stock 5 unidades y se solicita 1 unidad
    And el cupón "PROMO" está en condición "<couponCondition>"
    And el medio de pago "PM-OK" responde "APPROVED"
    When el cliente "C-01" confirma el pedido en el comercio "M-01" para la dirección "A-01"
    Then la API responde con HTTP 201
    And el pedido queda en estado "CONFIRMED"
    And el descuento calculado es 0.00 PEN
    And el motivo de rechazo del cupón es "<couponReason>"

    Examples:
      | couponCondition | couponReason          |
      | VENCIDO         | COUPON_EXPIRED        |
      | MINIMO_NO_ALCANZADO | MINIMUM_NOT_MET    |
      | COMERCIO_NO_APLICABLE | MERCHANT_NOT_APPLICABLE |
      | LIMITE_AGOTADO  | USAGE_LIMIT_REACHED   |

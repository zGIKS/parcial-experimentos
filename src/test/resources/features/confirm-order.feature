Feature: Confirm a delivery order
  The customer can confirm an order only when merchant hours, product stock,
  delivery coverage and payment authorization allow it.

  Scenario: Confirm an order with multiple products
    Given merchant "M-1" is "open", merchant zone "Z-CENTRO", customer zone "Z-CENTRO" and payment "APPROVED"
    And the catalog contains:
      | productId | name       | category | price | stock |
      | P-1       | Hamburguesa| Comida   | 10.00 | 5     |
      | P-2       | Jugo       | Bebida   | 5.00  | 3     |
    And customer requests these products:
      | productId | quantity | observations |
      | P-1       | 2        | sin cebolla  |
      | P-2       | 1        |              |
    When the customer submits order confirmation
    Then the API responds with HTTP 201
    And the order is "CONFIRMED" with total "31.50"
    And stock for "P-1" has 2 reserved units
    And stock for "P-2" has 1 reserved unit

  Scenario Outline: Decline an order when a confirmation rule fails
    Given merchant "M-1" is "<merchantState>", merchant zone "Z-CENTRO", customer zone "<customerZone>" and payment "<paymentState>"
    And product "P-1" has price "10.00" and stock <stock>
    And customer requests product "P-1" quantity <quantity>
    When the customer submits order confirmation
    Then the API responds with HTTP <httpStatus>
    And there are <savedOrders> saved orders
    And stock for "P-1" has <reserved> reserved units

    Examples:
      | merchantState | customerZone | paymentState | stock | quantity | httpStatus | savedOrders | reserved |
      | closed        | Z-CENTRO     | APPROVED     | 5     | 2        | 409        | 0           | 0        |
      | open          | Z-CENTRO     | APPROVED     | 1     | 2        | 409        | 0           | 0        |
      | open          | Z-NORTE      | APPROVED     | 5     | 2        | 422        | 0           | 0        |
      | open          | Z-CENTRO     | DECLINED     | 5     | 2        | 402        | 0           | 0        |

  Scenario Outline: Apply only a coupon that satisfies its business rules
    Given merchant "M-1" is "open", merchant zone "Z-CENTRO", customer zone "Z-CENTRO" and payment "APPROVED"
    And product "P-1" has price "20.00" and stock 5
    And the coupon condition is "<condition>"
    And customer requests product "P-1" quantity 1
    When the customer submits order confirmation
    Then the API responds with HTTP 201
    And coupon discount is "<discount>" and rejection reason is "<rejection>"

    Examples:
      | condition       | discount | rejection              |
      | VALID           | 2.00     | NONE                   |
      | EXPIRED         | 0.00     | EXPIRED                |
      | BELOW_MINIMUM   | 0.00     | MINIMUM_NOT_MET        |
      | OTHER_MERCHANT  | 0.00     | MERCHANT_NOT_APPLICABLE|
      | LIMIT_REACHED   | 0.00     | USAGE_LIMIT_REACHED    |

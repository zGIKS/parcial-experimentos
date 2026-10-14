package pe.entregago.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.entregago.application.OrderConfirmationService;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Confirmación de pedidos del marketplace")
public class OrderConfirmationController {
    private final OrderConfirmationService confirmationService;

    public OrderConfirmationController(OrderConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirmar pedido", description = "Confirma solo si el comercio está abierto, " +
            "hay stock, la dirección tiene cobertura y el pago queda autorizado.")
    @ApiResponse(responseCode = "201", description = "Pedido confirmado",
            content = @Content(schema = @Schema(implementation = OrderConfirmationResponse.class)))
    @ApiResponse(responseCode = "402", description = "Medio de pago no autorizado")
    @ApiResponse(responseCode = "409", description = "Comercio cerrado o stock insuficiente")
    @ApiResponse(responseCode = "422", description = "Dirección fuera de cobertura")
    public ResponseEntity<OrderConfirmationResponse> confirm(@RequestBody OrderConfirmationRequest request) {
        var result = confirmationService.confirm(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderConfirmationResponse.from(result));
    }
}

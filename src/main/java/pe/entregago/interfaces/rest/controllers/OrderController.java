package pe.entregago.interfaces.rest.controllers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.entregago.application.ConfirmOrderCommandService;
import pe.entregago.interfaces.rest.resources.ConfirmOrderRequest;
import pe.entregago.interfaces.rest.resources.OrderConfirmationResource;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final ConfirmOrderCommandService confirmOrder;
    public OrderController(ConfirmOrderCommandService confirmOrder) { this.confirmOrder=confirmOrder; }
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderConfirmationResource confirm(@RequestBody ConfirmOrderRequest request) {
        return OrderConfirmationResource.from(confirmOrder.handle(request.toCommand()));
    }
}

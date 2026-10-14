package pe.entregago.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.entregago.domain.model.FailureReason;
import pe.entregago.domain.model.OrderRejectedException;

@RestControllerAdvice
public class OrderRejectedExceptionHandler {
    @ExceptionHandler(OrderRejectedException.class)
    public ResponseEntity<ProblemDetail> handle(OrderRejectedException exception) {
        HttpStatus status = statusFor(exception.reason());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.reason().name());
        problem.setTitle("Order confirmation rejected");
        problem.setProperty("code", exception.reason().name());
        return ResponseEntity.status(status).body(problem);
    }

    private HttpStatus statusFor(FailureReason reason) {
        return switch (reason) {
            case PAYMENT_NOT_AUTHORIZED -> HttpStatus.PAYMENT_REQUIRED;
            case OUT_OF_COVERAGE, ADDRESS_NOT_FOUND, ADDRESS_NOT_OWNED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case MERCHANT_NOT_FOUND, MERCHANT_CLOSED, PRODUCT_NOT_FOUND, PRODUCT_UNAVAILABLE, INSUFFICIENT_STOCK ->
                    HttpStatus.CONFLICT;
        };
    }
}

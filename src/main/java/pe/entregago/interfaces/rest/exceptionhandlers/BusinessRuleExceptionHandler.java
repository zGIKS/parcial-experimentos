package pe.entregago.interfaces.rest.exceptionhandlers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.interfaces.rest.resources.ApiErrorResource;
@RestControllerAdvice
public class BusinessRuleExceptionHandler {
    @ExceptionHandler(BusinessRuleViolation.class)
    public ResponseEntity<ApiErrorResource> handle(BusinessRuleViolation failure) {
        HttpStatus status=switch (failure.code()) {
            case MERCHANT_NOT_FOUND, ADDRESS_NOT_FOUND, PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ADDRESS_OUT_OF_COVERAGE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PAYMENT_NOT_AUTHORIZED -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(new ApiErrorResource(failure.code().name(), failure.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResource> handleIllegalArgument(IllegalArgumentException failure) {
        return ResponseEntity.badRequest().body(new ApiErrorResource("INVALID_REQUEST", failure.getMessage()));
    }
}

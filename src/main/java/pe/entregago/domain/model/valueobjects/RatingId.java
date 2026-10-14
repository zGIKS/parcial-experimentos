package pe.entregago.domain.model.valueobjects;

public record RatingId(String value) {
    public RatingId { CustomerId.requireText(value, "ratingId"); }
}

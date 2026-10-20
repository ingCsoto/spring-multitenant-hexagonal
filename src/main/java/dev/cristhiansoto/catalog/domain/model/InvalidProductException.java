package dev.cristhiansoto.catalog.domain.model;

public class InvalidProductException extends RuntimeException {

    public InvalidProductException(String message) {
        super(message);
    }
}

package dev.cristhiansoto.catalog.domain.model;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String name) {
        super("A product named '" + name + "' already exists");
    }
}

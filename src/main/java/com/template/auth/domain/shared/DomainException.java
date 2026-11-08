package com.template.auth.domain.shared;

/**
 * Base of every expected business-flow failure. Subclasses (see
 * {@code domain.shared.exception}) are mapped to an HTTP status and a stable
 * machine-readable code in the presentation layer - the domain itself knows
 * nothing about HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

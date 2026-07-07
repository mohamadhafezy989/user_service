package com.eman.user_service.exception;


/**
 * Exception for tenant not found
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}

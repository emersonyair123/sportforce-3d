package com.sportforce3d.service;

/** Credenciales incorrectas, cuenta inactiva o sesión inexistente. */
public class AuthenticationException extends ServiceException {

    public AuthenticationException(String message) {
        super(message);
    }
}

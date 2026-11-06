package com.sportforce3d.service;

/** Error de negocio con un mensaje apto para mostrar al usuario. */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}

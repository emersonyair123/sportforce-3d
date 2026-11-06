package com.sportforce3d.service;

/** El usuario de la sesión no tiene el rol necesario para la operación. */
public class AccessDeniedException extends ServiceException {

    public AccessDeniedException(String message) {
        super(message);
    }
}

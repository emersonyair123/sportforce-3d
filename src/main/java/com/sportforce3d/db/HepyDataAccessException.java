package com.sportforce3d.db;

public class HepyDataAccessException extends RuntimeException {

    public HepyDataAccessException(String hepyMessage, Throwable hepyCause) {
        super(hepyMessage, hepyCause);
    }
}

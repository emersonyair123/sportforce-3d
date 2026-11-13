package com.sportforce3d.db;

import java.sql.SQLException;

@FunctionalInterface
public interface HepySqlFunction<T, R> {

    R hepyApply(T hepyValue) throws SQLException;
}

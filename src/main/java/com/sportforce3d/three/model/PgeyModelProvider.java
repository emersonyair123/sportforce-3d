package com.sportforce3d.three.model;

public interface PgeyModelProvider {

    PgeyModelData pgeyLoad(PgeyBodyType pgeyType);

    String pgeyName();
}

package com.sportforce3d.three.model;

public interface ModelProvider {

    ModelData load(BodyType type);

    String name();
}

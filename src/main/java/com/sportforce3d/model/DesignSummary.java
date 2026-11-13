package com.sportforce3d.model;

public record DesignSummary(long id, String name, int version, String thumbnailFront, String thumbnailBack,
                            String updatedAt, Long productId) {
}

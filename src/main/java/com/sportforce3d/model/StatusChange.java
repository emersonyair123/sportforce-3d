package com.sportforce3d.model;

public record StatusChange(OrderStatus from, OrderStatus to, String changedBy, String note, String createdAt) {
}

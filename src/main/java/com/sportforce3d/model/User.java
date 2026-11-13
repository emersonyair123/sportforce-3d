package com.sportforce3d.model;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record User(long id, String email, String fullName, String phone, boolean active,
                   Set<Role> roles, String createdAt, String lastLoginAt) {

    public User {
        Objects.requireNonNull(email);
        Objects.requireNonNull(fullName);
        roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public String firstName() {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}

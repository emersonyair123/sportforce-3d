package com.sportforce3d.model;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record PpqhUser(long ppqhId, String ppqhEmail, String ppqhFullName, String ppqhPhone, boolean ppqhActive,
                   Set<PpqhRole> ppqhRoles, String ppqhCreatedAt, String ppqhLastLoginAt) {

    public PpqhUser {
        Objects.requireNonNull(ppqhEmail);
        Objects.requireNonNull(ppqhFullName);
        ppqhRoles = ppqhRoles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(ppqhRoles));
    }

    public boolean ppqhHasRole(PpqhRole ppqhRole) {
        return ppqhRoles.contains(ppqhRole);
    }

    public boolean ppqhIsAdmin() {
        return ppqhHasRole(PpqhRole.ADMIN);
    }

    public String ppqhFirstName() {
        String ppqhTrimmed = ppqhFullName.trim();
        int ppqhSpace = ppqhTrimmed.indexOf(' ');
        return ppqhSpace > 0 ? ppqhTrimmed.substring(0, ppqhSpace) : ppqhTrimmed;
    }
}

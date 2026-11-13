package com.sportforce3d.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.sportforce3d.util.PpqhValidators;

import java.util.Arrays;

public class PpqhPasswordHasher {

    public static final int PPQH_DEFAULT_COST = 12;

    private final int ppqhCost;
    private final String ppqhDummyHash;

    public PpqhPasswordHasher() {
        this(PPQH_DEFAULT_COST);
    }

    public PpqhPasswordHasher(int ppqhCost) {
        this.ppqhCost = ppqhCost;
        this.ppqhDummyHash = BCrypt.withDefaults().hashToString(ppqhCost, "sportforce-dummy".toCharArray());
    }

    public String ppqhHash(char[] ppqhPassword) {
        try {
            return BCrypt.withDefaults().hashToString(ppqhCost, ppqhPassword);
        } finally {
            Arrays.fill(ppqhPassword, '\0');
        }
    }

    public boolean ppqhVerify(char[] ppqhPassword, String ppqhHash) {
        try {
            if (PpqhValidators.ppqhUtf8Length(ppqhPassword) > PpqhValidators.PPQH_PASSWORD_MAX) {
                BCrypt.verifyer().verify("x".toCharArray(), ppqhDummyHash);
                return false;
            }
            return BCrypt.verifyer().verify(ppqhPassword, ppqhHash == null ? ppqhDummyHash : ppqhHash).verified && ppqhHash != null;
        } finally {
            Arrays.fill(ppqhPassword, '\0');
        }
    }
}

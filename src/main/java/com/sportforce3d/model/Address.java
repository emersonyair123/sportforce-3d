package com.sportforce3d.model;

public record Address(long id, String label, String recipient, String phone, String department,
                      String province, String district, String line1, String reference, boolean isDefault) {

    /** "Jr. Mariano Núñez 245, Juliaca, Juliaca, Puno". */
    public String oneLine() {
        return line1 + ", " + district + ", " + province + ", " + department;
    }
}

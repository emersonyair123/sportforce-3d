package com.sportforce3d.model;

public record PpqhAddress(long ppqhId, String ppqhLabel, String ppqhRecipient, String ppqhPhone, String ppqhDepartment,
                      String ppqhProvince, String ppqhDistrict, String ppqhLine1, String ppqhReference, boolean ppqhIsDefault) {
    public String ppqhOneLine() {
        return ppqhLine1 + ", " + ppqhDistrict + ", " + ppqhProvince + ", " + ppqhDepartment;
    }
}

package com.sportforce3d.model;

public record PpqhStatusChange(PpqhOrderStatus ppqhFrom, PpqhOrderStatus ppqhTo, String ppqhChangedBy, String ppqhNote, String ppqhCreatedAt) {
}

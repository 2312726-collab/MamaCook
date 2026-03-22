package com.example.mamacook;

public class DanhMuc {
    private String id_danh_muc;
    private String ten_danh_muc;

    public DanhMuc() {}

    public DanhMuc(String ten_danh_muc) {
        this.ten_danh_muc = ten_danh_muc;
    }

    public String getId_danh_muc() { return id_danh_muc; }
    public void setId_danh_muc(String id_danh_muc) { this.id_danh_muc = id_danh_muc; }
    public String getTen_danh_muc() { return ten_danh_muc; }
}

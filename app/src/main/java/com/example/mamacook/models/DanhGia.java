package com.example.mamacook.models;

import com.google.firebase.Timestamp;

public class DanhGia {
    private String id_nguoi_dung;
    private String ten_nguoi_dung;
    private String avatar_url;
    private String id_mon_an;
    private String noi_dung;
    private float so_sao;
    private String hinh_anh_url;
    private String trang_thai;
    private Timestamp ngay_danh_gia;

    public DanhGia() {
    }

    public DanhGia(String id_nguoi_dung, String ten_nguoi_dung, String avatar_url, String id_mon_an,
                   String noi_dung, float so_sao, String hinh_anh_url, String trang_thai, Timestamp ngay_danh_gia) {
        this.id_nguoi_dung = id_nguoi_dung;
        this.ten_nguoi_dung = ten_nguoi_dung;
        this.avatar_url = avatar_url;
        this.id_mon_an = id_mon_an;
        this.noi_dung = noi_dung;
        this.so_sao = so_sao;
        this.hinh_anh_url = hinh_anh_url;
        this.trang_thai = trang_thai;
        this.ngay_danh_gia = ngay_danh_gia;
    }

    public String getId_nguoi_dung() {
        return id_nguoi_dung;
    }

    public void setId_nguoi_dung(String id_nguoi_dung) {
        this.id_nguoi_dung = id_nguoi_dung;
    }

    public String getTen_nguoi_dung() {
        return ten_nguoi_dung;
    }

    public void setTen_nguoi_dung(String ten_nguoi_dung) {
        this.ten_nguoi_dung = ten_nguoi_dung;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public String getId_mon_an() {
        return id_mon_an;
    }

    public void setId_mon_an(String id_mon_an) {
        this.id_mon_an = id_mon_an;
    }

    public String getNoi_dung() {
        return noi_dung;
    }

    public void setNoi_dung(String noi_dung) {
        this.noi_dung = noi_dung;
    }

    public float getSo_sao() {
        return so_sao;
    }

    public void setSo_sao(float so_sao) {
        this.so_sao = so_sao;
    }

    public String getHinh_anh_url() {
        return hinh_anh_url;
    }

    public void setHinh_anh_url(String hinh_anh_url) {
        this.hinh_anh_url = hinh_anh_url;
    }

    public String getTrang_thai() {
        return trang_thai;
    }

    public void setTrang_thai(String trang_thai) {
        this.trang_thai = trang_thai;
    }

    public Timestamp getNgay_danh_gia() {
        return ngay_danh_gia;
    }

    public void setNgay_danh_gia(Timestamp ngay_danh_gia) {
        this.ngay_danh_gia = ngay_danh_gia;
    }
}

package com.example.mamacook;

import com.google.firebase.Timestamp;

public class User {
    private String id_nguoi_dung;
    private String ho_ten;
    private String email;
    private String mat_khau;
    private String anh_dai_dien;
    private String ngay_sinh;
    private String gioi_tinh;
    private String so_dien_thoai;
    private Timestamp ngay_tao;
    private String trang_thai_tai_khoan;

    public User() {}

    public User(String ho_ten, String email, String mat_khau) {
        this.ho_ten = ho_ten;
        this.email = email;
        this.mat_khau = mat_khau;
        this.ngay_tao = Timestamp.now();
        this.trang_thai_tai_khoan = "active";
    }

    // Full Getters and Setters
    public String getId_nguoi_dung() { return id_nguoi_dung; }
    public void setId_nguoi_dung(String id_nguoi_dung) { this.id_nguoi_dung = id_nguoi_dung; }
    public String getHo_ten() { return ho_ten; }
    public void setHo_ten(String ho_ten) { this.ho_ten = ho_ten; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMat_khau() { return mat_khau; }
    public void setMat_khau(String mat_khau) { this.mat_khau = mat_khau; }
    public String getAnh_dai_dien() { return anh_dai_dien; }
    public void setAnh_dai_dien(String anh_dai_dien) { this.anh_dai_dien = anh_dai_dien; }
    public String getNgay_sinh() { return ngay_sinh; }
    public void setNgay_sinh(String ngay_sinh) { this.ngay_sinh = ngay_sinh; }
    public String getGioi_tinh() { return gioi_tinh; }
    public void setGioi_tinh(String gioi_tinh) { this.gioi_tinh = gioi_tinh; }
    public String getSo_dien_thoai() { return so_dien_thoai; }
    public void setSo_dien_thoai(String so_dien_thoai) { this.so_dien_thoai = so_dien_thoai; }
    public Timestamp getNgay_tao() { return ngay_tao; }
    public void setNgay_tao(Timestamp ngay_tao) { this.ngay_tao = ngay_tao; }
    public String getTrang_thai_tai_khoan() { return trang_thai_tai_khoan; }
    public void setTrang_thai_tai_khoan(String trang_thai_tai_khoan) { this.trang_thai_tai_khoan = trang_thai_tai_khoan; }
}

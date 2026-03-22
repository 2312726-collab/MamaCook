package com.example.mamacook;

import com.google.firebase.Timestamp;

public class User {
    private String id_nguoi_dung; // Firestore Document ID
    private String ho_ten;
    private String email;
    private String mat_khau;
    private String anh_dai_dien;
    private String ngay_sinh;
    private String gioi_tinh;
    private String so_dien_thoai;
    private Timestamp ngay_tao;
    private String trang_thai_tai_khoan;

    public User() {} // Bắt buộc cho Firebase

    public User(String ho_ten, String email, String mat_khau) {
        this.ho_ten = ho_ten;
        this.email = email;
        this.mat_khau = mat_khau;
        this.ngay_tao = Timestamp.now();
        this.trang_thai_tai_khoan = "active";
    }

    // Getters and Setters
    public String getId_nguoi_dung() { return id_nguoi_dung; }
    public void setId_nguoi_dung(String id_nguoi_dung) { this.id_nguoi_dung = id_nguoi_dung; }
    public String getHo_ten() { return ho_ten; }
    public String getEmail() { return email; }
    public String getAnh_dai_dien() { return anh_dai_dien; }
    public String getNgay_sinh() { return ngay_sinh; }
    public String getGioi_tinh() { return gioi_tinh; }
    public String getSo_dien_thoai() { return so_dien_thoai; }
    public Timestamp getNgay_tao() { return ngay_tao; }
    public String getTrang_thai_tai_khoan() { return trang_thai_tai_khoan; }
}

package com.example.mamacook;

import com.google.firebase.Timestamp;
import java.util.List;

public class MonAn {
    private String id_mon_an;
    private String ten_mon;
    private String hinh_anh;
    private int thoi_gian_nau;
    private String do_kho;
    private String id_danh_muc;
    private Timestamp ngay_tao;
    private String trang_thai;
    private int luot_xem;
    private double rating;
    private int tong_luot_danh_gia;

    private List<BuocNau> danh_sach_buoc_nau;
    private List<ChiTietNguyenLieu> danh_sach_nguyen_lieu;
    
    // Trường mới để hỗ trợ tìm kiếm cả tên món và nguyên liệu
    private List<String> tu_khoa_tim_kiem;

    public MonAn() {}

    public static class BuocNau {
        public int so_thu_tu;
        public String noi_dung_buoc;
        public String hinh_anh_buoc;
        public BuocNau() {}
    }

    public static class ChiTietNguyenLieu {
        public String ten_nguyen_lieu;
        public int so_luong;
        public String don_vi;
        public ChiTietNguyenLieu() {}
    }

    // Getters và Setters
    public String getId_mon_an() { return id_mon_an; }
    public void setId_mon_an(String id_mon_an) { this.id_mon_an = id_mon_an; }
    public String getTen_mon() { return ten_mon; }
    public void setTen_mon(String ten_mon) { this.ten_mon = ten_mon; }
    public String getHinh_anh() { return hinh_anh; }
    public void setHinh_anh(String hinh_anh) { this.hinh_anh = hinh_anh; }
    public int getThoi_gian_nau() { return thoi_gian_nau; }
    public void setThoi_gian_nau(int thoi_gian_nau) { this.thoi_gian_nau = thoi_gian_nau; }
    public String getDo_kho() { return do_kho; }
    public void setDo_kho(String do_kho) { this.do_kho = do_kho; }
    public String getId_danh_muc() { return id_danh_muc; }
    public void setId_danh_muc(String id_danh_muc) { this.id_danh_muc = id_danh_muc; }
    public Timestamp getNgay_tao() { return ngay_tao; }
    public void setNgay_tao(Timestamp ngay_tao) { this.ngay_tao = ngay_tao; }
    public String getTrang_thai() { return trang_thai; }
    public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }
    public int getLuot_xem() { return luot_xem; }
    public void setLuot_xem(int luot_xem) { this.luot_xem = luot_xem; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getTong_luot_danh_gia() { return tong_luot_danh_gia; }
    public void setTong_luot_danh_gia(int tong_luot_danh_gia) { this.tong_luot_danh_gia = tong_luot_danh_gia; }
    public List<BuocNau> getDanh_sach_buoc_nau() { return danh_sach_buoc_nau; }
    public void setDanh_sach_buoc_nau(List<BuocNau> danh_sach_buoc_nau) { this.danh_sach_buoc_nau = danh_sach_buoc_nau; }
    public List<ChiTietNguyenLieu> getDanh_sach_nguyen_lieu() { return danh_sach_nguyen_lieu; }
    public void setDanh_sach_nguyen_lieu(List<ChiTietNguyenLieu> danh_sach_nguyen_lieu) { this.danh_sach_nguyen_lieu = danh_sach_nguyen_lieu; }
    public List<String> getTu_khoa_tim_kiem() { return tu_khoa_tim_kiem; }
    public void setTu_khoa_tim_kiem(List<String> tu_khoa_tim_kiem) { this.tu_khoa_tim_kiem = tu_khoa_tim_kiem; }
}

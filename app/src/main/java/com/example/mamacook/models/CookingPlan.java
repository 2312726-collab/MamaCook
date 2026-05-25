package com.example.mamacook.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class CookingPlan {
    private String id_nguoi_dung;
    private String id_mon_an;
    private String id_plan; // Thêm trường này để lưu ID document Firestore
    private String ten_mon;
    private String hinh_anh;
    private Timestamp ngay_lap_ke_hoach;
    private String trang_thai; // "dang_di_cho" hoặc "cho_nau"
    private String buoi; // "Sang", "Trua", "Toi"
    private String ngay_chi_tiet; // "yyyy-MM-dd"
    private List<IngredientItem> danh_sach_nguyen_lieu;

    public CookingPlan() {}

    public String getId_plan() { return id_plan; }
    public void setId_plan(String id_plan) { this.id_plan = id_plan; }

    public String getBuoi() { return buoi; }
    public void setBuoi(String buoi) { this.buoi = buoi; }

    public String getNgay_chi_tiet() { return ngay_chi_tiet; }
    public void setNgay_chi_tiet(String ngay_chi_tiet) { this.ngay_chi_tiet = ngay_chi_tiet; }

    public static class IngredientItem {
        private String ten_nguyen_lieu;
        private int so_luong;
        private String don_vi;
        private boolean da_mua;

        public IngredientItem() {}

        public String getTen_nguyen_lieu() { return ten_nguyen_lieu; }
        public void setTen_nguyen_lieu(String ten_nguyen_lieu) { this.ten_nguyen_lieu = ten_nguyen_lieu; }
        public int getSo_luong() { return so_luong; }
        public void setSo_luong(int so_luong) { this.so_luong = so_luong; }
        public String getDon_vi() { return don_vi; }
        public void setDon_vi(String don_vi) { this.don_vi = don_vi; }
        public boolean isDa_mua() { return da_mua; }
        public void setDa_mua(boolean da_mua) { this.da_mua = da_mua; }
    }

    public String getId_nguoi_dung() { return id_nguoi_dung; }
    public void setId_nguoi_dung(String id_nguoi_dung) { this.id_nguoi_dung = id_nguoi_dung; }
    public String getId_mon_an() { return id_mon_an; }
    public void setId_mon_an(String id_mon_an) { this.id_mon_an = id_mon_an; }
    public String getTen_mon() { return ten_mon; }
    public void setTen_mon(String ten_mon) { this.ten_mon = ten_mon; }
    public String getHinh_anh() { return hinh_anh; }
    public void setHinh_anh(String hinh_anh) { this.hinh_anh = hinh_anh; }
    public Timestamp getNgay_lap_ke_hoach() { return ngay_lap_ke_hoach; }
    public void setNgay_lap_ke_hoach(Timestamp ngay_lap_ke_hoach) { this.ngay_lap_ke_hoach = ngay_lap_ke_hoach; }
    public String getTrang_thai() { return trang_thai; }
    public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }
    public List<IngredientItem> getDanh_sach_nguyen_lieu() { return danh_sach_nguyen_lieu; }
    public void setDanh_sach_nguyen_lieu(List<IngredientItem> danh_sach_nguyen_lieu) { this.danh_sach_nguyen_lieu = danh_sach_nguyen_lieu; }
}

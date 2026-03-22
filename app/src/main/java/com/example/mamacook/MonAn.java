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
    
    // Tối ưu Firestore: Lồng danh sách vào thay vì tách bảng
    private List<String> cac_buoc_nau; 
    private List<NguyenLieuMon> danh_sach_nguyen_lieu;

    public MonAn() {}

    public MonAn(String ten_mon, String hinh_anh, int thoi_gian_nau, String do_kho, String id_danh_muc) {
        this.ten_mon = ten_mon;
        this.hinh_anh = hinh_anh;
        this.thoi_gian_nau = thoi_gian_nau;
        this.do_kho = do_kho;
        this.id_danh_muc = id_danh_muc;
        this.ngay_tao = Timestamp.now();
        this.trang_thai = "cho_duyet";
    }

    // Lớp nội bộ để lưu nguyên liệu (Gộp từ CHI_TIET_NGUYEN_LIEU và NGUYEN_LIEU)
    public static class NguyenLieuMon {
        public String ten_nguyen_lieu;
        public String so_luong;
        public String don_vi;

        public NguyenLieuMon() {}
        public NguyenLieuMon(String ten, String sl, String dv) {
            this.ten_nguyen_lieu = ten;
            this.so_luong = sl;
            this.don_vi = dv;
        }
    }

    // Getters
    public String getId_mon_an() { return id_mon_an; }
    public String getTen_mon() { return ten_mon; }
    public String getHinh_anh() { return hinh_anh; }
    public int getThoi_gian_nau() { return thoi_gian_nau; }
    public String getDo_kho() { return do_kho; }
    public List<String> getCac_buoc_nau() { return cac_buoc_nau; }
    public List<NguyenLieuMon> getDanh_sach_nguyen_lieu() { return danh_sach_nguyen_lieu; }
}

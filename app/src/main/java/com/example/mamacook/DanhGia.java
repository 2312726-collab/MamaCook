package com.example.mamacook;

import java.util.Date;

public class DanhGia {
    private String id_mon_an;
    private String noi_dung;
    private float so_sao;
    private String trang_thai; // "cho_duyet" hoặc "hien_thi"
    private String ten_nguoi_dung;
    private Date ngay_danh_gia;

    // Constructor rỗng cho Firebase
    public DanhGia() {
    }

    // Constructor đầy đủ
    public DanhGia(String id_mon_an, String noi_dung, float so_sao, String trang_thai, String ten_nguoi_dung, Date ngay_danh_gia) {
        this.id_mon_an = id_mon_an;
        this.noi_dung = noi_dung;
        this.so_sao = so_sao;
        this.trang_thai = trang_thai;
        this.ten_nguoi_dung = ten_nguoi_dung;
        this.ngay_danh_gia = ngay_danh_gia;
    }

    // Getter và Setter
    public String getId_mon_an() { return id_mon_an; }
    public void setId_mon_an(String id_mon_an) { this.id_mon_an = id_mon_an; }

    public String getNoi_dung() { return noi_dung; }
    public void setNoi_dung(String noi_dung) { this.noi_dung = noi_dung; }

    public float getSo_sao() { return so_sao; }
    public void setSo_sao(float so_sao) { this.so_sao = so_sao; }

    public String getTrang_thai() { return trang_thai; }
    public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }

    public String getTen_nguoi_dung() { return ten_nguoi_dung; }
    public void setTen_nguoi_dung(String ten_nguoi_dung) { this.ten_nguoi_dung = ten_nguoi_dung; }

    public Date getNgay_danh_gia() { return ngay_danh_gia; }
    public void setNgay_danh_gia(Date ngay_danh_gia) { this.ngay_danh_gia = ngay_danh_gia; }
}

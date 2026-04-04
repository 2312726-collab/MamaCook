package com.example.mamacook;

import com.google.firebase.Timestamp;

public class DanhGia {
    private String id_danh_gia;
    private String id_nguoi_dung;
    private String id_mon_an;
    private String ho_ten;
    private String anh_dai_dien;
    private int so_sao;
    private String noi_dung_danh_gia;
    private String trang_thai; // 'hien_thi' hoặc 'pending'
    private Timestamp ngay_danh_gia;

    public DanhGia() {}

    public String getId_danh_gia() { return id_danh_gia; }
    public void setId_danh_gia(String id_danh_gia) { this.id_danh_gia = id_danh_gia; }

    public String getId_nguoi_dung() { return id_nguoi_dung; }
    public void setId_nguoi_dung(String id_nguoi_dung) { this.id_nguoi_dung = id_nguoi_dung; }

    public String getId_mon_an() { return id_mon_an; }
    public void setId_mon_an(String id_mon_an) { this.id_mon_an = id_mon_an; }

    public String getHo_ten() { return ho_ten; }
    public void setHo_ten(String ho_ten) { this.ho_ten = ho_ten; }

    public String getAnh_dai_dien() { return anh_dai_dien; }
    public void setAnh_dai_dien(String anh_dai_dien) { this.anh_dai_dien = anh_dai_dien; }

    public int getSo_sao() { return so_sao; }
    public void setSo_sao(int so_sao) { this.so_sao = so_sao; }

    public String getNoi_dung_danh_gia() { return noi_dung_danh_gia; }
    public void setNoi_dung_danh_gia(String noi_dung_danh_gia) { this.noi_dung_danh_gia = noi_dung_danh_gia; }

    public String getTrang_thai() { return trang_thai; }
    public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }

    public Timestamp getNgay_danh_gia() { return ngay_danh_gia; }
    public void setNgay_danh_gia(Timestamp ngay_danh_gia) { this.ngay_danh_gia = ngay_danh_gia; }
}

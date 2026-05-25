package com.example.mamacook.models;

import com.google.firebase.Timestamp;

public class ThongBao {

    private String id;
    private String tieu_de;
    private String noi_dung;
    private Timestamp ngay_tao;
    private String nguoi_gui;

    public ThongBao() {
    }

    public ThongBao(String tieu_de,
                    String noi_dung,
                    Timestamp ngay_tao,
                    String nguoi_gui) {

        this.tieu_de = tieu_de;
        this.noi_dung = noi_dung;
        this.ngay_tao = ngay_tao;
        this.nguoi_gui = nguoi_gui;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTieu_de() {
        return tieu_de;
    }

    public void setTieu_de(String tieu_de) {
        this.tieu_de = tieu_de;
    }

    public String getNoi_dung() {
        return noi_dung;
    }

    public void setNoi_dung(String noi_dung) {
        this.noi_dung = noi_dung;
    }

    public Timestamp getNgay_tao() {
        return ngay_tao;
    }

    public void setNgay_tao(Timestamp ngay_tao) {
        this.ngay_tao = ngay_tao;
    }

    public String getNguoi_gui() {
        return nguoi_gui;
    }

    public void setNguoi_gui(String nguoi_gui) {
        this.nguoi_gui = nguoi_gui;
    }
}
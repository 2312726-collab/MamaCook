package com.example.mamacook;

import com.google.firebase.Timestamp;

public class LichSuXem {
    private String id_lich_su;
    private String id_nguoi_dung;
    private String id_mon_an;
    private Timestamp thoi_gian_xem;

    public LichSuXem() {}

    public String getId_lich_su() { return id_lich_su; }
    public void setId_lich_su(String id_lich_su) { this.id_lich_su = id_lich_su; }

    public String getId_nguoi_dung() { return id_nguoi_dung; }
    public void setId_nguoi_dung(String id_nguoi_dung) { this.id_nguoi_dung = id_nguoi_dung; }

    public String getId_mon_an() { return id_mon_an; }
    public void setId_mon_an(String id_mon_an) { this.id_mon_an = id_mon_an; }

    public Timestamp getThoi_gian_xem() { return thoi_gian_xem; }
    public void setThoi_gian_xem(Timestamp thoi_gian_xem) { this.thoi_gian_xem = thoi_gian_xem; }
}

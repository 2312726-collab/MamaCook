package com.example.mamacook.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MonAn implements Parcelable {
    private String id_mon_an;
    private String ten_mon;
    private String hinh_anh;
    private int thoi_gian_nau;
    private String do_kho;
    private String id_danh_muc;
    private Timestamp ngay_tao;
    private Timestamp ngay_cap_nhat;
    private String trang_thai;
    private int luot_xem;
    private double rating;
    private int reviewCount;
    private int tong_luot_danh_gia;
    private double totalScore;
    private double gia_tien;
    private int match_score;
    private int khau_phan;
    private String vung_mien;

    private List<BuocNau> danh_sach_buoc_nau;
    private List<ChiTietNguyenLieu> danh_sach_nguyen_lieu;
    private List<SoChe> danh_sach_so_che;

    private List<String> tu_khoa_tim_kiem;

    public MonAn() {}

    protected MonAn(Parcel in) {
        id_mon_an = in.readString();
        ten_mon = in.readString();
        hinh_anh = in.readString();
        thoi_gian_nau = in.readInt();
        do_kho = in.readString();
        id_danh_muc = in.readString();
        long ngayTaoSeconds = in.readLong();
        if (ngayTaoSeconds != -1) ngay_tao = new Timestamp(ngayTaoSeconds, in.readInt());
        long ngayCapNhatSeconds = in.readLong();
        if (ngayCapNhatSeconds != -1) ngay_cap_nhat = new Timestamp(ngayCapNhatSeconds, in.readInt());
        trang_thai = in.readString();
        luot_xem = in.readInt();
        rating = in.readDouble();
        reviewCount = in.readInt();
        totalScore = in.readDouble();
        gia_tien = in.readDouble();
        match_score = in.readInt();
        khau_phan = in.readInt();
        vung_mien = in.readString();
        danh_sach_buoc_nau = in.createTypedArrayList(BuocNau.CREATOR);
        danh_sach_nguyen_lieu = in.createTypedArrayList(ChiTietNguyenLieu.CREATOR);
        danh_sach_so_che = in.createTypedArrayList(SoChe.CREATOR);
        tu_khoa_tim_kiem = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id_mon_an);
        dest.writeString(ten_mon);
        dest.writeString(hinh_anh);
        dest.writeInt(thoi_gian_nau);
        dest.writeString(do_kho);
        dest.writeString(id_danh_muc);
        if (ngay_tao != null) {
            dest.writeLong(ngay_tao.getSeconds());
            dest.writeInt(ngay_tao.getNanoseconds());
        } else dest.writeLong(-1);
        if (ngay_cap_nhat != null) {
            dest.writeLong(ngay_cap_nhat.getSeconds());
            dest.writeInt(ngay_cap_nhat.getNanoseconds());
        } else dest.writeLong(-1);
        dest.writeString(trang_thai);
        dest.writeInt(luot_xem);
        dest.writeDouble(rating);
        dest.writeInt(reviewCount);
        dest.writeDouble(totalScore);
        dest.writeDouble(gia_tien);
        dest.writeInt(match_score);
        dest.writeInt(khau_phan);
        dest.writeString(vung_mien);
        dest.writeTypedList(danh_sach_buoc_nau);
        dest.writeTypedList(danh_sach_nguyen_lieu);
        dest.writeTypedList(danh_sach_so_che);
        dest.writeStringList(tu_khoa_tim_kiem);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<MonAn> CREATOR = new Creator<MonAn>() {
        @Override
        public MonAn createFromParcel(Parcel in) { return new MonAn(in); }
        @Override
        public MonAn[] newArray(int size) { return new MonAn[size]; }
    };

    public static class SoChe implements Parcelable {
        public String tieu_de;
        public String noi_dung;
        public SoChe() {}
        protected SoChe(Parcel in) {
            tieu_de = in.readString();
            noi_dung = in.readString();
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(tieu_de);
            dest.writeString(noi_dung);
        }
        @Override
        public int describeContents() { return 0; }
        public static final Creator<SoChe> CREATOR = new Creator<SoChe>() {
            @Override
            public SoChe createFromParcel(Parcel in) { return new SoChe(in); }
            @Override
            public SoChe[] newArray(int size) { return new SoChe[size]; }
        };
    }

    public static class BuocNau implements Parcelable {
        public int so_buoc;
        public String tieu_de;
        public String mo_ta;
        public String hinh_anh_buoc;
        public int thoi_gian_buoc;
        public BuocNau() {}
        protected BuocNau(Parcel in) {
            so_buoc = in.readInt();
            tieu_de = in.readString();
            mo_ta = in.readString();
            hinh_anh_buoc = in.readString();
            thoi_gian_buoc = in.readInt();
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(so_buoc);
            dest.writeString(tieu_de);
            dest.writeString(mo_ta);
            dest.writeString(hinh_anh_buoc);
            dest.writeInt(thoi_gian_buoc);
        }
        @Override
        public int describeContents() { return 0; }
        public static final Creator<BuocNau> CREATOR = new Creator<BuocNau>() {
            @Override
            public BuocNau createFromParcel(Parcel in) { return new BuocNau(in); }
            @Override
            public BuocNau[] newArray(int size) { return new BuocNau[size]; }
        };
    }

    public static class ChiTietNguyenLieu implements Parcelable {
        public String ten_nguyen_lieu;
        public double so_luong;
        public String don_vi;
        public String ghi_chu;
        public ChiTietNguyenLieu() {}
        protected ChiTietNguyenLieu(Parcel in) {
            ten_nguyen_lieu = in.readString();
            so_luong = in.readDouble();
            don_vi = in.readString();
            ghi_chu = in.readString();
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(ten_nguyen_lieu);
            dest.writeDouble(so_luong);
            dest.writeString(don_vi);
            dest.writeString(ghi_chu);
        }
        @Override
        public int describeContents() { return 0; }
        public static final Creator<ChiTietNguyenLieu> CREATOR = new Creator<ChiTietNguyenLieu>() {
            @Override
            public ChiTietNguyenLieu createFromParcel(Parcel in) { return new ChiTietNguyenLieu(in); }
            @Override
            public ChiTietNguyenLieu[] newArray(int size) { return new ChiTietNguyenLieu[size]; }
        };
    }

    // Getters và Setters
    public int getMatch_score() { return match_score; }
    public void setMatch_score(int match_score) { this.match_score = match_score; }

    public double getGia_tien() { return gia_tien; }
    public void setGia_tien(double gia_tien) { this.gia_tien = gia_tien; }

    public int getKhau_phan() { return khau_phan; }
    public void setKhau_phan(int khau_phan) { this.khau_phan = khau_phan; }

    public String getVung_mien() { return vung_mien; }
    public void setVung_mien(String vung_mien) { this.vung_mien = vung_mien; }

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
    public Timestamp getNgay_cap_nhat() { return ngay_cap_nhat; }
    public void setNgay_cap_nhat(Timestamp ngay_cap_nhat) { this.ngay_cap_nhat = ngay_cap_nhat; }
    public String getTrang_thai() { return trang_thai; }
    public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }
    public int getLuot_xem() { return luot_xem; }
    public void setLuot_xem(int luot_xem) { this.luot_xem = luot_xem; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getTong_luot_danh_gia() { return tong_luot_danh_gia; }
    public void setTong_luot_danh_gia(int tong_luot_danh_gia) { this.tong_luot_danh_gia = tong_luot_danh_gia; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
    public List<BuocNau> getDanh_sach_buoc_nau() { return danh_sach_buoc_nau; }
    public void setDanh_sach_buoc_nau(List<BuocNau> danh_sach_buoc_nau) { this.danh_sach_buoc_nau = danh_sach_buoc_nau; }
    public List<ChiTietNguyenLieu> getDanh_sach_nguyen_lieu() { return danh_sach_nguyen_lieu; }
    public void setDanh_sach_nguyen_lieu(List<ChiTietNguyenLieu> danh_sach_nguyen_lieu) { this.danh_sach_nguyen_lieu = danh_sach_nguyen_lieu; }
    public List<SoChe> getDanh_sach_so_che() { return danh_sach_so_che; }
    public void setDanh_sach_so_che(List<SoChe> danh_sach_so_che) { this.danh_sach_so_che = danh_sach_so_che; }
    public List<String> getTu_khoa_tim_kiem() { return tu_khoa_tim_kiem; }
    public void setTu_khoa_tim_kiem(List<String> tu_khoa_tim_kiem) { this.tu_khoa_tim_kiem = tu_khoa_tim_kiem; }
    public int getTong_luot_danh_gia() { return reviewCount; }
}

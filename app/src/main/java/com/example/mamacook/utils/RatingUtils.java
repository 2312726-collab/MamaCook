package com.example.mamacook.utils;

import com.example.mamacook.models.MonAn;
import java.util.Locale;

public class RatingUtils {
    /**
     * Trả về chuỗi hiển thị Rating chuẩn 1 chữ số thập phân, dấu phẩy Việt Nam.
     * Ví dụ: "4,8 (140)" hoặc "5,0 (0)"
     */
    public static String getRatingString(MonAn monAn) {
        if (monAn == null) return "5,0 (0)";
        
        double rating = monAn.getReviewCount() > 0 ? monAn.getRating() : 5.0;
        int count = monAn.getReviewCount();
        
        // Làm tròn về 1 chữ số thập phân cho chắc chắn (dù format đã làm)
        double roundedRating = Math.round(rating * 10.0) / 10.0;
        
        return String.format(new Locale("vi", "VN"), "%.1f (%d)", roundedRating, count);
    }

    /**
     * Chỉ trả về số rating (Ví dụ: "4,5")
     */
    public static String getRatingOnly(MonAn monAn) {
        if (monAn == null) return "5,0";
        double rating = monAn.getReviewCount() > 0 ? monAn.getRating() : 5.0;
        double roundedRating = Math.round(rating * 10.0) / 10.0;
        return String.format(new Locale("vi", "VN"), "%.1f", roundedRating);
    }
}

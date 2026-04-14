package com.example.mamacook.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class VNCharacterUtils {

    /**
     * Loại bỏ dấu tiếng Việt khỏi chuỗi.
     */
    public static String removeAccents(String value) {
        if (value == null) return "";
        String temp = Normalizer.normalize(value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
        return result.toLowerCase().trim();
    }

    /**
     * PHƯƠNG THỨC TÁCH CHUỖI TỪ KHÓA TÌM KIẾM (DÙNG CHO ADMIN KHI LƯU MÓN ĂN).
     * Hàm này sẽ tự động tạo danh sách các từ khóa cả có dấu và không dấu từ tên món ăn và nguyên liệu.
     * 
     * @param tenMon Tên của món ăn.
     * @param danhSachNguyenLieu Danh sách tên các nguyên liệu (có thể truyền null nếu không cần).
     * @return Danh sách các từ khóa duy nhất đã được xử lý.
     */
    public static List<String> generateKeywords(String tenMon, List<String> danhSachNguyenLieu) {
        Set<String> keywordSet = new HashSet<>();

        // 1. Xử lý tên món ăn
        if (tenMon != null && !tenMon.isEmpty()) {
            addWordsToSet(keywordSet, tenMon);
        }

        // 2. Xử lý danh sách nguyên liệu
        if (danhSachNguyenLieu != null) {
            for (String nguyenLieu : danhSachNguyenLieu) {
                addWordsToSet(keywordSet, nguyenLieu);
            }
        }

        return new ArrayList<>(keywordSet);
    }

    /**
     * Hàm phụ trợ để tách từ và thêm vào Set (tránh trùng lặp).
     */
    private static void addWordsToSet(Set<String> set, String input) {
        String cleanInput = input.toLowerCase(Locale.getDefault());
        String noToneInput = removeAccents(cleanInput);

        // Tách từ theo khoảng trắng và các ký tự đặc biệt
        String[] wordsWithTone = cleanInput.split("[\\s\\p{Punct}]+");
        String[] wordsNoTone = noToneInput.split("[\\s\\p{Punct}]+");

        set.addAll(Arrays.asList(wordsWithTone));
        set.addAll(Arrays.asList(wordsNoTone));
    }
}

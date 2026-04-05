package com.example.mamacook;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class VNCharacterUtils {
    public static String removeAccents(String value) {
        if (value == null) return "";
        String temp = Normalizer.normalize(value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
        return result.toLowerCase().trim();
    }
}

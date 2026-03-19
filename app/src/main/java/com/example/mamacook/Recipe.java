package com.example.mamacook;

import java.util.List;

public class Recipe {
    private String title;
    private String description;
    private List<String> ingredients; // Danh sách nguyên liệu
    private String instructions;      // Các bước thực hiện
    private int cookingTime;          // Thời gian nấu (phút)
    private String imageUrl;          // Đường dẫn ảnh (nếu có)
    private String userId;            // ID của người tạo công thức này

    public Recipe() {} // Cần thiết cho Firebase

    public Recipe(String title, String description, List<String> ingredients, String instructions, int cookingTime, String userId) {
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.cookingTime = cookingTime;
        this.userId = userId;
    }

    // Getters và Setters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    public int getCookingTime() { return cookingTime; }
    public String getUserId() { return userId; }
}

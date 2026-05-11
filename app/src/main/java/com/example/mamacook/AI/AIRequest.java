package com.example.mamacook.AI;

import java.util.List;

public class AIRequest {

    private String tenMon;
    private double rating;
    private List<String> comments;

    public AIRequest(String tenMon, double rating, List<String> comments) {
        this.tenMon = tenMon;
        this.rating = rating;
        this.comments = comments;
    }

    public String getTenMon() {
        return tenMon;
    }

    public double getRating() {
        return rating;
    }

    public List<String> getComments() {
        return comments;
    }
}
package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.AI.AIRequest;
import com.example.mamacook.AI.AIResponse;
import com.example.mamacook.AI.ApiService;
import com.example.mamacook.AI.RetrofitClient;
import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuanLyMonAnActivity extends AppCompatActivity {

    private RecyclerView rvMonAnAdmin;
    private Button btnTatCaMon, btnMonDanhGiaThap;

    private FirebaseFirestore db;
    private MonAnVerticalAdapter adapter;

    private final List<MonAn> fullList = new ArrayList<>();
    private final List<MonAn> filteredList = new ArrayList<>();

    private boolean dangLocDanhGiaThap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_mon_an);

        db = FirebaseFirestore.getInstance();

        rvMonAnAdmin = findViewById(R.id.rv_mon_an_admin);
        btnTatCaMon = findViewById(R.id.btn_tat_ca_mon);
        btnMonDanhGiaThap = findViewById(R.id.btn_mon_danh_gia_thap);

        adapter = new MonAnVerticalAdapter(filteredList);

        adapter.setOnItemLongClickListener(monAn -> {
            phanTichAI(monAn);
        });

        rvMonAnAdmin.setLayoutManager(new LinearLayoutManager(this));
        rvMonAnAdmin.setAdapter(adapter);

        btnTatCaMon.setOnClickListener(v -> {
            dangLocDanhGiaThap = false;
            locMonAn();
        });

        btnMonDanhGiaThap.setOnClickListener(v -> {
            dangLocDanhGiaThap = true;
            locMonAn();
        });

        loadMonAn();
    }

    private void loadMonAn() {
        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query -> {
                    fullList.clear();
                    filteredList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        MonAn monAn = doc.toObject(MonAn.class);

                        if (monAn.getId_mon_an() == null || monAn.getId_mon_an().isEmpty()) {
                            monAn.setId_mon_an(doc.getId());
                        }

                        fullList.add(monAn);
                    }

                    locMonAn();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Lỗi tải món ăn: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void locMonAn() {
        filteredList.clear();

        for (MonAn monAn : fullList) {
            if (!dangLocDanhGiaThap) {
                filteredList.add(monAn);
            } else {
                if (monAn.getTong_luot_danh_gia() > 0 && monAn.getRating() <= 3.5) {
                    filteredList.add(monAn);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (dangLocDanhGiaThap && filteredList.isEmpty()) {
            Toast.makeText(
                    this,
                    "Chưa có món nào bị đánh giá thấp",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void phanTichAI(MonAn monAn) {

        AlertDialog loading = new AlertDialog.Builder(this)
                .setTitle("AI đang phân tích...")
                .setMessage("Vui lòng chờ vài giây")
                .setCancelable(false)
                .create();

        loading.show();

        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", monAn.getId_mon_an())
                .get()
                .addOnSuccessListener(query -> {

                    List<String> comments = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String noiDung = doc.getString("noi_dung");
                        Double soSao = doc.getDouble("so_sao");

                        if (noiDung != null && !noiDung.trim().isEmpty()) {
                            if (soSao == null || soSao <= 3.0) {
                                comments.add(noiDung);
                            }
                        }
                    }

                    if (comments.isEmpty()) {
                        comments = Arrays.asList(
                                "Món ăn có điểm đánh giá thấp nhưng chưa có nhiều bình luận chi tiết.",
                                "Admin cần kiểm tra lại công thức, hình ảnh và hướng dẫn nấu."
                        );
                    }

                    goiGeminiAI(monAn, comments, loading);
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();

                    Toast.makeText(
                            QuanLyMonAnActivity.this,
                            "Lỗi tải bình luận: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void goiGeminiAI(MonAn monAn, List<String> comments, AlertDialog loading) {

        ApiService apiService = RetrofitClient
                .getClient()
                .create(ApiService.class);

        AIRequest request = new AIRequest(
                monAn.getTen_mon(),
                monAn.getRating(),
                comments
        );

        apiService.analyzeRecipe(request)
                .enqueue(new Callback<AIResponse>() {

                    @Override
                    public void onResponse(Call<AIResponse> call,
                                           Response<AIResponse> response) {
                        loading.dismiss();

                        if (response.isSuccessful() && response.body() != null) {

                            String ketQua = response.body().getResult();

                            new AlertDialog.Builder(QuanLyMonAnActivity.this)
                                    .setTitle("🤖 Phân tích AI")
                                    .setMessage(ketQua)
                                    .setPositiveButton("OK", null)
                                    .show();

                        } else if (response.code() == 429) {
                            // ✅ Xử lý riêng 429
                            new AlertDialog.Builder(QuanLyMonAnActivity.this)
                                    .setTitle("AI đang bận")
                                    .setMessage("Hệ thống AI đang xử lý quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.")
                                    .setPositiveButton("OK", null)
                                    .show();

                        } else {
                            // ✅ Log rõ lỗi để debug
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception ignored) {
                            }

                            Toast.makeText(
                                    QuanLyMonAnActivity.this,
                                    "AI lỗi " + response.code() + ": " + errorBody,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AIResponse> call, Throwable t) {
                        loading.dismiss();

                        // ✅ Phân biệt timeout vs lỗi mạng
                        String msg;
                        if (t instanceof java.net.SocketTimeoutException) {
                            msg = "AI xử lý quá lâu, vui lòng thử lại.";
                        } else if (t instanceof java.net.ConnectException) {
                            msg = "Không kết nối được server AI. Hãy kiểm tra server có đang chạy không.";
                        } else {
                            msg = "Lỗi AI: " + t.getMessage();
                        }

                        new AlertDialog.Builder(QuanLyMonAnActivity.this)
                                .setTitle("Lỗi")
                                .setMessage(msg)
                                .setPositiveButton("Thử lại", (d, w) -> goiGeminiAI(monAn, comments, loading))
                                .setNegativeButton("Bỏ qua", null)
                                .show();

                        // Hiện lại loading nếu thử lại
                        loading.show();
                    }
                });
    }
}
package com.example.mamacook.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.adapters.BinhLuanNgangAdapter;
import com.example.mamacook.adapters.BuocNauAdapter;
import com.example.mamacook.adapters.NguyenLieuAdapter;
import com.example.mamacook.models.DanhGia;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.utils.RatingUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private static final String TAG = "DetailMonAnActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    public static final String EXTRA_ID           = "ID_MON_AN";
    public static final String EXTRA_HINH_ANH     = "HINH_ANH";
    public static final String EXTRA_TEN_MON      = "TEN_MON";
    public static final String EXTRA_THOI_GIAN    = "THOI_GIAN";
    public static final String EXTRA_RATING       = "RATING";
    public static final String EXTRA_REVIEW_COUNT = "REVIEW_COUNT";
    public static final String EXTRA_NGUYEN_LIEU  = "NGUYEN_LIEU";

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnAddAttachment, imgPreviewComment;
    private ImageView btnEditMonAn, btnDeleteMonAn;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnFavoriteDetail, btnAddToPlan, btnQrCode;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvDiemTrungBinh, tvXemTatCa;
    private RelativeLayout layoutPreviewImage;
    private LinearLayout layoutInputComment;
    private RecyclerView rvDanhGia;
    private BinhLuanNgangAdapter adapterBinhLuan;
    private final List<DanhGia> danhSachBinhLuan = new ArrayList<>();
    private EditText etBinhLuan;
    private RatingBar rbChonSao;
    private ImageView btnGuiBinhLuan;
    private String currentDishId;
    private String currentUserId;
    private boolean isSaved = false;
    private boolean isInPlan = false;
    private MonAn currentMonAn;
    private String preSelectedDate, preSelectedMeal;
    private ListenerRegistration monAnListener;

    private Uri imageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ProgressDialog progressDialog;
    private RecyclerView rvNguyenLieu, rvBuocNau;
    private NguyenLieuAdapter nguyenLieuAdapter;
    private BuocNauAdapter buocNauAdapter;
    private TextView tvDoKho, tvKhauPhan, tvRegion, tvSoDanhGia, tvChuanBi, btnXemThemNguyenLieu;
    private List<MonAn.ChiTietNguyenLieu> fullNguyenLieuList = new ArrayList<>();
    private boolean isShowingAllNguyenLieu = false;

    public static Intent createIntent(Context context, MonAn monAn) {
        Intent intent = new Intent(context, DetailMonAnActivity.class);
        intent.putExtra(EXTRA_ID,           monAn.getId_mon_an());
        intent.putExtra(EXTRA_HINH_ANH,     monAn.getHinh_anh());
        intent.putExtra(EXTRA_TEN_MON,      monAn.getTen_mon());
        intent.putExtra(EXTRA_THOI_GIAN,    monAn.getThoi_gian_nau());
        intent.putExtra(EXTRA_RATING,       RatingUtils.getRatingOnly(monAn));
        intent.putExtra(EXTRA_REVIEW_COUNT, monAn.getReviewCount());
        return intent;
    }

    public static Intent createIntent(Context context, String dishId) {
        Intent intent = new Intent(context, DetailMonAnActivity.class);
        intent.putExtra(EXTRA_ID, dishId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_mon_an);

        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupScrollBehavior();
        setupClickListeners();
        initImageLaunchers();

        checkUserRole();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        currentDishId = getIntent().getStringExtra(EXTRA_ID);
        preSelectedDate = getIntent().getStringExtra("PRE_SELECTED_DATE");
        preSelectedMeal = getIntent().getStringExtra("PRE_SELECTED_MEAL");

        if (TextUtils.isEmpty(currentDishId)) {
            Toast.makeText(this, "Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderFromIntent();
        addToHistory(currentDishId);
        incrementViewCount(currentDishId);
    }

    private void incrementViewCount(String dishId) {
        if (dishId == null) return;
        db.collection("mon_an").document(dishId)
                .update("luot_xem", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật lượt xem: " + e.getMessage()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentDishId != null) {
            startListeningMonAnDetail(currentDishId);
            fetchLatestReviews(currentDishId);
            checkIfSaved();
            checkIfInPlan();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (monAnListener != null) {
            monAnListener.remove();
            monAnListener = null;
        }
    }

    private void initViews() {
        imgMonAn           = findViewById(R.id.img_detail_mon_an);
        tvTen              = findViewById(R.id.tv_detail_ten);
        tvRatingInfo       = findViewById(R.id.tv_detail_rating_info);
        tvThoiGian         = findViewById(R.id.tv_detail_thoi_gian);
        tvDiemTrungBinh    = findViewById(R.id.tv_detail_diem_trung_binh);
        tvXemTatCa         = findViewById(R.id.tv_detail_xem_tat_ca);
        rvDanhGia          = findViewById(R.id.rv_detail_danh_gia);
        etBinhLuan         = findViewById(R.id.et_detail_binh_luan);
        rbChonSao          = findViewById(R.id.rb_detail_chon_sao);
        btnGuiBinhLuan     = findViewById(R.id.btn_detail_gui_binh_luan);
        btnFavoriteDetail  = findViewById(R.id.btn_favorite_detail);
        btnAddToPlan       = findViewById(R.id.btn_add_to_plan);
        btnAddAttachment   = findViewById(R.id.btn_detail_add_attachment);
        imgPreviewComment  = findViewById(R.id.img_detail_preview_comment);
        layoutPreviewImage = findViewById(R.id.layout_detail_preview_image);
        btnQrCode          = findViewById(R.id.btn_qr_code);
        layoutInputComment = findViewById(R.id.layout_detail_input_comment);
        rvNguyenLieu = findViewById(R.id.rv_detail_nguyen_lieu);
        rvBuocNau = findViewById(R.id.rv_detail_buoc_nau);
        tvDoKho = findViewById(R.id.tv_detail_do_kho);
        tvKhauPhan = findViewById(R.id.tv_detail_khau_phan);
        tvRegion = findViewById(R.id.tv_detail_region);
        tvSoDanhGia = findViewById(R.id.tv_detail_so_danh_gia);
        tvChuanBi = findViewById(R.id.tv_detail_chuan_bi);
        btnXemThemNguyenLieu = findViewById(R.id.btn_detail_xem_them_nguyen_lieu);

        btnEditMonAn = findViewById(R.id.btn_edit_mon_an);
        btnDeleteMonAn = findViewById(R.id.btn_delete_mon_an);
    }

    private void checkUserRole() {
        // Mặc định cho User
        if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.VISIBLE);
        if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.VISIBLE);
        if (btnEditMonAn != null) btnEditMonAn.setVisibility(View.GONE);
        if (btnDeleteMonAn != null) btnDeleteMonAn.setVisibility(View.GONE);

        if (currentUserId == null) return;

        db.collection("nguoi_dung").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        if (role != null && "admin".equalsIgnoreCase(role.trim())) {
                            // Hiện nút Admin
                            if (btnEditMonAn != null) btnEditMonAn.setVisibility(View.VISIBLE);
                            if (btnDeleteMonAn != null) btnDeleteMonAn.setVisibility(View.VISIBLE);
                            // Ẩn nút User
                            if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.GONE);
                            if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi kiểm tra quyền: " + e.getMessage()));
    }

    private void renderFromIntent() {
        Intent i = getIntent();
        loadDishImage(i.getStringExtra(EXTRA_HINH_ANH));
        tvTen.setText(i.getStringExtra(EXTRA_TEN_MON));
        int thoiGian = i.getIntExtra(EXTRA_THOI_GIAN, 0);
        tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", thoiGian));
        updateRatingUI(i.getStringExtra(EXTRA_RATING), i.getIntExtra(EXTRA_REVIEW_COUNT, 0));
    }

    private void loadDishImage(String hinhAnh) {
        if (TextUtils.isEmpty(hinhAnh)) return;
        if (hinhAnh.startsWith("http")) {
            Glide.with(this).load(hinhAnh).placeholder(R.drawable.bg_splash).into(imgMonAn);
        } else {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(hinhAnh);
            Glide.with(this).load(ref).placeholder(R.drawable.bg_splash).into(imgMonAn);
        }
    }

    private void updateRatingUI(String ratingParam, int count) {
        String rating = (ratingParam == null) ? "0.0" : ratingParam;
        if (tvRatingInfo != null) tvRatingInfo.setText(String.format(new Locale("vi", "VN"), "🕒 %s ⭐ (%d)", rating, count));
        if (tvDiemTrungBinh != null) tvDiemTrungBinh.setText("⭐ " + rating);
        if (tvSoDanhGia != null) tvSoDanhGia.setText(String.format(new Locale("vi", "VN"), "(%d đánh giá)", count));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        rvNguyenLieu.setLayoutManager(new GridLayoutManager(this, 2));
        nguyenLieuAdapter = new NguyenLieuAdapter(new ArrayList<>());
        rvNguyenLieu.setAdapter(nguyenLieuAdapter);

        rvBuocNau.setLayoutManager(new LinearLayoutManager(this));
        buocNauAdapter = new BuocNauAdapter(new ArrayList<>());
        rvBuocNau.setAdapter(buocNauAdapter);

        rvDanhGia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterBinhLuan = new BinhLuanNgangAdapter(danhSachBinhLuan);
        rvDanhGia.setAdapter(adapterBinhLuan);
    }

    private void setupScrollBehavior() {
        NestedScrollView scrollDetail = findViewById(R.id.scroll_detail);
        if (layoutInputComment != null) {
            layoutInputComment.setOnClickListener(v -> {
                etBinhLuan.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etBinhLuan, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        }
        etBinhLuan.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etBinhLuan.postDelayed(() -> scrollDetail.fullScroll(View.FOCUS_DOWN), 400);
        });
    }

    private void setupClickListeners() {
        if (btnAddAttachment != null) btnAddAttachment.setOnClickListener(v -> showAttachmentMenu());
        if (btnFavoriteDetail != null) btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        if (btnAddToPlan != null) btnAddToPlan.setOnClickListener(v -> toggleCookingPlan());
        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());
        if (btnQrCode != null) btnQrCode.setOnClickListener(v -> openQRCode());
        if (btnQrCode != null) btnQrCode.setOnClickListener(v -> openQRCode());
        if (tvXemTatCa != null) tvXemTatCa.setOnClickListener(v -> {
            Intent intent = new Intent(this, TatCaBinhLuanActivity.class);
            intent.putExtra(EXTRA_ID, currentDishId);
            startActivity(intent);
        });
        findViewById(R.id.btn_detail_remove_preview).setOnClickListener(v -> {
            imageUri = null;
            layoutPreviewImage.setVisibility(View.GONE);
        });
        if (btnXemThemNguyenLieu != null) btnXemThemNguyenLieu.setOnClickListener(v -> toggleNguyenLieu());

        if (btnEditMonAn != null) {
            btnEditMonAn.setOnClickListener(v -> {
                if (currentDishId != null) {
                    Intent intent = AddEditMonAnActivity.createIntent(DetailMonAnActivity.this, currentDishId);
                    startActivity(intent);
                }
            });
        }

        if (btnDeleteMonAn != null) {
            btnDeleteMonAn.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa món ăn này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteMonAn())
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }
    }

    private void openQRCode() {
        if (currentDishId == null) {
            Toast.makeText(this, "Không có ID món ăn", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, QRCodeViewerActivity.class);
        intent.putExtra("ID_MON_AN", currentDishId);
        startActivity(intent);
    }

    private void deleteMonAn() {
        if (currentDishId == null) return;
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Đang xóa...");
        pd.setCancelable(false);
        pd.show();

        // Lấy thông tin mới nhất từ Firestore trước khi xóa để đảm bảo có đường dẫn ảnh chính xác
        db.collection("mon_an").document(currentDishId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String hinhAnh = doc.getString("hinh_anh");
                
                // 1. Xóa hình ảnh trong Storage nếu có
                if (!TextUtils.isEmpty(hinhAnh)) {
                    try {
                        StorageReference storageRef;
                        if (hinhAnh.startsWith("http")) {
                            // Nếu là URL (thường là download URL của Firebase)
                            storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(hinhAnh);
                        } else {
                            // Nếu là path (ví dụ: mon_an/abc.jpg)
                            storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                        }
                        
                        storageRef.delete().addOnFailureListener(e -> Log.e(TAG, "Lỗi xóa ảnh Storage: " + e.getMessage()));
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý xóa ảnh: " + e.getMessage());
                    }
                }
            }

            // 2. Xóa tài liệu trong Firestore (Luôn xóa tài liệu kể cả khi không có ảnh hoặc lỗi xóa ảnh)
            db.collection("mon_an").document(currentDishId).delete()
                    .addOnSuccessListener(aVoid -> {
                        pd.dismiss();
                        Toast.makeText(this, "Đã xóa món ăn thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(this, "Lỗi xóa dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(this, "Không thể truy cập dữ liệu để xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Cần cấp quyền để sử dụng camera và thư viện ảnh", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initImageLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
                showPreview();
            }
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (Boolean.TRUE.equals(result)) showPreview();
        });
    }

    private void showPreview() {
        if (imageUri != null) {
            Glide.with(this).load(imageUri).into(imgPreviewComment);
            layoutPreviewImage.setVisibility(View.VISIBLE);
        }
    }

    private void showAttachmentMenu() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attachment, null);
        view.findViewById(R.id.btnChupHinh).setOnClickListener(v -> {
            dialog.dismiss();
            try {
                File photoFile = createImageFile();
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(imageUri);
            } catch (IOException ex) { ex.printStackTrace(); }
        });
        view.findViewById(R.id.btnChonAnh).setOnClickListener(v -> {
            dialog.dismiss();
            galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private File createImageFile() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile("JPEG_" + ts + "_", ".jpg", getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
    }

    private void fetchLatestReviews(String dishId) {
        db.collection("danh_gia").whereEqualTo("id_mon_an", dishId).whereEqualTo("trang_thai", "hien_thi")
                .orderBy("ngay_danh_gia", Query.Direction.DESCENDING).limit(5)
                .addSnapshotListener(this, (value, error) -> {
                    if (value != null) {
                        danhSachBinhLuan.clear();
                        for (QueryDocumentSnapshot doc : value) danhSachBinhLuan.add(doc.toObject(DanhGia.class));
                        adapterBinhLuan.notifyDataSetChanged();
                    }
                });
    }

    private void guiBinhLuan() {
        if (currentUserId == null) { Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show(); return; }
        String noiDung = etBinhLuan.getText().toString().trim();
        float sao = rbChonSao.getRating();
        if (TextUtils.isEmpty(noiDung) || sao == 0) return;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi...");
        progressDialog.show();
        if (imageUri != null) compressAndUploadImage(imageUri, noiDung, sao);
        else saveReviewToFirestore(noiDung, sao, null);
    }

    private void compressAndUploadImage(Uri uri, String noiDung, float sao) {
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();
                runOnUiThread(() -> uploadBytesToFirebase(data, noiDung, sao));
            } catch (Exception e) { runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(this, "Lỗi nén ảnh", Toast.LENGTH_SHORT).show(); }); }
        }).start();
    }

    private void uploadBytesToFirebase(byte[] data, String noiDung, float sao) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("reviews/" + System.currentTimeMillis() + ".jpg");
        ref.putBytes(data).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> saveReviewToFirestore(noiDung, sao, uri.toString())));
    }

    private void saveReviewToFirestore(String content, float stars, String imageUrl) {
        // Lấy tên người dùng từ Firestore
        db.collection("nguoi_dung").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    String tenNguoiDung = "Ẩn danh";
                    if (userDoc.exists()) {
                        tenNguoiDung = userDoc.getString("ho_ten");
                        if (tenNguoiDung == null || tenNguoiDung.isEmpty()) {
                            tenNguoiDung = "Ẩn danh";
                        }
                    }

                    Map<String, Object> review = new HashMap<>();
                    review.put("id_nguoi_dung", currentUserId);
                    review.put("ten_nguoi_dung", tenNguoiDung);
                    review.put("id_mon_an", currentDishId);
                    review.put("noi_dung", content);
                    review.put("so_sao", stars);
                    review.put("hinh_anh_url", imageUrl);
                    review.put("trang_thai", "cho_duyet");
                    review.put("ngay_danh_gia", FieldValue.serverTimestamp());

                    db.collection("danh_gia").add(review).addOnSuccessListener(docRef -> {
                        // Cập nhật số sao và lượt đánh giá vào mon_an ngay lập tức
                        updateMonAnRating(stars);

                        progressDialog.dismiss();
                        etBinhLuan.setText("");
                        rbChonSao.setRating(5);
                        imageUri = null;
                        layoutPreviewImage.setVisibility(View.GONE);
                        Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMonAnRating(float newStar) {
        if (currentDishId == null || currentMonAn == null) return;

        DocumentReference dishRef = db.collection("mon_an").document(currentDishId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(dishRef);
            
            double currentRating = 0;
            if (snapshot.contains("rating")) {
                Object r = snapshot.get("rating");
                if (r instanceof Number) currentRating = ((Number) r).doubleValue();
            }
            
            long currentCount = 0;
            if (snapshot.contains("reviewCount")) {
                Long c = snapshot.getLong("reviewCount");
                if (c != null) currentCount = c;
            }

            double newTotalRating = (currentRating * currentCount) + newStar;
            long newCount = currentCount + 1;
            double newAverage = newTotalRating / newCount;

            // Làm tròn 1 chữ số thập phân
            newAverage = Math.round(newAverage * 10.0) / 10.0;

            transaction.update(dishRef, "rating", newAverage);
            transaction.update(dishRef, "reviewCount", newCount);

            return null;
        }).addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật rating: " + e.getMessage()));
    }

    private void checkIfInPlan() {
        if (currentUserId == null) return;
        db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("id_mon_an", currentDishId)
                .addSnapshotListener(this, (value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (value != null) {
                        isInPlan = !value.isEmpty();
                        updatePlanButtonUI();
                    }
                });
    }

    private void updatePlanButtonUI() {
        if (btnAddToPlan == null) return;
        if (isInPlan) {
            btnAddToPlan.setBackgroundResource(R.drawable.bg_header_button_plan_selected);
            btnAddToPlan.setColorFilter(Color.parseColor("#FF6600"));
        } else {
            btnAddToPlan.setBackgroundResource(R.drawable.bg_header_button_glass);
            btnAddToPlan.setColorFilter(Color.WHITE);
        }
    }

    private void toggleCookingPlan() {
        if (currentUserId == null) { Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show(); return; }
        if (currentMonAn == null) return;
        if (preSelectedDate != null && preSelectedMeal != null) {
            saveToCookingPlan(preSelectedDate, preSelectedMeal);
            return;
        }

        List<String> dateList = new ArrayList<>();
        List<String> dateDisplayList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) cal.add(Calendar.DAY_OF_YEAR, -6);

        SimpleDateFormat sdfDisplay = new SimpleDateFormat("EEEE (dd/MM)", new Locale("vi", "VN"));
        SimpleDateFormat sdfDatabase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            dateList.add(sdfDatabase.format(cal.getTime()));
            dateDisplayList.add(sdfDisplay.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn ngày nấu")
                .setItems(dateDisplayList.toArray(new String[0]), (dialog, which) -> {
                    String selectedDate = dateList.get(which);
                    showMealTypeDialog(selectedDate);
                })
                .show();
    }

    private void showMealTypeDialog(String selectedDate) {
        String[] types = {"Sáng", "Trưa", "Tối"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn buổi nấu ăn")
                .setItems(types, (dialog, which) -> {
                    String mealType = (which == 0) ? "Sang" : (which == 1) ? "Trua" : "Toi";
                    saveToCookingPlan(selectedDate, mealType);
                })
                .show();
    }

    private void saveToCookingPlan(String selectedDate, String mealType) {
        String buoiTiengViet = mealType.equals("Sang") ? "Bữa Sáng" : mealType.equals("Trua") ? "Bữa Trưa" : "Bữa Tối";
        String displayDate = "";
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dbFormat.parse(selectedDate);
            if (date != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE (dd/MM)", new Locale("vi", "VN"));
                displayDate = displayFormat.format(date);
            }
        } catch (Exception e) { 
            displayDate = selectedDate; 
        }

        String finalDisplayDate = displayDate;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thêm món")
                .setMessage("Bạn có chắc muốn thêm '" + currentMonAn.getTen_mon() + "' vào " + buoiTiengViet + " " + finalDisplayDate + "?")
                .setPositiveButton("Thêm", (dialog, which) -> checkDuplicateAndSave(selectedDate, mealType, finalDisplayDate))
                .setNegativeButton("Hủy", null).show();
    }

    private void checkDuplicateAndSave(String selectedDate, String mealType, String thuMay) {
        db.collection("ke_hoach_nau_an").whereEqualTo("id_nguoi_dung", currentUserId).whereEqualTo("id_mon_an", currentDishId).whereEqualTo("ngay_chi_tiet", selectedDate).whereEqualTo("buoi", mealType).get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) Toast.makeText(this, "Món ăn đã có trong kế hoạch!", Toast.LENGTH_SHORT).show();
                    else performSavePlan(selectedDate, mealType, thuMay);
                });
    }

    private void performSavePlan(String selectedDate, String mealType, String thuMay) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id_nguoi_dung", currentUserId);
        plan.put("id_mon_an", currentDishId);
        plan.put("ten_mon", currentMonAn.getTen_mon());
        plan.put("hinh_anh", currentMonAn.getHinh_anh());
        plan.put("ngay_lap_ke_hoach", FieldValue.serverTimestamp());
        plan.put("ngay_chi_tiet", selectedDate);
        plan.put("trang_thai", "dang_di_cho");
        plan.put("buoi", mealType);

        List<Map<String, Object>> listNL = new ArrayList<>();
        if (currentMonAn.getDanh_sach_nguyen_lieu() != null) {
            for (MonAn.ChiTietNguyenLieu nl : currentMonAn.getDanh_sach_nguyen_lieu()) {
                Map<String, Object> item = new HashMap<>();
                item.put("ten_nguyen_lieu", nl.ten_nguyen_lieu);
                item.put("so_luong", nl.so_luong);
                item.put("don_vi", nl.don_vi);
                item.put("da_mua", false);
                listNL.add(item);
            }
        }
        plan.put("danh_sach_nguyen_lieu", listNL);

        db.collection("ke_hoach_nau_an").add(plan)
                .addOnSuccessListener(ref -> Toast.makeText(this, "Đã thêm vào kế hoạch!", Toast.LENGTH_SHORT).show());
    }

    private void startListeningMonAnDetail(String id) {
        DocumentReference docRef = db.collection("mon_an").document(id);
        monAnListener = docRef.addSnapshotListener(this, (doc, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            if (doc != null && doc.exists()) {
                currentMonAn = doc.toObject(MonAn.class);
                if (currentMonAn != null) {
                    currentMonAn.setId_mon_an(doc.getId());
                    loadDishImage(currentMonAn.getHinh_anh());
                    updateRatingUI(RatingUtils.getRatingOnly(currentMonAn), currentMonAn.getReviewCount());
                    tvTen.setText(currentMonAn.getTen_mon());
                    tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", currentMonAn.getThoi_gian_nau()));

                    if (currentMonAn.getDanh_sach_nguyen_lieu() != null) {
                        fullNguyenLieuList = currentMonAn.getDanh_sach_nguyen_lieu();
                        updateNguyenLieuDisplay();
                    }

                    if (currentMonAn.getDanh_sach_buoc_nau() != null) {
                        buocNauAdapter = new BuocNauAdapter(currentMonAn.getDanh_sach_buoc_nau());
                        rvBuocNau.setAdapter(buocNauAdapter);
                    }

                    if (currentMonAn.getDanh_sach_so_che() != null && !currentMonAn.getDanh_sach_so_che().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (MonAn.SoChe sc : currentMonAn.getDanh_sach_so_che()) {
                            if (!TextUtils.isEmpty(sc.tieu_de)) {
                                sb.append("• ").append(sc.tieu_de).append(": ");
                            }
                            sb.append(sc.noi_dung).append("\n");
                        }
                        tvChuanBi.setText(sb.toString().trim());
                        tvChuanBi.setVisibility(View.VISIBLE);
                    } else {
                        tvChuanBi.setText("Không có thông tin chuẩn bị/sơ chế cho món này.");
                        tvChuanBi.setVisibility(View.VISIBLE);
                    }

                    if (currentMonAn.getDo_kho() != null) tvDoKho.setText(currentMonAn.getDo_kho());
                    tvKhauPhan.setText(String.format(Locale.getDefault(), "%d người", currentMonAn.getKhau_phan()));
                    if (currentMonAn.getVung_mien() != null) tvRegion.setText(currentMonAn.getVung_mien());
                }
            } else {
                Toast.makeText(this, "Món ăn không tồn tại hoặc đã bị xóa", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void checkIfSaved() {
        if (currentUserId == null) return;
        db.collection("mon_da_luu").document(currentUserId + "_" + currentDishId).addSnapshotListener(this, (doc, error) -> {
            if (doc != null) { isSaved = doc.exists(); updateSaveButtonUI(); }
        });
    }

    private void updateSaveButtonUI() {
        if (isSaved) {
            btnFavoriteDetail.setImageResource(R.drawable.ic_heart_filled);
            btnFavoriteDetail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF1744")));
            btnFavoriteDetail.setColorFilter(Color.WHITE);
        } else {
            btnFavoriteDetail.setImageResource(R.drawable.ic_heart_outline);
            btnFavoriteDetail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            btnFavoriteDetail.setColorFilter(Color.parseColor("#FF5252"));
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) return;
        String idLuu = currentUserId + "_" + currentDishId;

        if (isSaved) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có muốn bỏ yêu thích món ăn này không?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("mon_da_luu").document(idLuu).delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Không", null)
                    .show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void addToHistory(String dishId) {
        if (currentUserId == null) return;
        String historyId = currentUserId + "_" + dishId;
        Map<String, Object> h = new HashMap<>();
        h.put("id_nguoi_dung", currentUserId);
        h.put("id_mon_an", dishId);
        h.put("thoi_gian_xem", FieldValue.serverTimestamp());
        db.collection("lich_su_xem").document(historyId).set(h)
                .addOnSuccessListener(ref -> limitHistoryTo15());
    }

    private void limitHistoryTo15() {
        db.collection("lich_su_xem").whereEqualTo("id_nguoi_dung", currentUserId).orderBy("thoi_gian_xem", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> docs = qs.getDocuments();
                    for (int i = 15; i < docs.size(); i++) db.collection("lich_su_xem").document(docs.get(i).getId()).delete();
                });
    }

    private void updateNguyenLieuDisplay() {
        List<MonAn.ChiTietNguyenLieu> display = (isShowingAllNguyenLieu || fullNguyenLieuList.size() <= 6) ? fullNguyenLieuList : fullNguyenLieuList.subList(0, 6);
        btnXemThemNguyenLieu.setVisibility(fullNguyenLieuList.size() > 6 ? View.VISIBLE : View.GONE);
        btnXemThemNguyenLieu.setText(isShowingAllNguyenLieu ? "- Thu gọn" : "+ Xem thêm");
        rvNguyenLieu.setAdapter(new NguyenLieuAdapter(display));
    }

    private void toggleNguyenLieu() { isShowingAllNguyenLieu = !isShowingAllNguyenLieu; updateNguyenLieuDisplay(); }
}

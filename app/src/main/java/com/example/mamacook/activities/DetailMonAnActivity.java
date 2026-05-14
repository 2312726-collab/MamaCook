package com.example.mamacook.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.adapters.BinhLuanNgangAdapter;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.mamacook.adapters.NguyenLieuAdapter;
import com.example.mamacook.adapters.BuocNauAdapter;

public class DetailMonAnActivity extends AppCompatActivity {

    private static final String TAG = "DetailMonAnActivity";

    // =========================================================================
    // Keys cho Intent extras
    // =========================================================================
    public static final String EXTRA_ID           = "ID_MON_AN";
    public static final String EXTRA_HINH_ANH     = "HINH_ANH";
    public static final String EXTRA_TEN_MON      = "TEN_MON";
    public static final String EXTRA_THOI_GIAN    = "THOI_GIAN";
    public static final String EXTRA_RATING       = "RATING";
    public static final String EXTRA_REVIEW_COUNT = "REVIEW_COUNT";
    public static final String EXTRA_NGUYEN_LIEU  = "NGUYEN_LIEU";

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnFavoriteDetail, btnAddToPlan, btnAddAttachment, imgPreviewComment;
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

    private Uri imageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ProgressDialog progressDialog;
    private RecyclerView rvNguyenLieu, rvBuocNau;
    private NguyenLieuAdapter nguyenLieuAdapter;
    private BuocNauAdapter buocNauAdapter;
    private TextView tvDoKho, tvKhauPhan, tvRegion, tvSoDanhGia, tvChuanBi, btnXemThemNguyenLieu;
    private LinearLayout layoutBuocNauHeader;
    private ImageView ivBuocNauArrow;
    private boolean isBuocNauExpanded = false;
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
        intent.putExtra(EXTRA_NGUYEN_LIEU,  buildNguyenLieuTextStatic(monAn));
        return intent;
    }

    public static String buildNguyenLieuTextStatic(MonAn monAn) {
        if (monAn.getDanh_sach_nguyen_lieu() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (MonAn.ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) {
            sb.append("• ")
                    .append(formatSoLuong(nl.so_luong))
                    .append(" ")
                    .append(nl.don_vi != null ? nl.don_vi.trim() : "")
                    .append(" ")
                    .append(nl.ten_nguyen_lieu)
                    .append("\n");
        }
        return sb.toString().trim();
    }

    public static String formatSoLuong(double soLuong) {
        if (soLuong == (long) soLuong) {
            return String.valueOf((long) soLuong);
        } else {
            return String.valueOf(soLuong);
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        currentDishId = getIntent().getStringExtra(EXTRA_ID);
        if (TextUtils.isEmpty(currentDishId)) {
            Toast.makeText(this, "Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderFromIntent();
        listenRatingRealtime(currentDishId);
        fetchLatestReviews(currentDishId);
        checkIfSaved();
        checkIfInPlan();
        addToHistory(currentDishId);
    }

    // =========================================================================
    // BƯỚC 1 — Render tức thì từ Intent
    // =========================================================================

    private void renderFromIntent() {
        Intent i = getIntent();

        String hinhAnh = i.getStringExtra(EXTRA_HINH_ANH);
        if (!TextUtils.isEmpty(hinhAnh)) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(this)
                        .load(hinhAnh)
                        .placeholder(R.drawable.bg_splash)
                        .into(imgMonAn);
            } else {
                StorageReference ref = FirebaseStorage.getInstance()
                        .getReference()
                        .child(hinhAnh);
                Glide.with(this)
                        .load(ref)
                        .placeholder(R.drawable.bg_splash)
                        .into(imgMonAn);
            }
        }

        String tenMon = i.getStringExtra(EXTRA_TEN_MON);
        if (!TextUtils.isEmpty(tenMon)) {
            tvTen.setText(tenMon);
        }

        int thoiGian = i.getIntExtra(EXTRA_THOI_GIAN, 0);
        if (thoiGian > 0) {
            tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", thoiGian));
        }

        String rating      = i.getStringExtra(EXTRA_RATING);
        int    reviewCount = i.getIntExtra(EXTRA_REVIEW_COUNT, 0);
        if (!TextUtils.isEmpty(rating)) {
            updateRatingUI(rating, reviewCount);
        }
    }

    // =========================================================================
    // Rating UI
    // =========================================================================

    private void updateRatingUI(String rating, int count) {
        tvRatingInfo.setText(
                String.format(new Locale("vi", "VN"), "🕒 %s ⭐ (%d)", rating, count));
        tvDiemTrungBinh.setText("⭐ " + rating);
        if (tvSoDanhGia != null) {
            tvSoDanhGia.setText(String.format(new Locale("vi", "VN"), "(%d đánh giá)", count));
        }
    }

    // =========================================================================
    // Setup UI
    // =========================================================================

    private void initViews() {
        imgMonAn           = findViewById(R.id.img_detail_mon_an);
        tvTen              = findViewById(R.id.tv_detail_ten);
        tvRatingInfo       = findViewById(R.id.tv_detail_rating_info);
        tvThoiGian         = findViewById(R.id.tv_detail_thoi_gian);
        tvDiemTrungBinh    = findViewById(R.id.tvDiemTrungBinh);
        tvXemTatCa         = findViewById(R.id.tvXemTatCa);
        rvDanhGia          = findViewById(R.id.rv_danh_gia);
        etBinhLuan         = findViewById(R.id.et_binh_luan);
        rbChonSao          = findViewById(R.id.rb_chon_sao);
        btnGuiBinhLuan     = findViewById(R.id.btn_gui_binh_luan);
        btnFavoriteDetail  = findViewById(R.id.btn_favorite_detail);
        btnAddToPlan       = findViewById(R.id.btn_add_to_plan);
        btnAddAttachment   = findViewById(R.id.btn_add_attachment);
        imgPreviewComment  = findViewById(R.id.img_preview_comment);
        layoutPreviewImage = findViewById(R.id.layout_preview_image);
        layoutInputComment = findViewById(R.id.layout_input_comment);
        rvNguyenLieu = findViewById(R.id.rv_nguyen_lieu);
        rvBuocNau = findViewById(R.id.rv_buoc_nau);
        tvDoKho = findViewById(R.id.tv_detail_do_kho);
        tvKhauPhan = findViewById(R.id.tv_detail_khau_phan);
        tvRegion = findViewById(R.id.tv_detail_region);
        tvSoDanhGia = findViewById(R.id.tv_so_danh_gia);
        tvChuanBi = findViewById(R.id.tv_detail_chuan_bi);
        btnXemThemNguyenLieu = findViewById(R.id.btn_xem_them_nguyen_lieu);
        layoutBuocNauHeader = findViewById(R.id.layout_buoc_nau_header);
        ivBuocNauArrow = findViewById(R.id.iv_buoc_nau_arrow);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        // Nguyên liệu Grid 2 cột
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvNguyenLieu.setLayoutManager(gridLayoutManager);
        nguyenLieuAdapter = new NguyenLieuAdapter(new ArrayList<>());
        rvNguyenLieu.setAdapter(nguyenLieuAdapter);

        // Bước nấu Linear
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvBuocNau.setLayoutManager(linearLayoutManager);
        buocNauAdapter = new BuocNauAdapter(new ArrayList<>());
        rvBuocNau.setAdapter(buocNauAdapter);

        // Comment Horizontal List
        LinearLayoutManager horizontalLayout = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvDanhGia.setLayoutManager(horizontalLayout);
        adapterBinhLuan = new BinhLuanNgangAdapter(danhSachBinhLuan);
        rvDanhGia.setAdapter(adapterBinhLuan);
    }

    private void setupScrollBehavior() {
        NestedScrollView scrollDetail = findViewById(R.id.scroll_detail);
        if (scrollDetail == null || etBinhLuan == null) {
            return;
        }

        // Khi khung bao quanh được click -> focus vào ô nhập
        if (layoutInputComment != null) {
            layoutInputComment.setOnClickListener(v -> {
                etBinhLuan.requestFocus();
                // Hiển thị bàn phím thủ công nếu cần
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etBinhLuan, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        }

        etBinhLuan.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etBinhLuan.postDelayed(
                        () -> scrollDetail.fullScroll(View.FOCUS_DOWN), 400);
            }
        });

        scrollDetail.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (bottom < oldBottom) {
                        scrollDetail.postDelayed(
                                () -> scrollDetail.fullScroll(View.FOCUS_DOWN), 100);
                    }
                });
    }

    private void setupClickListeners() {
        if (btnAddAttachment != null) {
            btnAddAttachment.setOnClickListener(v -> showAttachmentMenu());
        }

        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }
        if (btnAddToPlan != null) {
            btnAddToPlan.setOnClickListener(v -> toggleCookingPlan());
        }

        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());

        if (tvXemTatCa != null) {
            tvXemTatCa.setOnClickListener(v -> {
                Intent intent = new Intent(this, TatCaBinhLuanActivity.class);
                intent.putExtra(EXTRA_ID, currentDishId);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        View btnRemovePreview = findViewById(R.id.btn_remove_preview);
        if (btnRemovePreview != null) {
            btnRemovePreview.setOnClickListener(v -> {
                imageUri = null;
                layoutPreviewImage.setVisibility(View.GONE);
            });
        }

        // Xem thêm nguyên liệu
        if (btnXemThemNguyenLieu != null) {
            btnXemThemNguyenLieu.setOnClickListener(v -> toggleNguyenLieu());
        }

        // Toggle các bước nấu
        if (layoutBuocNauHeader != null) {
            layoutBuocNauHeader.setOnClickListener(v -> toggleBuocNau());
        }
    }

    private void initImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        showPreview();
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (Boolean.TRUE.equals(result)) {
                        showPreview();
                    }
                });
    }

    private void showPreview() {
        if (imageUri != null) {
            Glide.with(this).load(imageUri).into(imgPreviewComment);
            layoutPreviewImage.setVisibility(View.VISIBLE);
        }
    }

    private void showAttachmentMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attachment, null);

        view.findViewById(R.id.btnChupHinh).setOnClickListener(v -> {
            dialog.dismiss();
            try {
                File photoFile = createImageFile();
                imageUri = FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(imageUri);
            } catch (IOException ex) {
                Toast.makeText(this, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnChonAnh).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(
                    Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    // =========================================================================
    // Review / Bình luận
    // =========================================================================

    private void fetchLatestReviews(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi")
                .orderBy("ngay_danh_gia", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener(this, (value, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (error != null) {
                        Log.e(TAG, "Lỗi load bình luận: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        danhSachBinhLuan.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            DanhGia dg = doc.toObject(DanhGia.class);
                            danhSachBinhLuan.add(dg);
                        }
                        adapterBinhLuan.notifyDataSetChanged();
                    }
                });
    }

    // =========================================================================
    // THAY ĐỔI: guiBinhLuan — không gọi AI ở đây, chỉ validate và lưu ngay
    // =========================================================================
    private void guiBinhLuan() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String noiDung = etBinhLuan.getText().toString().trim();
        float  soSao   = rbChonSao.getRating();

        if (TextUtils.isEmpty(noiDung)) {
            etBinhLuan.setError("Nhập nội dung bình luận!");
            etBinhLuan.requestFocus();
            return;
        }

        if (soSao == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiện loading, chặn nút gửi
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi bình luận...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        btnGuiBinhLuan.setEnabled(false);

        // Upload ảnh nếu có, không thì lưu thẳng
        if (imageUri != null) {
            compressAndUploadImage(imageUri, noiDung, soSao);
        } else {
            saveReviewToFirestore(noiDung, soSao, null);
        }
    }

    private void compressAndUploadImage(Uri uri, String noiDung, float soSao) {
        progressDialog.setMessage("Đang xử lý và nén ảnh...");

        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) {
                    inputStream.close();
                }

                if (bitmap == null) {
                    runOnUiThread(() -> {
                        dismissProgress();
                        Toast.makeText(this, "Không thể đọc được ảnh!", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int   maxSize = 1024;
                float ratio   = Math.min(
                        (float) maxSize / bitmap.getWidth(),
                        (float) maxSize / bitmap.getHeight());
                if (ratio < 1.0f) {
                    bitmap = Bitmap.createScaledBitmap(
                            bitmap,
                            (int) (bitmap.getWidth()  * ratio),
                            (int) (bitmap.getHeight() * ratio),
                            true);
                }

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] data = baos.toByteArray();
                bitmap.recycle();

                runOnUiThread(() -> uploadBytesToFirebase(data, noiDung, soSao));

            } catch (Exception e) {
                Log.e(TAG, "Lỗi xử lý ảnh: " + e.getMessage());
                runOnUiThread(() -> {
                    dismissProgress();
                    Toast.makeText(this, "Lỗi nén ảnh!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void uploadBytesToFirebase(byte[] data, String noiDung, float soSao) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("reviews/" + System.currentTimeMillis() + ".jpg");

        storageRef.putBytes(data)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setMessage("Đang tải ảnh lên: " + (int) progress + "%");
                    }
                })
                .addOnSuccessListener(taskSnapshot -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setMessage("Đang lưu bình luận...");
                    }
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveReviewToFirestore(noiDung, soSao, downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    Log.e(TAG, "Lỗi upload ảnh: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải ảnh lên!", Toast.LENGTH_SHORT).show();
                });
    }

    // =========================================================================
    // THAY ĐỔI: Lưu ngay với "cho_duyet" → toast → gọi AI kiểm duyệt ngầm
    // =========================================================================
    private void saveReviewToFirestore(String content, float stars, String imageUrl) {
        FirebaseUser user   = FirebaseAuth.getInstance().getCurrentUser();
        String name   = (user != null && !TextUtils.isEmpty(user.getDisplayName()))
                ? user.getDisplayName() : "Người dùng";
        String avatar = (user != null && user.getPhotoUrl() != null)
                ? user.getPhotoUrl().toString() : null;

        Map<String, Object> review = new HashMap<>();
        review.put("id_nguoi_dung",  currentUserId);
        review.put("ten_nguoi_dung", name);
        review.put("avatar_url",     avatar);
        review.put("id_mon_an",      currentDishId);
        review.put("noi_dung",       content);
        review.put("so_sao",         stars);
        review.put("hinh_anh_url",   imageUrl);
        review.put("trang_thai",     "cho_duyet"); // Lưu trước, chờ AI duyệt
        review.put("ngay_danh_gia",  FieldValue.serverTimestamp());

        db.collection("danh_gia").add(review)
                .addOnSuccessListener(documentReference -> {
                    dismissProgress();
                    etBinhLuan.setText("");
                    rbChonSao.setRating(0);
                    imageUri = null;
                    layoutPreviewImage.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã gửi bình luận, đang kiểm duyệt...", Toast.LENGTH_SHORT).show();

                    // Lắng nghe Cloud Function cập nhật trang_thai
                    String docId = documentReference.getId();
                    final float finalStars = stars;
                    documentReference.addSnapshotListener((snap, err) -> {
                        if (snap == null || !snap.exists()) return;
                        String trangThai = snap.getString("trang_thai");
                        if (trangThai == null || trangThai.equals("cho_duyet")) return;

                        // Cloud Function đã duyệt xong
                        if (trangThai.equals("hien_thi")) {
                            updateTotalRating(finalStars);
                            Toast.makeText(this, "Bình luận đã được duyệt!", Toast.LENGTH_SHORT).show();
                        } else if (trangThai.equals("vi_pham")) {
                            Toast.makeText(this, "Bình luận bị từ chối do vi phạm nội quy.", Toast.LENGTH_LONG).show();
                        }
                        // Hủy listener sau khi đã xử lý xong
                        documentReference.addSnapshotListener((s, e) -> {}).remove();
                    });
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    Log.e(TAG, "Lỗi lưu bình luận: " + e.getMessage());
                    Toast.makeText(this, "Lỗi lưu bình luận!", Toast.LENGTH_SHORT).show();
                });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (btnGuiBinhLuan != null) {
            btnGuiBinhLuan.setEnabled(true);
        }
    }

    private void updateTotalRating(float newStar) {
        DocumentReference monAnRef = db.collection("mon_an").document(currentDishId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(monAnRef);
            if (!snapshot.exists()) {
                return null;
            }

            Double totalScoreVal  = snapshot.getDouble("totalScore");
            Long   reviewCountVal = snapshot.getLong("reviewCount");

                Map<String, Object> comment = new HashMap<>();

                comment.put("id_mon_an", currentDishId);
                comment.put("noi_dung", noiDung);
                comment.put("so_sao", soSaoMoi);

                comment.put("trang_thai", "hien_thi");

                comment.put("ten_nguoi_dung", name);

                comment.put("ngay_danh_gia", FieldValue.serverTimestamp());
            double currentTotalScore  = totalScoreVal  != null ? totalScoreVal  : 0.0;
            long   currentReviewCount = reviewCountVal != null ? reviewCountVal : 0L;

            double newTotalScore  = currentTotalScore + newStar;
            long   newReviewCount = currentReviewCount + 1;
            double newRating      = Math.round(
                    (newTotalScore / newReviewCount) * 10.0) / 10.0;

            transaction.update(monAnRef,
                    "totalScore",  newTotalScore,
                    "reviewCount", newReviewCount,
                    "rating",      newRating);
            return null;
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction rating thất bại: " + e.getMessage());
        });
    }

    private void checkIfInPlan() {
        if (currentUserId == null) return;
        db.collection("ke_hoach_nau_an")
                .document(currentUserId + "_" + currentDishId)
                .addSnapshotListener(this, (doc, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (doc != null) {
                        isInPlan = doc.exists();
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
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMonAn == null) return;

        String idPlan = currentUserId + "_" + currentDishId;
        if (isInPlan) {
            db.collection("ke_hoach_nau_an").document(idPlan).delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa khỏi kế hoạch!", Toast.LENGTH_SHORT).show());
        } else {
            String[] types = {"Sáng", "Trưa", "Tối"};
            new AlertDialog.Builder(this)
                    .setTitle("Chọn buổi nấu ăn")
                    .setItems(types, (dialog, which) -> {
                        String mealType;
                        if (which == 0) mealType = "Sang";
                        else if (which == 1) mealType = "Trua";
                        else mealType = "Toi";

                        saveToCookingPlan(idPlan, mealType);
                    })
                    .show();
        }
    }

    private void saveToCookingPlan(String idPlan, String mealType) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id_nguoi_dung", currentUserId);
        plan.put("id_mon_an", currentDishId);
        plan.put("ten_mon", currentMonAn.getTen_mon());
        plan.put("hinh_anh", currentMonAn.getHinh_anh());
        plan.put("ngay_lap_ke_hoach", FieldValue.serverTimestamp());
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

        db.collection("ke_hoach_nau_an").document(idPlan).set(plan)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã thêm vào kế hoạch " + (mealType.equals("Sang") ? "Sáng" : mealType.equals("Trua") ? "Trưa" : "Tối") + "!", Toast.LENGTH_SHORT).show());
    }

    private void listenRatingRealtime(String id) {
        db.collection("mon_an").document(id)
                .addSnapshotListener(this, (doc, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe món ăn: " + error.getMessage());
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        return;
                    }
                    currentMonAn = doc.toObject(MonAn.class);
                    if (currentMonAn != null) {
                        currentMonAn.setId_mon_an(doc.getId());
                        updateRatingUI(RatingUtils.getRatingOnly(currentMonAn), currentMonAn.getReviewCount());

                        tvTen.setText(currentMonAn.getTen_mon());
                        tvThoiGian.setText(String.format(Locale.getDefault(), "%d phút", currentMonAn.getThoi_gian_nau()));

                        // Cập nhật nguyên liệu - THAY ĐỔI
                        if (currentMonAn.getDanh_sach_nguyen_lieu() != null) {
                            fullNguyenLieuList = currentMonAn.getDanh_sach_nguyen_lieu();
                            updateNguyenLieuDisplay();
                        }

                        // Cập nhật bước nấu
                        if (currentMonAn.getDanh_sach_buoc_nau() != null) {
                            buocNauAdapter = new BuocNauAdapter(currentMonAn.getDanh_sach_buoc_nau());
                            rvBuocNau.setAdapter(buocNauAdapter);
                            rvBuocNau.setVisibility(View.VISIBLE);
                        }

                        // Cập nhật info card - MỚI
                        if (tvDoKho != null && currentMonAn.getDo_kho() != null) {
                            tvDoKho.setText(currentMonAn.getDo_kho());
                        }
                        if (tvKhauPhan != null) {
                            tvKhauPhan.setText(String.format(Locale.getDefault(), "%d người", currentMonAn.getKhau_phan()));
                        }
                        if (tvRegion != null && currentMonAn.getVung_mien() != null) {
                            tvRegion.setText(currentMonAn.getVung_mien());
                        }

                        // Cập nhật chuẩn bị/sơ chế - MỚI
                        if (tvChuanBi != null) {
                            if (currentMonAn.getDanh_sach_so_che() != null && !currentMonAn.getDanh_sach_so_che().isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (MonAn.SoChe sc : currentMonAn.getDanh_sach_so_che()) {
                                    if (sc.tieu_de != null && !sc.tieu_de.isEmpty()) {
                                        sb.append(sc.tieu_de).append(": ");
                                    }
                                    sb.append(sc.noi_dung).append("\n\n");
                                }
                                tvChuanBi.setText(sb.toString().trim());
                                ((View)tvChuanBi.getParent()).setVisibility(View.VISIBLE);
                            } else {
                                ((View)tvChuanBi.getParent()).setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    private void checkIfSaved() {
        if (currentUserId == null) {
            return;
        }
        db.collection("mon_da_luu")
                .document(currentUserId + "_" + currentDishId)
                .addSnapshotListener(this, (doc, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (doc != null) {
                        isSaved = doc.exists();
                        updateSaveButtonUI(false);
                    }
                });
    }

    private void updateSaveButtonUI(boolean animate) {
        if (btnFavoriteDetail == null) {
            return;
        }
        if (isSaved) {
            btnFavoriteDetail.setImageResource(R.drawable.ic_heart_filled);
            btnFavoriteDetail.setColorFilter(Color.RED);
            btnFavoriteDetail.setBackgroundResource(R.drawable.bg_header_button_favorite_selected);
        } else {
            btnFavoriteDetail.setImageResource(R.drawable.ic_heart_outline);
            btnFavoriteDetail.setColorFilter(Color.WHITE);
            btnFavoriteDetail.setBackgroundResource(R.drawable.bg_header_button_glass);
        }
        if (animate) {
            btnFavoriteDetail.animate()
                    .scaleX(1.4f)
                    .scaleY(1.4f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            btnFavoriteDetail.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(150)
                                    .start();
                        }
                    }).start();
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String idLuu = currentUserId + "_" + currentDishId;

        if (isSaved) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bỏ yêu thích món này?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("mon_da_luu").document(idLuu).delete()
                                .addOnSuccessListener(unused -> updateSaveButtonUI(true));
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an",     currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data)
                    .addOnSuccessListener(unused -> updateSaveButtonUI(true));
        }
    }

    // =========================================================================
    // Lịch sử xem
    // =========================================================================

    private void addToHistory(String dishId) {
        if (currentUserId == null) {
            return;
        }
        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("id_mon_an", dishId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!querySnapshot.isEmpty()) {
                        db.collection("lich_su_xem")
                                .document(querySnapshot.getDocuments().get(0).getId())
                                .update("thoi_gian_xem", FieldValue.serverTimestamp());
                    } else {
                        Map<String, Object> h = new HashMap<>();
                        h.put("id_nguoi_dung", currentUserId);
                        h.put("id_mon_an",     dishId);
                        h.put("thoi_gian_xem", FieldValue.serverTimestamp());
                        db.collection("lich_su_xem").add(h)
                                .addOnSuccessListener(ref -> limitHistoryTo15());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi thêm lịch sử xem: " + e.getMessage());
                });
    }

    private void limitHistoryTo15() {
        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    for (int i = 15; i < docs.size(); i++) {
                        db.collection("lich_su_xem")
                                .document(docs.get(i).getId())
                                .delete();
                    }
                });
    }

    private void updateNguyenLieuDisplay() {
        if (fullNguyenLieuList == null || fullNguyenLieuList.isEmpty()) {
            return;
        }

        List<MonAn.ChiTietNguyenLieu> displayList;
        if (isShowingAllNguyenLieu || fullNguyenLieuList.size() <= 6) {
            displayList = fullNguyenLieuList;
        } else {
            displayList = fullNguyenLieuList.subList(0, 6);
        }

        // Cập nhật text của nút xem thêm
        if (btnXemThemNguyenLieu != null) {
            if (fullNguyenLieuList.size() <= 6) {
                btnXemThemNguyenLieu.setVisibility(View.GONE);
            } else {
                btnXemThemNguyenLieu.setVisibility(View.VISIBLE);
                if (isShowingAllNguyenLieu) {
                    btnXemThemNguyenLieu.setText("- Thu gọn");
                } else {
                    int remaining = fullNguyenLieuList.size() - 6;
                    btnXemThemNguyenLieu.setText(String.format("+ Xem thêm %d nguyên liệu", remaining));
                }
            }
        }

        nguyenLieuAdapter = new NguyenLieuAdapter(displayList);
        rvNguyenLieu.setAdapter(nguyenLieuAdapter);
    }

    private void toggleNguyenLieu() {
        isShowingAllNguyenLieu = !isShowingAllNguyenLieu;
        updateNguyenLieuDisplay();

        if (isShowingAllNguyenLieu && btnXemThemNguyenLieu != null) {
            btnXemThemNguyenLieu.setText("- Thu gọn");
        }
    }

    private void toggleBuocNau() {
        isBuocNauExpanded = !isBuocNauExpanded;

        if (isBuocNauExpanded) {
            rvBuocNau.setVisibility(View.VISIBLE);
            if (ivBuocNauArrow != null) {
                ivBuocNauArrow.setRotation(180);
            }
        } else {
            rvBuocNau.setVisibility(View.GONE);
            if (ivBuocNauArrow != null) {
                ivBuocNauArrow.setRotation(0);
            }
        }
    }
}
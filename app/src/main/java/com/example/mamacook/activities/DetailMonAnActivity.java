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
import androidx.recyclerview.widget.GridLayoutManager;
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

    public static final String EXTRA_ID           = "ID_MON_AN";
    public static final String EXTRA_HINH_ANH     = "HINH_ANH";
    public static final String EXTRA_TEN_MON      = "TEN_MON";
    public static final String EXTRA_THOI_GIAN    = "THOI_GIAN";
    public static final String EXTRA_RATING       = "RATING";
    public static final String EXTRA_REVIEW_COUNT = "REVIEW_COUNT";
    public static final String EXTRA_NGUYEN_LIEU  = "NGUYEN_LIEU";

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnFavoriteDetail, btnAddToPlan, btnAddAttachment, imgPreviewComment, btnQrCode, btnQrCodeFab;
    private ImageView btnEditMonAn, btnDeleteMonAn; 
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
        btnQrCodeFab       = findViewById(R.id.btn_qr_code_fab);
        layoutInputComment = findViewById(R.id.layout_detail_input_comment);
        rvNguyenLieu = findViewById(R.id.rv_detail_nguyen_lieu);
        rvBuocNau = findViewById(R.id.rv_detail_buoc_nau);
        tvDoKho = findViewById(R.id.tv_detail_do_kho);
        tvKhauPhan = findViewById(R.id.tv_detail_khau_phan);
        tvRegion = findViewById(R.id.tv_detail_region);
        tvSoDanhGia = findViewById(R.id.tv_detail_so_danh_gia);
        tvChuanBi = findViewById(R.id.tv_detail_chuan_bi);
        btnXemThemNguyenLieu = findViewById(R.id.btn_detail_xem_them_nguyen_lieu);
        layoutBuocNauHeader = findViewById(R.id.layout_detail_buoc_nau_header);
        ivBuocNauArrow = findViewById(R.id.iv_detail_buoc_nau_arrow);

        btnEditMonAn = findViewById(R.id.btn_edit_mon_an);
        btnDeleteMonAn = findViewById(R.id.btn_delete_mon_an);
    }

    private void checkUserRole() {
        // 1. Mặc định ban đầu (Dành cho User thường hoặc chưa đăng nhập)
        if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.VISIBLE);
        if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.VISIBLE);
        if (btnEditMonAn != null) btnEditMonAn.setVisibility(View.GONE);
        if (btnDeleteMonAn != null) btnDeleteMonAn.setVisibility(View.GONE);

        if (currentUserId == null) return;

        // 2. Gọi đúng bảng "nguoi_dung" bằng Document ID (UID)
        db.collection("nguoi_dung").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        // 3. Nếu kiểm tra đúng là Admin
                        if ("admin".equalsIgnoreCase(role)) {
            // BẬT 3 nút của Admin (Sửa, Xóa, QR)
            if (btnEditMonAn != null) btnEditMonAn.setVisibility(View.VISIBLE);
            if (btnDeleteMonAn != null) btnDeleteMonAn.setVisibility(View.VISIBLE);
            if (btnQrCode != null) btnQrCode.setVisibility(View.VISIBLE);
            if (btnQrCodeFab != null) btnQrCodeFab.setVisibility(View.GONE);

            // ẨN 2 nút của User (Yêu thích, Kế hoạch)
            if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.GONE);
            if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.GONE);
        } else {
            // Nếu role không phải admin hoặc null -> giữ nguyên mặc định user
            if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.VISIBLE);
            if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.VISIBLE);
            if (btnEditMonAn != null) btnEditMonAn.setVisibility(View.GONE);
            if (btnDeleteMonAn != null) btnDeleteMonAn.setVisibility(View.GONE);
            if (btnQrCode != null) btnQrCode.setVisibility(View.GONE);
            if (btnQrCodeFab != null) btnQrCodeFab.setVisibility(View.VISIBLE);
        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi kiểm tra quyền: " + e.getMessage());
                    // Mặc định an toàn nếu lỗi mạng/DB
                    if (btnFavoriteDetail != null) btnFavoriteDetail.setVisibility(View.VISIBLE);
                    if (btnAddToPlan != null) btnAddToPlan.setVisibility(View.VISIBLE);
                });
    }

    private void renderFromIntent() {
        Intent i = getIntent();
        String hinhAnh = i.getStringExtra(EXTRA_HINH_ANH);
        if (!TextUtils.isEmpty(hinhAnh)) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(this).load(hinhAnh).placeholder(R.drawable.bg_splash).into(imgMonAn);
            } else {
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                Glide.with(this).load(ref).placeholder(R.drawable.bg_splash).into(imgMonAn);
            }
        }
        tvTen.setText(i.getStringExtra(EXTRA_TEN_MON));
        int thoiGian = i.getIntExtra(EXTRA_THOI_GIAN, 0);
        tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", thoiGian));
        updateRatingUI(i.getStringExtra(EXTRA_RATING), i.getIntExtra(EXTRA_REVIEW_COUNT, 0));
    }

    private void updateRatingUI(String rating, int count) {
        // Ẩn các thông tin rating dư thừa theo yêu cầu tinh chỉnh UI
        if (tvRatingInfo != null) tvRatingInfo.setVisibility(View.GONE);
        if (tvDiemTrungBinh != null) tvDiemTrungBinh.setVisibility(View.GONE);
        if (tvSoDanhGia != null) tvSoDanhGia.setVisibility(View.GONE);
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
        if (btnQrCodeFab != null) btnQrCodeFab.setOnClickListener(v -> openQRCode());
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
        if (layoutBuocNauHeader != null) layoutBuocNauHeader.setOnClickListener(v -> toggleBuocNau());

        // Xử lý Click riêng của Admin
        if (btnEditMonAn != null) {
            btnEditMonAn.setOnClickListener(v -> {
                if (currentDishId != null) {
                    Intent intent = AddEditMonAnActivity.createIntent(DetailMonAnActivity.this, currentDishId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnDeleteMonAn != null) {
            btnDeleteMonAn.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa món ăn này không? Hành động này không thể hoàn tác.")
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
        intent.putExtra("EXTRA_ID", currentDishId);
        startActivity(intent);
    }

    private void deleteMonAn() {
        if (currentDishId == null) return;
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Đang xóa...");
        pd.show();
        db.collection("mon_an").document(currentDishId).delete()
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(this, "Đã xóa món ăn thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void initImageLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> review = new HashMap<>();
        review.put("id_nguoi_dung", currentUserId);
        review.put("ten_nguoi_dung", user != null ? user.getDisplayName() : "Ẩn danh");
        review.put("id_mon_an", currentDishId);
        review.put("noi_dung", content);
        review.put("so_sao", stars);
        review.put("hinh_anh_url", imageUrl);
        review.put("trang_thai", "cho_duyet");
        review.put("ngay_danh_gia", FieldValue.serverTimestamp());
        db.collection("danh_gia").add(review).addOnSuccessListener(docRef -> {
            progressDialog.dismiss();
            etBinhLuan.setText("");
            Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkIfInPlan() {
        if (currentUserId == null) return;
        // Kiểm tra xem món này có trong bất kỳ kế hoạch nào của người dùng không
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
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMonAn == null) return;

        // CẢI TIẾN: Nếu đã có thông tin ngày và buổi từ trước, lưu thẳng
        if (preSelectedDate != null && preSelectedMeal != null) {
            saveToCookingPlan(preSelectedDate, preSelectedMeal);
            return;
        }

        // 1. Tạo danh sách 7 ngày trong tuần hiện tại
        List<String> dateList = new ArrayList<>();
        List<String> dateDisplayList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Điều chỉnh về Thứ 2 đầu tuần
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
             cal.add(Calendar.DAY_OF_YEAR, -6); // Nếu hôm nay là CN, quay lại T2 tuần trước?
             // Thường tuần hiện tại tính từ T2 đến CN.
        }

        // Cách đơn giản nhất: Lấy 7 ngày kể từ hôm nay hoặc từ đầu tuần này.
        // User muốn "các ngày trong tuần hiện tại"
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

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

        // Lấy thông tin Thứ mấy từ selectedDate (yyyy-MM-dd)
        String thuMay = "";
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dbFormat.parse(selectedDate);
            if (date != null) {
                SimpleDateFormat thuFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
                thuMay = thuFormat.format(date);
            }
        } catch (Exception e) {
            thuMay = selectedDate; // Fallback nếu có lỗi
        }

        String finalThuMay = thuMay;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thêm món")
                .setMessage("Bạn có chắc chắn muốn thêm món '" + currentMonAn.getTen_mon() + "' vào " + buoiTiengViet + " " + finalThuMay + " không?")
                .setPositiveButton("Thêm ngay", (dialog, which) -> {
                    checkDuplicateAndSave(selectedDate, mealType, finalThuMay);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void checkDuplicateAndSave(String selectedDate, String mealType, String thuMay) {
        db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("id_mon_an", currentDishId)
                .whereEqualTo("ngay_chi_tiet", selectedDate)
                .whereEqualTo("buoi", mealType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String buoiTiengViet = mealType.equals("Sang") ? "Bữa Sáng" : mealType.equals("Trua") ? "Bữa Trưa" : "Bữa Tối";
                        Toast.makeText(this, "Món ăn này đã có trong " + buoiTiengViet + " " + thuMay + "!", Toast.LENGTH_LONG).show();
                    } else {
                        performSavePlan(selectedDate, mealType, thuMay);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performSavePlan(String selectedDate, String mealType, String thuMay) {
        Map<String, Object> plan = new HashMap<>();
        // ... (phần code map vẫn giữ nguyên)
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
                .addOnSuccessListener(docRef -> Toast.makeText(this, "Đã thêm vào kế hoạch " + (mealType.equals("Sang") ? "Bữa Sáng" : mealType.equals("Trua") ? "Bữa Trưa" : "Bữa Tối") + " " + thuMay + "!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void listenRatingRealtime(String id) {
        db.collection("mon_an").document(id).addSnapshotListener(this, (doc, error) -> {
    private void startListeningMonAnDetail(String id) {
        DocumentReference docRef = db.collection("mon_an").document(id);
        // Thêm 'this' để Firebase tự động quản lý vòng đời listener theo Activity
        monAnListener = docRef.addSnapshotListener(this, (doc, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            if (doc != null && doc.exists()) {
                currentMonAn = doc.toObject(MonAn.class);
                if (currentMonAn != null) {
                    currentMonAn.setId_mon_an(doc.getId());
                    updateUI(currentMonAn);
                }
            } else {
                Toast.makeText(this, "Món ăn không tồn tại hoặc đã bị xóa", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI(MonAn monAn) {
        if (monAn == null) return;

        // 1. Cập nhật Tên và Ảnh
        tvTen.setText(monAn.getTen_mon());
        String hinhAnh = monAn.getHinh_anh();
        if (!TextUtils.isEmpty(hinhAnh)) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(this).load(hinhAnh).placeholder(R.drawable.bg_splash).into(imgMonAn);
            } else {
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                Glide.with(this).load(ref).placeholder(R.drawable.bg_splash).into(imgMonAn);
            }
        }

        // 2. Cập nhật 3 cột thông tin (Thời gian, Độ khó, Khẩu phần)
        tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", monAn.getThoi_gian_nau()));

        String dk = monAn.getDo_kho();
        tvDoKho.setText(TextUtils.isEmpty(dk) ? "Chưa rõ" : dk);

        int kp = monAn.getKhau_phan();
        tvKhauPhan.setText(kp > 0 ? kp + " người" : "Chưa rõ");

        String vm = monAn.getVung_mien();
        tvRegion.setText("Vùng miền: " + (TextUtils.isEmpty(vm) ? "Chưa rõ" : vm));

        // 3. Cập nhật phần Sơ chế (Chuẩn bị) - Xử lý List SoChe
        if (monAn.getDanh_sach_so_che() != null && !monAn.getDanh_sach_so_che().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (MonAn.SoChe sc : monAn.getDanh_sach_so_che()) {
                if (!TextUtils.isEmpty(sc.tieu_de)) {
                    sb.append("• ").append(sc.tieu_de).append(":\n");
                }
                sb.append(sc.noi_dung).append("\n\n");
            }
            tvChuanBi.setText(sb.toString().trim());
            tvChuanBi.setVisibility(View.VISIBLE);
        } else {
            tvChuanBi.setText("Không có thông tin sơ chế.");
        }

        // 4. Cập nhật Nguyên liệu & Bước nấu
        if (monAn.getDanh_sach_nguyen_lieu() != null) {
            fullNguyenLieuList = monAn.getDanh_sach_nguyen_lieu();
            updateNguyenLieuDisplay();
        }

        if (monAn.getDanh_sach_buoc_nau() != null) {
            buocNauAdapter = new BuocNauAdapter(monAn.getDanh_sach_buoc_nau());
            rvBuocNau.setAdapter(buocNauAdapter);
        }

        updateRatingUI(null, 0); // Ẩn các view rating theo thiết kế mới
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (monAnListener != null) {
            monAnListener.remove();
            monAnListener = null;
        }
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
            btnFavoriteDetail.setColorFilter(Color.RED);
        } else {
            btnFavoriteDetail.setImageResource(R.drawable.ic_heart_outline);
            btnFavoriteDetail.setColorFilter(Color.WHITE);
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) return;
        String idLuu = currentUserId + "_" + currentDishId;
        if (isSaved) db.collection("mon_da_luu").document(idLuu).delete();
        else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data);
        }
    }

    private void checkIfInPlan() {
        if (currentUserId == null) return;
        db.collection("ke_hoach_nau_an").document(currentUserId + "_" + currentDishId).addSnapshotListener(this, (doc, error) -> {
            if (doc != null) isInPlan = doc.exists();
        });
    }

    private void toggleCookingPlan() {
        if (currentUserId == null) return;
        if (isInPlan) db.collection("ke_hoach_nau_an").document(currentUserId + "_" + currentDishId).delete();
        else saveToCookingPlan();
    }

    private void saveToCookingPlan() {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id_nguoi_dung", currentUserId);
        plan.put("id_mon_an", currentDishId);
        plan.put("ngay_lap_ke_hoach", FieldValue.serverTimestamp());
        db.collection("ke_hoach_nau_an").document(currentUserId + "_" + currentDishId).set(plan)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã thêm vào kế hoạch!", Toast.LENGTH_SHORT).show());
    }

    private void addToHistory(String dishId) {
        if (currentUserId == null) return;
        Map<String, Object> h = new HashMap<>();
        h.put("id_nguoi_dung", currentUserId);
        h.put("id_mon_an", dishId);
        h.put("thoi_gian_xem", FieldValue.serverTimestamp());
        db.collection("lich_su_xem").add(h).addOnSuccessListener(ref -> limitHistoryTo15());
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
    private void toggleBuocNau() {
        isBuocNauExpanded = !isBuocNauExpanded;
        rvBuocNau.setVisibility(isBuocNauExpanded ? View.VISIBLE : View.GONE);
        ivBuocNauArrow.setRotation(isBuocNauExpanded ? 180 : 0);
    }
}
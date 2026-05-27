package com.example.mamacook.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.models.MonAn;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class AddEditMonAnActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView imgMonAn;
    private EditText edtTenMon, edtThoiGian, edtKhauPhan;
    private AutoCompleteTextView spinnerDanhMuc, spinnerDoKho, spinnerVungMien;
    private LinearLayout containerNguyenLieu, containerSoChe, containerBuocNau;
    private Button btnSave, btnSelectImg, btnCaptureImg;
    private ImageButton btnBack, btnAddNguyenLieu, btnAddSoChe, btnAddBuocNau;
    private ProgressBar progressBar;

    private Uri imageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String idMonAnExisting = null;
    private String currentImageUrl = null;
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_mon_an);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupSpinners();
        setupListeners();

        idMonAnExisting = getIntent().getStringExtra("id_mon_an");
        if (idMonAnExisting != null) {
            loadMonAnData(idMonAnExisting);
        } else {
            addNguyenLieuRow(null);
            addSoCheRow(null);
            addBuocNauRow(null);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_form);
        imgMonAn = findViewById(R.id.img_mon_an_form);
        btnCaptureImg = findViewById(R.id.btn_capture_img);
        btnSelectImg = findViewById(R.id.btn_select_img);
        edtTenMon = findViewById(R.id.edt_ten_mon);
        edtThoiGian = findViewById(R.id.edt_thoi_gian_nau);
        edtKhauPhan = findViewById(R.id.edt_khau_phan);
        spinnerDanhMuc = findViewById(R.id.spinner_danh_muc);
        spinnerDoKho = findViewById(R.id.spinner_do_kho);
        spinnerVungMien = findViewById(R.id.spinner_vung_mien);
        btnAddNguyenLieu = findViewById(R.id.btn_add_nguyen_lieu_form);
        containerNguyenLieu = findViewById(R.id.container_nguyen_lieu);
        btnAddSoChe = findViewById(R.id.btn_add_so_che_form);
        containerSoChe = findViewById(R.id.container_so_che);
        btnAddBuocNau = findViewById(R.id.btn_add_buoc_nau_form);
        containerBuocNau = findViewById(R.id.container_buoc_nau);
        btnSave = findViewById(R.id.btn_save_form);
        progressBar = findViewById(R.id.progress_bar_form);
    }

    private void setupSpinners() {
        String[] danhMuc = {"Món chính", "Món phụ", "Món tráng miệng", "Đồ uống", "Món ăn vặt"};
        ArrayAdapter<String> adapterDanhMuc = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, danhMuc);
        spinnerDanhMuc.setAdapter(adapterDanhMuc);

        String[] doKho = {"Dễ", "Trung bình", "Khó"};
        ArrayAdapter<String> adapterDoKho = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, doKho);
        spinnerDoKho.setAdapter(adapterDoKho);

        String[] vungMien = {"Miền Bắc", "Miền Trung", "Miền Nam", "Toàn quốc"};
        ArrayAdapter<String> adapterVungMien = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, vungMien);
        spinnerVungMien.setAdapter(adapterVungMien);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSelectImg.setOnClickListener(v -> openGallery());
        btnCaptureImg.setOnClickListener(v -> openCamera());
        btnAddNguyenLieu.setOnClickListener(v -> addNguyenLieuRow(null));
        btnAddSoChe.setOnClickListener(v -> addSoCheRow(null));
        btnAddBuocNau.setOnClickListener(v -> addBuocNauRow(null));
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void openGallery() {
        if (checkPermissions()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } else {
            requestPermissions();
        }
    }

    private void openCamera() {
        if (checkPermissions()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
        } else {
            requestPermissions();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST && data.getData() != null) {
                imageUri = data.getData();
                imgMonAn.setImageURI(imageUri);
            } else if (requestCode == CAPTURE_IMAGE_REQUEST && data.getExtras() != null) {
                android.graphics.Bitmap bitmap = (android.graphics.Bitmap) data.getExtras().get("data");
                imgMonAn.setImageBitmap(bitmap);
                imageUri = getImageUriFromBitmap(bitmap);
            }
        }
    }

    private Uri getImageUriFromBitmap(android.graphics.Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "temp_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (Exception e) {
            return null;
        }
    }

    private void addNguyenLieuRow(@Nullable MonAn.ChiTietNguyenLieu data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_nguyen_lieu, containerNguyenLieu, false);
        EditText edtName = view.findViewById(R.id.edtIngredientName);
        EditText edtQty = view.findViewById(R.id.edtIngredientQty);
        EditText edtUnit = view.findViewById(R.id.edtIngredientUnit);
        View btnRemove = view.findViewById(R.id.btnRemoveIngredient);

        if (data != null) {
            edtName.setText(data.ten_nguyen_lieu);
            edtQty.setText(String.valueOf(data.so_luong));
            edtUnit.setText(data.don_vi);
        }

        btnRemove.setOnClickListener(v -> containerNguyenLieu.removeView(view));
        containerNguyenLieu.addView(view);
    }

    private void addSoCheRow(@Nullable MonAn.SoChe data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_so_che, containerSoChe, false);
        EditText edtContent = view.findViewById(R.id.edtSoChe);
        View btnRemove = view.findViewById(R.id.btnRemove);

        if (data != null) {
            edtContent.setText(data.noi_dung);
        }

        btnRemove.setOnClickListener(v -> containerSoChe.removeView(view));
        containerSoChe.addView(view);
    }

    private void addBuocNauRow(@Nullable MonAn.BuocNau data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_buoc_nau, containerBuocNau, false);
        EditText edtTitle = view.findViewById(R.id.edtStepTitle);
        EditText edtDesc = view.findViewById(R.id.edtStepDescription);
        EditText edtTime = view.findViewById(R.id.edtStepTime);
        View btnRemove = view.findViewById(R.id.btnRemoveStep);

        if (data != null) {
            edtTitle.setText(data.tieu_de);
            edtDesc.setText(data.mo_ta);
            edtTime.setText(String.valueOf(data.thoi_gian_buoc));
        }

        btnRemove.setOnClickListener(v -> containerBuocNau.removeView(view));
        containerBuocNau.addView(view);
    }

    private void loadMonAnData(String id) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("mon_an").document(id).get().addOnSuccessListener(doc -> {
            progressBar.setVisibility(View.GONE);
            MonAn monAn = doc.toObject(MonAn.class);
            if (monAn != null) {
                currentImageUrl = monAn.getHinh_anh();
                edtTenMon.setText(monAn.getTen_mon());
                edtThoiGian.setText(String.valueOf(monAn.getThoi_gian_nau()));
                edtKhauPhan.setText(String.valueOf(monAn.getKhau_phan()));

                if (monAn.getHinh_anh() != null && !monAn.getHinh_anh().isEmpty()) {
                    Glide.with(this).load(monAn.getHinh_anh()).into(imgMonAn);
                }

                spinnerDoKho.setText(monAn.getDo_kho(), false);
                spinnerVungMien.setText(monAn.getVung_mien(), false);

                containerNguyenLieu.removeAllViews();
                if (monAn.getDanh_sach_nguyen_lieu() != null) {
                    for (MonAn.ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) {
                        addNguyenLieuRow(nl);
                    }
                }

                containerSoChe.removeAllViews();
                if (monAn.getDanh_sach_so_che() != null) {
                    for (MonAn.SoChe sc : monAn.getDanh_sach_so_che()) {
                        addSoCheRow(sc);
                    }
                }

                containerBuocNau.removeAllViews();
                if (monAn.getDanh_sach_buoc_nau() != null) {
                    for (MonAn.BuocNau bn : monAn.getDanh_sach_buoc_nau()) {
                        addBuocNauRow(bn);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validateData() {
        String tenMon = edtTenMon.getText().toString().trim();
        if (tenMon.isEmpty()) {
            edtTenMon.setError("Vui lòng nhập tên món");
            edtTenMon.requestFocus();
            return false;
        }

        String thoiGian = edtThoiGian.getText().toString().trim();
        if (thoiGian.isEmpty()) {
            edtThoiGian.setError("Vui lòng nhập thời gian");
            edtThoiGian.requestFocus();
            return false;
        }

        String khauPhan = edtKhauPhan.getText().toString().trim();
        if (khauPhan.isEmpty()) {
            edtKhauPhan.setError("Vui lòng nhập khẩu phần");
            edtKhauPhan.requestFocus();
            return false;
        }

        if (spinnerDoKho.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn độ khó", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void validateAndSave() {
        if (isSaving) return;
        if (!validateData()) return;

        isSaving = true;
        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            compressAndUploadImage(imageUri);
        } else if (idMonAnExisting != null && currentImageUrl != null) {
            saveToFirestore(idMonAnExisting, currentImageUrl);
        } else {
            isSaving = false;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng chọn ảnh món ăn", Toast.LENGTH_SHORT).show();
        }
    }

    private void compressAndUploadImage(Uri uri) {
        try {
            String path = getFilePathFromUri(uri);
            if (path == null) {
                uploadImage(uri);
                return;
            }
            Luban.with(this)
                    .load(path)
                    .ignoreBy(100)
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {}

                        @Override
                        public void onSuccess(File file) {
                            uploadImage(Uri.fromFile(file));
                        }

                        @Override
                        public void onError(Throwable e) {
                            uploadImage(uri);
                        }
                    }).launch();
        } catch (Exception e) {
            uploadImage(uri);
        }
    }

    private String getFilePathFromUri(Uri uri) {
        if (uri == null) return null;
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        } else if ("content".equals(uri.getScheme())) {
            try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) return null;
                File tempFile = new File(getCacheDir(), "temp_upload_" + System.currentTimeMillis() + ".jpg");
                try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return tempFile.getAbsolutePath();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void uploadImage(Uri uri) {
        String id = (idMonAnExisting != null) ? idMonAnExisting : db.collection("mon_an").document().getId();
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child("mon_an/" + fileName);

        storageRef.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            saveToFirestore(id, downloadUri.toString());
        }).addOnFailureListener(e -> {
            isSaving = false;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveToFirestore(String id, String imagePath) {
        MonAn monAn = new MonAn();
        monAn.setId_mon_an(id);
        monAn.setTen_mon(edtTenMon.getText().toString().trim());
        monAn.setHinh_anh(imagePath);
        monAn.setThoi_gian_nau(safeParseInt(edtThoiGian.getText().toString().trim()));
        monAn.setKhau_phan(safeParseInt(edtKhauPhan.getText().toString().trim()));
        monAn.setDo_kho(spinnerDoKho.getText().toString().trim());
        monAn.setVung_mien(spinnerVungMien.getText().toString().trim());
        monAn.setId_danh_muc("default_cat");
        monAn.setGia_tien(0);
        monAn.setLuot_xem(0);
        monAn.setMatch_score(0);
        monAn.setRating(0.0);
        monAn.setReviewCount(0);
        monAn.setTong_luot_danh_gia(0);
        monAn.setTotalScore(0.0);
        monAn.setTrang_thai("hiển thị");
        monAn.setNgay_tao(Timestamp.now());
        monAn.setNgay_cap_nhat(Timestamp.now());

        extractDynamicData(monAn);
        monAn.setTu_khoa_tim_kiem(generateKeywords(monAn.getTen_mon()));

        db.collection("mon_an").document(id).set(monAn).addOnSuccessListener(aVoid -> {
            isSaving = false;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Lưu món ăn thành công!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            isSaving = false;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void extractDynamicData(MonAn monAn) {
        List<MonAn.ChiTietNguyenLieu> ingredients = new ArrayList<>();
        for (int i = 0; i < containerNguyenLieu.getChildCount(); i++) {
            View v = containerNguyenLieu.getChildAt(i);
            MonAn.ChiTietNguyenLieu nl = new MonAn.ChiTietNguyenLieu();
            nl.ten_nguyen_lieu = ((EditText) v.findViewById(R.id.edtIngredientName)).getText().toString().trim();
            nl.so_luong = safeParseDouble(((EditText) v.findViewById(R.id.edtIngredientQty)).getText().toString().trim());
            nl.don_vi = ((EditText) v.findViewById(R.id.edtIngredientUnit)).getText().toString().trim();
            nl.ghi_chu = "";
            ingredients.add(nl);
        }
        monAn.setDanh_sach_nguyen_lieu(ingredients);

        List<MonAn.SoChe> soCheList = new ArrayList<>();
        for (int i = 0; i < containerSoChe.getChildCount(); i++) {
            View v = containerSoChe.getChildAt(i);
            MonAn.SoChe sc = new MonAn.SoChe();
            sc.tieu_de = "Sơ chế";
            sc.noi_dung = ((EditText) v.findViewById(R.id.edtSoChe)).getText().toString().trim();
            soCheList.add(sc);
        }
        monAn.setDanh_sach_so_che(soCheList);

        List<MonAn.BuocNau> buocNauList = new ArrayList<>();
        for (int i = 0; i < containerBuocNau.getChildCount(); i++) {
            View v = containerBuocNau.getChildAt(i);
            MonAn.BuocNau bn = new MonAn.BuocNau();
            bn.so_buoc = i + 1;
            bn.tieu_de = ((EditText) v.findViewById(R.id.edtStepTitle)).getText().toString().trim();
            bn.mo_ta = ((EditText) v.findViewById(R.id.edtStepDescription)).getText().toString().trim();
            bn.thoi_gian_buoc = safeParseInt(((EditText) v.findViewById(R.id.edtStepTime)).getText().toString().trim());
            bn.hinh_anh_buoc = null;
            buocNauList.add(bn);
        }
        monAn.setDanh_sach_buoc_nau(buocNauList);
    }

    private int safeParseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private double safeParseDouble(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    private List<String> generateKeywords(String name) {
        List<String> keywords = new ArrayList<>();
        String[] words = name.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < words.length; j++) {
                sb.append(words[j]).append(" ");
                keywords.add(sb.toString().trim());
            }
        }
        return keywords;
    }
}

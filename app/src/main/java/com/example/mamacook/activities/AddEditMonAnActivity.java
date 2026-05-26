package com.example.mamacook.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.models.MonAn;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
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

    private ImageView imgMonAn;
    private EditText edtTenMon, edtThoiGian, edtKhauPhan;
    private ChipGroup cgDoKho;
    private LinearLayout containerIngredients, containerSoChe, containerBuocNau;
    private MaterialButton btnSaveMonAn, btnChonAnh;
    private MaterialButton btnAddIngredient, btnAddSoChe, btnAddBuocNau;

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
        setupListeners();

        idMonAnExisting = getIntent().getStringExtra("ID_MON_AN");
        if (idMonAnExisting != null) {
            loadMonAnData(idMonAnExisting);
        } else {
            // Thêm mặc định 1 dòng cho mỗi loại để người dùng dễ nhập liệu
            addIngredientRow(null);
            addSoCheRow(null);
            addBuocNauRow(null);
        }
    }

    /**
     * Khởi tạo các view từ layout XML
     */
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgMonAn = findViewById(R.id.imgMonAn);
        edtTenMon = findViewById(R.id.edtTenMon);
        edtThoiGian = findViewById(R.id.edtThoiGian);
        edtKhauPhan = findViewById(R.id.edtKhauPhan);
        cgDoKho = findViewById(R.id.cgDoKho);
        containerIngredients = findViewById(R.id.containerIngredients);
        containerSoChe = findViewById(R.id.containerSoChe);
        containerBuocNau = findViewById(R.id.containerBuocNau);
        btnSaveMonAn = findViewById(R.id.btnSaveMonAn);
        btnChonAnh = findViewById(R.id.btnChonAnh);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnAddSoChe = findViewById(R.id.btnAddSoChe);
        btnAddBuocNau = findViewById(R.id.btnAddBuocNau);
    }

    /**
     * Thiết lập các sự kiện click cho các button
     */
    private void setupListeners() {
        btnChonAnh.setOnClickListener(v -> openGallery());
        btnAddIngredient.setOnClickListener(v -> addIngredientRow(null));
        btnAddSoChe.setOnClickListener(v -> addSoCheRow(null));
        btnAddBuocNau.setOnClickListener(v -> addBuocNauRow(null));
        btnSaveMonAn.setOnClickListener(v -> validateAndSave());
    }

    /**
     * Mở thư viện ảnh để chọn ảnh món ăn
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Nhận kết quả trả về sau khi chọn ảnh từ gallery
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgMonAn.setImageURI(imageUri);
        }
    }

    /**
     * Thêm một hàng nhập liệu nguyên liệu mới vào giao diện
     * @param data Dữ liệu nguyên liệu hiện có (nếu là chế độ sửa)
     */
    private void addIngredientRow(@Nullable MonAn.ChiTietNguyenLieu data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_nguyen_lieu, containerIngredients, false);
        EditText edtName = view.findViewById(R.id.edtIngredientName);
        EditText edtQty = view.findViewById(R.id.edtIngredientQty);
        EditText edtUnit = view.findViewById(R.id.edtIngredientUnit);
        View btnRemove = view.findViewById(R.id.btnRemoveIngredient);

        // [VALIDATION HOTFIX] - Clear error on text change
        edtName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { edtName.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        if (data != null) {
            edtName.setText(data.ten_nguyen_lieu);
            edtQty.setText(String.valueOf(data.so_luong));
            edtUnit.setText(data.don_vi);
        }

        btnRemove.setOnClickListener(v -> containerIngredients.removeView(view));
        containerIngredients.addView(view);
    }

    /**
     * Thêm một hàng nhập liệu phần sơ chế vào giao diện
     * @param data Dữ liệu sơ chế hiện có
     */
    private void addSoCheRow(@Nullable MonAn.SoChe data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_so_che, containerSoChe, false);
        EditText edtContent = view.findViewById(R.id.edtSoChe);
        View btnRemove = view.findViewById(R.id.btnRemove);

        // [VALIDATION HOTFIX] - Clear error on text change
        edtContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { edtContent.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        if (data != null) {
            edtContent.setText(data.noi_dung);
        }

        btnRemove.setOnClickListener(v -> containerSoChe.removeView(view));
        containerSoChe.addView(view);
    }

    /**
     * Thêm một hàng nhập liệu các bước nấu vào giao diện
     * @param data Dữ liệu bước nấu hiện có
     */
    private void addBuocNauRow(@Nullable MonAn.BuocNau data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dynamic_buoc_nau, containerBuocNau, false);
        EditText edtTitle = view.findViewById(R.id.edtStepTitle);
        EditText edtDesc = view.findViewById(R.id.edtStepDescription);
        EditText edtTime = view.findViewById(R.id.edtStepTime);
        View btnRemove = view.findViewById(R.id.btnRemoveStep);

        // [VALIDATION HOTFIX] - Clear error on text change
        edtTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { edtTitle.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        if (data != null) {
            edtTitle.setText(data.tieu_de);
            edtDesc.setText(data.mo_ta);
            edtTime.setText(String.valueOf(data.thoi_gian_buoc));
        }

        btnRemove.setOnClickListener(v -> containerBuocNau.removeView(view));
        containerBuocNau.addView(view);
    }

    /**
     * Tải dữ liệu món ăn từ Firestore khi ở chế độ chỉnh sửa
     * @param id ID của món ăn cần tải
     */
    private void loadMonAnData(String id) {
        db.collection("mon_an").document(id).get().addOnSuccessListener(documentSnapshot -> {
            MonAn monAn = documentSnapshot.toObject(MonAn.class);
            if (monAn != null) {
                currentImageUrl = monAn.getHinh_anh(); // Lưu lại URL hiện tại để dùng nếu không thay ảnh mới
                edtTenMon.setText(monAn.getTen_mon());
                edtThoiGian.setText(String.valueOf(monAn.getThoi_gian_nau()));
                edtKhauPhan.setText(String.valueOf(monAn.getKhau_phan()));
                
                String hinhAnh = monAn.getHinh_anh();
                if (hinhAnh != null && !hinhAnh.isEmpty()) {
                    if (hinhAnh.startsWith("http")) {
                        Glide.with(this).load(hinhAnh).into(imgMonAn);
                    } else {
                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                        Glide.with(this).load(ref).into(imgMonAn);
                    }
                }

                // Thiết lập độ khó dựa trên dữ liệu tải về
                if (monAn.getDo_kho().equals("Dễ")) cgDoKho.check(R.id.chipDe);
                else if (monAn.getDo_kho().equals("Trung bình")) cgDoKho.check(R.id.chipTrungBinh);
                else if (monAn.getDo_kho().equals("Khó")) cgDoKho.check(R.id.chipKho);

                // Xóa và tải lại các danh sách động
                containerIngredients.removeAllViews();
                if (monAn.getDanh_sach_nguyen_lieu() != null) {
                    for (MonAn.ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) addIngredientRow(nl);
                }

                containerSoChe.removeAllViews();
                if (monAn.getDanh_sach_so_che() != null) {
                    for (MonAn.SoChe sc : monAn.getDanh_sach_so_che()) addSoCheRow(sc);
                }

                containerBuocNau.removeAllViews();
                if (monAn.getDanh_sach_buoc_nau() != null) {
                    for (MonAn.BuocNau bn : monAn.getDanh_sach_buoc_nau()) addBuocNauRow(bn);
                }
            }
        });
    }

    /**
     * Kiểm tra tính hợp lệ của toàn bộ dữ liệu trên form trước khi lưu
     * @return true nếu dữ liệu hợp lệ
     */
    private boolean validateAndProcessData() {
        // 1. Kiểm tra Tên món
        String tenMon = edtTenMon.getText().toString().trim();
        if (tenMon.isEmpty()) {
            edtTenMon.setError(getString(R.string.err_empty_dish_name));
            edtTenMon.requestFocus();
            return false;
        }

        // 2. Kiểm tra Thời gian
        String thoiGian = edtThoiGian.getText().toString().trim();
        if (thoiGian.isEmpty() || Integer.parseInt(thoiGian) <= 0) {
            edtThoiGian.setError(getString(R.string.err_invalid_time));
            edtThoiGian.requestFocus();
            return false;
        }

        // 3. Kiểm tra Khẩu phần
        String khauPhan = edtKhauPhan.getText().toString().trim();
        if (khauPhan.isEmpty() || Integer.parseInt(khauPhan) <= 0) {
            edtKhauPhan.setError(getString(R.string.err_invalid_portion));
            edtKhauPhan.requestFocus();
            return false;
        }

        // 4. Kiểm tra Độ khó
        if (cgDoKho.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(this, R.string.err_empty_difficulty, Toast.LENGTH_SHORT).show();
            cgDoKho.requestFocus();
            return false;
        }

        // 5. Kiểm tra dữ liệu động (Nguyên liệu, Sơ chế, Bước nấu)
        return extractAndValidateDynamicData();
    }

    /**
     * Kiểm tra chi tiết tính hợp lệ của các danh sách động (Nguyên liệu, Sơ chế, Bước nấu)
     * @return true nếu các danh sách không có ô nào trống
     */
    private boolean extractAndValidateDynamicData() {
        // Kiểm tra Nguyên liệu
        for (int i = 0; i < containerIngredients.getChildCount(); i++) {
            View v = containerIngredients.getChildAt(i);
            EditText edtName = v.findViewById(R.id.edtIngredientName);
            EditText edtQty = v.findViewById(R.id.edtIngredientQty);

            if (edtName.getText().toString().trim().isEmpty()) {
                edtName.setError(getString(R.string.err_empty_ingredient_name));
                edtName.requestFocus();
                return false;
            }
            if (edtQty.getText().toString().trim().isEmpty()) {
                edtQty.setError(getString(R.string.err_empty_ingredient_qty));
                edtQty.requestFocus();
                return false;
            }
        }

        // Kiểm tra Sơ chế
        for (int i = 0; i < containerSoChe.getChildCount(); i++) {
            View v = containerSoChe.getChildAt(i);
            EditText edtContent = v.findViewById(R.id.edtSoChe);
            if (edtContent.getText().toString().trim().isEmpty()) {
                edtContent.setError(getString(R.string.err_empty_soche_content));
                edtContent.requestFocus();
                return false;
            }
        }

        // Kiểm tra Bước nấu
        for (int i = 0; i < containerBuocNau.getChildCount(); i++) {
            View v = containerBuocNau.getChildAt(i);
            EditText edtTitle = v.findViewById(R.id.edtStepTitle);
            EditText edtDesc = v.findViewById(R.id.edtStepDescription);

            if (edtTitle.getText().toString().trim().isEmpty()) {
                edtTitle.setError(getString(R.string.err_empty_step_title));
                edtTitle.requestFocus();
                return false;
            }
            if (edtDesc.getText().toString().trim().isEmpty()) {
                edtDesc.setError(getString(R.string.err_empty_step_desc));
                edtDesc.requestFocus();
                return false;
            }
        }

        return true;
    }

    /**
     * Thực hiện quy trình nén ảnh và upload hoặc lưu dữ liệu trực tiếp
     */
    private void validateAndSave() {
        if (isSaving) return; 
        if (validateAndProcessData()) {
            isSaving = true;
            if (imageUri != null) {
                compressAndUploadImage(imageUri);
            } else if (idMonAnExisting != null) {
                saveToFirestore(idMonAnExisting, edtTenMon.getText().toString().trim(), currentImageUrl);
            } else {
                isSaving = false;
                Toast.makeText(this, R.string.err_no_image, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Nén ảnh bằng thư viện Luban trước khi upload lên Storage
     * @param uri Uri của ảnh gốc
     */
    private void compressAndUploadImage(Uri uri) {
        try {
            String path = getFilePathFromUri(uri);
            if (path == null) {
                handleUploadProcess(uri);
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
                            handleUploadProcess(Uri.fromFile(file));
                        }

                        @Override
                        public void onError(Throwable e) {
                            handleUploadProcess(uri);
                        }
                    }).launch();
        } catch (Exception e) {
            handleUploadProcess(uri);
        }
    }

    /**
     * Sao chép Uri từ content provider vào thư mục cache để lấy đường dẫn file vật lý
     * @param uri Uri cần xử lý
     * @return Đường dẫn tuyệt đối đến file tạm
     */
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
                android.util.Log.e("URI_PATH_ERR", "Lỗi copy file: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Thực hiện upload ảnh lên Firebase Storage và nhận Download URL
     * @param uri Uri của file ảnh (đã nén)
     */
    private void handleUploadProcess(Uri uri) {
        String id = (idMonAnExisting != null) ? idMonAnExisting : db.collection("mon_an").document().getId();
        String ten = edtTenMon.getText().toString().trim();
        
        // 1. Định tuyến thư mục Storage rõ ràng: mon_an/
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child("mon_an/" + fileName);

        // 2. Tải ảnh lên và lấy Download URL công khai (tránh Callback Hell bằng continueWithTask)
        storageRef.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null) throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            // 3. Lưu vào Firestore với URL thực tế
            saveToFirestore(id, ten, downloadUri.toString());
        }).addOnFailureListener(e -> {
            isSaving = false;
            android.util.Log.e("FIREBASE_STORAGE_ERR", "Upload failed: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Khởi tạo một mẫu đối tượng MonAn với các giá trị thống kê mặc định
     * @param id ID của món ăn
     * @return Đối tượng MonAn sơ khai
     */
    private MonAn createNewMonAnTemplate(String id) {
        MonAn monAn = new MonAn();
        monAn.setId_mon_an(id);
        monAn.setTen_mon(edtTenMon.getText().toString().trim());
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
        return monAn;
    }

    /**
     * Trích xuất dữ liệu từ các danh sách động trên UI và đưa vào model
     * @param monAn Đối tượng MonAn để gán dữ liệu
     */
    private void extractDynamicData(MonAn monAn) {
        // Nguyên liệu
        List<MonAn.ChiTietNguyenLieu> ingredients = new ArrayList<>();
        for (int i = 0; i < containerIngredients.getChildCount(); i++) {
            View v = containerIngredients.getChildAt(i);
            MonAn.ChiTietNguyenLieu nl = new MonAn.ChiTietNguyenLieu();
            nl.ten_nguyen_lieu = ((EditText) v.findViewById(R.id.edtIngredientName)).getText().toString().trim();
            nl.so_luong = safeParseDouble(((EditText) v.findViewById(R.id.edtIngredientQty)).getText().toString().trim());
            nl.don_vi = ((EditText) v.findViewById(R.id.edtIngredientUnit)).getText().toString().trim();
            nl.ghi_chu = ""; // Mặc định rỗng
            ingredients.add(nl);
        }
        monAn.setDanh_sach_nguyen_lieu(ingredients);

        // Sơ chế
        List<MonAn.SoChe> soCheList = new ArrayList<>();
        for (int i = 0; i < containerSoChe.getChildCount(); i++) {
            View v = containerSoChe.getChildAt(i);
            MonAn.SoChe sc = new MonAn.SoChe();
            sc.tieu_de = "Sơ chế"; // Mặc định
            sc.noi_dung = ((EditText) v.findViewById(R.id.edtSoChe)).getText().toString().trim();
            soCheList.add(sc);
        }
        monAn.setDanh_sach_so_che(soCheList);

        // Bước nấu
        List<MonAn.BuocNau> buocNauList = new ArrayList<>();
        for (int i = 0; i < containerBuocNau.getChildCount(); i++) {
            View v = containerBuocNau.getChildAt(i);
            MonAn.BuocNau bn = new MonAn.BuocNau();
            bn.so_buoc = i + 1;
            bn.tieu_de = ((EditText) v.findViewById(R.id.edtStepTitle)).getText().toString().trim();
            bn.mo_ta = ((EditText) v.findViewById(R.id.edtStepDescription)).getText().toString().trim();
            bn.thoi_gian_buoc = safeParseInt(((EditText) v.findViewById(R.id.edtStepTime)).getText().toString().trim());
            bn.hinh_anh_buoc = null; // [FIREBASE SYNC HOTFIX]
            buocNauList.add(bn);
        }
        monAn.setDanh_sach_buoc_nau(buocNauList);
    }

    /**
     * Ghi toàn bộ thông tin món ăn vào Firestore
     * @param id ID document
     * @param ten Tên món ăn
     * @param imagePath Đường dẫn ảnh (Download URL)
     */
    private void saveToFirestore(String id, String ten, String imagePath) {
        MonAn monAn = createNewMonAnTemplate(id);
        monAn.setTen_mon(ten);
        monAn.setHinh_anh(imagePath);
        monAn.setThoi_gian_nau(safeParseInt(edtThoiGian.getText().toString().trim()));
        monAn.setKhau_phan(safeParseInt(edtKhauPhan.getText().toString().trim()));
        monAn.setVung_mien("Toàn quốc"); // Mặc định
        monAn.setId_danh_muc("default_cat"); // Mặc định

        int checkedId = cgDoKho.getCheckedChipId();
        if (checkedId == R.id.chipDe) monAn.setDo_kho("Dễ");
        else if (checkedId == R.id.chipTrungBinh) monAn.setDo_kho("Trung bình");
        else if (checkedId == R.id.chipKho) monAn.setDo_kho("Khó");

        extractDynamicData(monAn);
        monAn.setTu_khoa_tim_kiem(generateKeywords(ten));

        db.collection("mon_an").document(id).set(monAn).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Lưu món ăn thành công!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            isSaving = false;
            Toast.makeText(this, "Lỗi lưu dữ liệu", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Chuyển đổi chuỗi sang số nguyên an toàn
     * @param val Chuỗi cần chuyển
     * @return Giá trị số nguyên hoặc 0 nếu lỗi
     */
    private int safeParseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    /**
     * Chuyển đổi chuỗi sang số thực an toàn
     * @param val Chuỗi cần chuyển
     * @return Giá trị số thực hoặc 0.0 nếu lỗi
     */
    private double safeParseDouble(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    /**
     * Tạo danh sách từ khóa tìm kiếm từ tên món ăn để hỗ trợ tìm kiếm trên Firestore
     * @param name Tên món ăn
     * @return Danh sách các từ khóa
     */
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

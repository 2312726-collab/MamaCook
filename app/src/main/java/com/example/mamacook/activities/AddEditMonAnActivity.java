package com.example.mamacook.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.adapters.BuocNauAdapter;
import com.example.mamacook.adapters.NguyenLieuAdapter;
import com.example.mamacook.models.DanhMuc;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.models.MonAn.BuocNau;
import com.example.mamacook.models.MonAn.ChiTietNguyenLieu;
import com.example.mamacook.models.MonAn.SoChe;
import com.example.mamacook.utils.VNCharacterUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddEditMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private MonAn currentMonAn;
    private boolean isEditMode = false;
    private Uri selectedMainImageUri = null;

    private ImageView imgMonAnForm;
    private TextView tvTitleForm;
    private EditText edtTenMon, edtThoiGianNau, edtKhauPhan;
    private AutoCompleteTextView spinnerDoKho, spinnerVungMien, spinnerDanhMuc;
    private LinearLayout containerNguyenLieu, containerSoChe, containerBuocNau;
    private Button btnSaveForm, btnSelectImg, btnCaptureImg, btnAddNguyenLieuForm, btnAddSoCheForm, btnAddBuocNauForm;
    private ImageButton btnBackForm;
    private ProgressBar progressBarForm;

    private List<DanhMuc> listDanhMuc = new ArrayList<>();
    private List<ChiTietNguyenLieu> danhSachNguyenLieu = new ArrayList<>();
    private List<SoChe> danhSachSoChe = new ArrayList<>();
    private List<BuocNau> danhSachBuocNau = new ArrayList<>();

    private static class NguyenLieuViewHolder {
        TextView tvStt;
        EditText edtTen, edtSoLuong, edtDonVi, edtGhiChu;
        NguyenLieuViewHolder(View view) {
            tvStt = view.findViewById(R.id.tv_stt_nguyen_lieu);
            edtTen = view.findViewById(R.id.edt_sub_ten_nguyen_lieu);
            edtSoLuong = view.findViewById(R.id.edt_sub_so_luong);
            edtDonVi = view.findViewById(R.id.edt_sub_don_vi);
            edtGhiChu = view.findViewById(R.id.edt_sub_ghi_chu_nguyen_lieu);
        }
    }

    private static class SoCheViewHolder {
        TextView tvStt;
        EditText edtTieuDe, edtNoiDung;
        SoCheViewHolder(View view) {
            tvStt = view.findViewById(R.id.tv_stt_so_che);
            edtTieuDe = view.findViewById(R.id.edt_sub_tieu_de_so_che);
            edtNoiDung = view.findViewById(R.id.edt_sub_noi_dung_so_che);
        }
    }

    private static class BuocNauViewHolder {
        TextView tvStt;
        EditText edtTieuDe, edtMoTa;
        BuocNauViewHolder(View view) {
            tvStt = view.findViewById(R.id.tv_stt_buoc_nau);
            edtTieuDe = view.findViewById(R.id.edt_sub_tieu_de_buoc_nau);
            edtMoTa = view.findViewById(R.id.edt_sub_mo_ta_buoc_nau);
        }
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedMainImageUri = uri;
                    imgMonAnForm.setImageURI(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    imgMonAnForm.setImageURI(selectedMainImageUri);
                }
            }
    );

    public static Intent createIntent(Context context, String monAnId) {
        Intent intent = new Intent(context, AddEditMonAnActivity.class);
        if (monAnId != null && !monAnId.isEmpty()) {
            intent.putExtra("mon_an_id", monAnId);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_mon_an);

        initViews();
        initFirebase();
        setupDropdownMenus();
        checkIntentAndInitForm();
        setupActionListeners();
    }

    private void initViews() {
        imgMonAnForm = findViewById(R.id.img_mon_an_form);
        tvTitleForm = findViewById(R.id.tv_title_form);
        edtTenMon = findViewById(R.id.edt_ten_mon);
        edtThoiGianNau = findViewById(R.id.edt_thoi_gian_nau);
        edtKhauPhan = findViewById(R.id.edt_khau_phan);
        spinnerDoKho = findViewById(R.id.spinner_do_kho);
        spinnerVungMien = findViewById(R.id.spinner_vung_mien);
        spinnerDanhMuc = findViewById(R.id.spinner_danh_muc);
        containerNguyenLieu = findViewById(R.id.container_nguyen_lieu);
        containerSoChe = findViewById(R.id.container_so_che);
        containerBuocNau = findViewById(R.id.container_buoc_nau);
        btnSaveForm = findViewById(R.id.btn_save_form);
        btnSelectImg = findViewById(R.id.btn_select_img);
        btnCaptureImg = findViewById(R.id.btn_capture_img);
        btnBackForm = findViewById(R.id.btn_back_form);
        btnAddNguyenLieuForm = findViewById(R.id.btn_add_nguyen_lieu_form);
        btnAddSoCheForm = findViewById(R.id.btn_add_so_che_form);
        btnAddBuocNauForm = findViewById(R.id.btn_add_buoc_nau_form);
        progressBarForm = findViewById(R.id.progress_bar_form);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        // Để Firebase tự động nhận diện Bucket từ file google-services.json để tránh lỗi Unknown Error
        storage = FirebaseStorage.getInstance();
    }

    private void setupDropdownMenus() {
        String[] doKho = {"Dễ", "Trung bình", "Khó"};
        ArrayAdapter<String> adapterDoKho = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, doKho);
        spinnerDoKho.setAdapter(adapterDoKho);

        String[] vungMien = {"Miền Bắc", "Miền Trung", "Miền Nam", "Quốc tế"};
        ArrayAdapter<String> adapterVungMien = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, vungMien);
        spinnerVungMien.setAdapter(adapterVungMien);

        // 🔥 ĐÃ SỬA: Gọi đúng tên bảng "danh_muc_mon" theo ảnh Firebase của bạn
        db.collection("danh_muc_mon").get().addOnSuccessListener(queryDocumentSnapshots -> {
            listDanhMuc.clear();
            List<String> tenDanhMuc = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                DanhMuc dm = doc.toObject(DanhMuc.class);
                if (dm != null) {
                    dm.setId_danh_muc(doc.getId());
                    listDanhMuc.add(dm);
                    // Dựa vào ảnh, trường tên danh mục là "ten_danh_muc", code này đã khớp:
                    if (dm.getTen_danh_muc() != null) tenDanhMuc.add(dm.getTen_danh_muc());
                }
            }

            if (tenDanhMuc.isEmpty()) {
                Toast.makeText(this, "Không có danh mục nào được tải về!", Toast.LENGTH_SHORT).show();
            }

            ArrayAdapter<String> adapterDM = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tenDanhMuc);
            spinnerDanhMuc.setAdapter(adapterDM);
            
            // Cập nhật lại giao diện nếu đang ở chế độ Edit
            if (isEditMode && currentMonAn != null && currentMonAn.getId_danh_muc() != null) {
                updateDanhMucDisplay(currentMonAn.getId_danh_muc());
            }

            spinnerDanhMuc.setInputType(android.text.InputType.TYPE_NULL);
            spinnerDanhMuc.setFocusable(false);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
        
        // --- CHÈN VÀO CUỐI HÀM setupDropdownMenus ---
        // Xử lý sự kiện click: Xóa bộ lọc (filter) cũ để luôn xổ ra 100% danh sách
        View.OnClickListener clickListener = v -> {
            AutoCompleteTextView autoComplete = (AutoCompleteTextView) v;
            
            // Xóa chữ cũ đang được lưu trong bộ lọc
            if (autoComplete.getAdapter() instanceof ArrayAdapter) {
                ((ArrayAdapter<?>) autoComplete.getAdapter()).getFilter().filter(null);
            }
            
            autoComplete.showDropDown();
        };

        spinnerDoKho.setOnClickListener(clickListener);
        spinnerVungMien.setOnClickListener(clickListener);
        spinnerDanhMuc.setOnClickListener(clickListener);
    }

    private void checkIntentAndInitForm() {
        // 1. Xác định trạng thái (Mode) chặt chẽ dựa trên Intent
        isEditMode = getIntent() != null && getIntent().hasExtra("mon_an_id");

        if (isEditMode) {
            // 2. Cấu hình UI cho chế độ CHỈNH SỬA
            tvTitleForm.setText("Chỉnh sửa món ăn");
            btnSaveForm.setText("Cập nhật");

            String dishId = getIntent().getStringExtra("mon_an_id");
            if (dishId != null && !dishId.isEmpty()) {
                setLoading(true);
                db.collection("mon_an").document(dishId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            setLoading(false);
                            if (documentSnapshot.exists()) {
                                currentMonAn = documentSnapshot.toObject(MonAn.class);
                                if (currentMonAn != null) {
                                    currentMonAn.setId_mon_an(documentSnapshot.getId());
                                    fillDataToViews(currentMonAn);
                                }
                            } else {
                                Toast.makeText(this, "Không tìm thấy dữ liệu món ăn!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        });
            } else {
                Toast.makeText(this, "ID món ăn không hợp lệ!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // 2. Cấu hình UI cho chế độ THÊM MỚI
            tvTitleForm.setText("Thêm món ăn mới");
            btnSaveForm.setText("Thêm món");
            
            // 3. Reset sạch sẽ form và container để loại bỏ dữ liệu rác
            resetFormToDefault();
        }
    }

    private void resetFormToDefault() {
        // Clear basic EditTexts
        edtTenMon.setText("");
        edtThoiGianNau.setText("");
        edtKhauPhan.setText("");
        
        // Reset AutoCompleteTextViews
        spinnerDoKho.setText("", false);
        spinnerVungMien.setText("", false);
        spinnerDanhMuc.setText("", false);
        
        // Clear dynamic containers
        containerNguyenLieu.removeAllViews();
        containerSoChe.removeAllViews();
        containerBuocNau.removeAllViews();
        
        // Reset image and URI
        imgMonAnForm.setImageResource(R.drawable.ic_nav_profile);
        selectedMainImageUri = null;
        
        // Add initial empty rows for fresh start
        addNguyenLieuFormRow(null);
        addSoCheFormRow(null, -1);
        addBuocNauFormRow(null, -1);
    }

    private void fillDataToViews(MonAn monAn) {
        edtTenMon.setText(monAn.getTen_mon());
        edtThoiGianNau.setText(String.valueOf(monAn.getThoi_gian_nau()));
        edtKhauPhan.setText(String.valueOf(monAn.getKhau_phan()));
        spinnerDoKho.setText(monAn.getDo_kho(), false);
        spinnerVungMien.setText(monAn.getVung_mien(), false);

        if (monAn.getId_danh_muc() != null) {
            updateDanhMucDisplay(monAn.getId_danh_muc());
        }

        if (monAn.getHinh_anh() != null && !monAn.getHinh_anh().isEmpty()) {
            if (monAn.getHinh_anh().startsWith("http")) {
                Glide.with(this).load(monAn.getHinh_anh()).into(imgMonAnForm);
            } else {
                storage.getReference().child(monAn.getHinh_anh()).getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            if (!isFinishing()) Glide.with(this).load(uri).into(imgMonAnForm);
                        });
            }
        }

        if (monAn.getDanh_sach_nguyen_lieu() != null) {
            for (ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) addNguyenLieuFormRow(nl);
        }
        if (monAn.getDanh_sach_so_che() != null) {
            for (SoChe sc : monAn.getDanh_sach_so_che()) addSoCheFormRow(sc, -1);
        }
        if (monAn.getDanh_sach_buoc_nau() != null) {
            for (BuocNau bn : monAn.getDanh_sach_buoc_nau()) addBuocNauFormRow(bn, -1);
        }
    }

    private void updateDanhMucDisplay(String idDanhMuc) {
        if (idDanhMuc == null || listDanhMuc.isEmpty()) return;
        for (DanhMuc dm : listDanhMuc) {
            if (dm.getId_danh_muc().equals(idDanhMuc)) {
                spinnerDanhMuc.setText(dm.getTen_danh_muc(), false); // false để không lọc adapter
                break;
            }
        }
    }

    private void setupActionListeners() {
        btnBackForm.setOnClickListener(v -> closeActivityWithAnimation());
        btnSelectImg.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCaptureImg.setOnClickListener(v -> openCamera());
        btnAddNguyenLieuForm.setOnClickListener(v -> addNguyenLieuFormRow(null));
        btnAddSoCheForm.setOnClickListener(v -> addSoCheFormRow(null, -1));
        btnAddBuocNauForm.setOnClickListener(v -> addBuocNauFormRow(null, -1));
        btnSaveForm.setOnClickListener(v -> validateAndProcessData());
    }

    private void addNguyenLieuFormRow(ChiTietNguyenLieu data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_input_nguyen_lieu, containerNguyenLieu, false);
        NguyenLieuViewHolder holder = new NguyenLieuViewHolder(view);
        view.setTag(holder);
        if (data != null) {
            holder.edtTen.setText(data.ten_nguyen_lieu);
            holder.edtSoLuong.setText(String.valueOf(data.so_luong));
            holder.edtDonVi.setText(data.don_vi);
            holder.edtGhiChu.setText(data.ghi_chu);
        }
        view.findViewById(R.id.btn_delete_nguyen_lieu).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa nguyên liệu này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        containerNguyenLieu.removeView(view);
                        recalculateSTT(containerNguyenLieu, R.id.tv_stt_nguyen_lieu);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        containerNguyenLieu.addView(view);
        recalculateSTT(containerNguyenLieu, R.id.tv_stt_nguyen_lieu);
    }

    private void addSoCheFormRow(SoChe data, int insertIndex) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_input_so_che, containerSoChe, false);
        SoCheViewHolder holder = new SoCheViewHolder(view);
        view.setTag(holder);
        if (data != null) {
            holder.edtTieuDe.setText(data.tieu_de);
            holder.edtNoiDung.setText(data.noi_dung);
        }

        // Nút thêm sơ chế vào giữa
        view.findViewById(R.id.btn_insert_so_che).setOnClickListener(v -> {
            int currentIdx = containerSoChe.indexOfChild(view);
            addSoCheFormRow(null, currentIdx + 1);
        });

        view.findViewById(R.id.btn_delete_so_che).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa mục sơ chế này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        containerSoChe.removeView(view);
                        recalculateSTT(containerSoChe, R.id.tv_stt_so_che);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        if (insertIndex == -1) {
            containerSoChe.addView(view);
        } else {
            containerSoChe.addView(view, insertIndex);
        }
        recalculateSTT(containerSoChe, R.id.tv_stt_so_che);
    }

    private void addBuocNauFormRow(BuocNau data, int insertIndex) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_input_buoc_nau, containerBuocNau, false);
        BuocNauViewHolder holder = new BuocNauViewHolder(view);
        view.setTag(holder);
        if (data != null) {
            holder.edtTieuDe.setText(data.tieu_de);
            holder.edtMoTa.setText(data.mo_ta);
        }

        // Nút thêm bước vào giữa
        view.findViewById(R.id.btn_insert_buoc_nau).setOnClickListener(v -> {
            int currentIdx = containerBuocNau.indexOfChild(view);
            addBuocNauFormRow(null, currentIdx + 1);
        });

        view.findViewById(R.id.btn_delete_buoc_nau).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa bước nấu này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        containerBuocNau.removeView(view);
                        recalculateSTT(containerBuocNau, R.id.tv_stt_buoc_nau);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        if (insertIndex == -1) {
            containerBuocNau.addView(view);
        } else {
            containerBuocNau.addView(view, insertIndex);
        }
        recalculateSTT(containerBuocNau, R.id.tv_stt_buoc_nau);
    }

    private void recalculateSTT(LinearLayout container, int tvId) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView tv = container.getChildAt(i).findViewById(tvId);
            if (tv != null) tv.setText(String.valueOf(i + 1));
        }
    }

    private void openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 100);
            return;
        }

        try {
            // Tạo file ảnh thật trong thư mục Cache của App
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            java.io.File imageFile = java.io.File.createTempFile("MamaCook_" + timeStamp, ".jpg", storageDir);
            
            // Chuyển file thành URI an toàn qua FileProvider
            selectedMainImageUri = androidx.core.content.FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", imageFile);
            
            cameraLauncher.launch(selectedMainImageUri);
        } catch (java.io.IOException e) {
            Toast.makeText(this, "Lỗi tạo file chụp ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndProcessData() {
        String ten = edtTenMon.getText().toString().trim();
        String thoiGian = edtThoiGianNau.getText().toString().trim();
        String khauPhan = edtKhauPhan.getText().toString().trim();
        String doKho = spinnerDoKho.getText().toString().trim();
        String vungMien = spinnerVungMien.getText().toString().trim();
        String danhMuc = spinnerDanhMuc.getText().toString().trim();

        if (ten.isEmpty()) {
            edtTenMon.setError("Vui lòng nhập tên món ăn");
            edtTenMon.requestFocus();
            return;
        }

        if (thoiGian.isEmpty()) {
            edtThoiGianNau.setError("Vui lòng nhập thời gian nấu");
            edtThoiGianNau.requestFocus();
            return;
        }

        if (khauPhan.isEmpty()) {
            edtKhauPhan.setError("Vui lòng nhập khẩu phần");
            edtKhauPhan.requestFocus();
            return;
        }

        if (doKho.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn độ khó", Toast.LENGTH_SHORT).show();
            return;
        }

        if (vungMien.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn vùng miền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (danhMuc.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEditMode && selectedMainImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh cho món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trích xuất dữ liệu động (nguyên liệu, sơ chế, các bước)
        if (!extractDynamicData()) return;

        setLoading(true);

        if (isEditMode) {
            String dishId = getIntent().getStringExtra("mon_an_id");
            if (dishId != null && !dishId.isEmpty()) {
                handleUploadProcess(dishId, ten);
            } else {
                setLoading(false);
                Toast.makeText(this, "Lỗi: Không tìm thấy ID để cập nhật", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Tối ưu ID: Loại bỏ dấu, ký tự đặc biệt, thay khoảng trắng bằng gạch dưới
            String normalizedId = removeAccent(ten).replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
            if (normalizedId.isEmpty()) {
                normalizedId = "dish_" + System.currentTimeMillis();
            }
            
            String finalDishId = normalizedId;
            db.collection("mon_an").document(finalDishId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    setLoading(false);
                    Toast.makeText(this, "ID món ăn đã tồn tại! Vui lòng thay đổi tên.", Toast.LENGTH_LONG).show();
                } else {
                    handleUploadProcess(finalDishId, ten);
                }
            });
        }
    }

    private boolean extractDynamicData() {
        List<ChiTietNguyenLieu> tempNL = new ArrayList<>();
        List<SoChe> tempSC = new ArrayList<>();
        List<BuocNau> tempBN = new ArrayList<>();

        // 1. Xử lý nguyên liệu
        for (int i = 0; i < containerNguyenLieu.getChildCount(); i++) {
            NguyenLieuViewHolder holder = (NguyenLieuViewHolder) containerNguyenLieu.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tenNL = holder.edtTen.getText().toString().trim();
            String soLuongStr = holder.edtSoLuong.getText().toString().trim();
            String donVi = holder.edtDonVi.getText().toString().trim();
            
            if (tenNL.isEmpty()) {
                holder.edtTen.setError("Vui lòng nhập tên nguyên liệu");
                holder.edtTen.requestFocus();
                return false;
            }
            if (soLuongStr.isEmpty()) {
                holder.edtSoLuong.setError("Nhập số lượng");
                holder.edtSoLuong.requestFocus();
                return false;
            }
            if (donVi.isEmpty()) {
                holder.edtDonVi.setError("Nhập đơn vị");
                holder.edtDonVi.requestFocus();
                return false;
            }

            ChiTietNguyenLieu nl = new ChiTietNguyenLieu();
            nl.ten_nguyen_lieu = tenNL;
            nl.so_luong = safeParseDouble(soLuongStr, 0);
            nl.don_vi = donVi;
            nl.ghi_chu = holder.edtGhiChu.getText().toString().trim(); // Ghi chú được phép để trống
            tempNL.add(nl);
        }

        // 2. Xử lý sơ chế
        for (int i = 0; i < containerSoChe.getChildCount(); i++) {
            SoCheViewHolder holder = (SoCheViewHolder) containerSoChe.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tDe = holder.edtTieuDe.getText().toString().trim();
            String nDung = holder.edtNoiDung.getText().toString().trim();
            
            if (tDe.isEmpty()) {
                holder.edtTieuDe.setError("Nhập tiêu đề sơ chế");
                holder.edtTieuDe.requestFocus();
                return false;
            }
            if (nDung.isEmpty()) {
                holder.edtNoiDung.setError("Nhập nội dung sơ chế");
                holder.edtNoiDung.requestFocus();
                return false;
            }
            
            SoChe sc = new SoChe();
            sc.tieu_de = tDe;
            sc.noi_dung = nDung;
            tempSC.add(sc);
        }

        // 3. Xử lý bước nấu
        for (int i = 0; i < containerBuocNau.getChildCount(); i++) {
            BuocNauViewHolder holder = (BuocNauViewHolder) containerBuocNau.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tDe = holder.edtTieuDe.getText().toString().trim();
            String mTa = holder.edtMoTa.getText().toString().trim();

            if (tDe.isEmpty()) {
                holder.edtTieuDe.setError("Nhập tên bước nấu");
                holder.edtTieuDe.requestFocus();
                return false;
            }
            if (mTa.isEmpty()) {
                holder.edtMoTa.setError("Nhập mô tả cách làm");
                holder.edtMoTa.requestFocus();
                return false;
            }

            BuocNau bn = new BuocNau();
            bn.so_buoc = i + 1;
            bn.tieu_de = tDe;
            bn.mo_ta = mTa;
            bn.thoi_gian_buoc = 0; // Không dùng trường thời gian ở bước này nữa
            tempBN.add(bn);
        }

        if (tempNL.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một nguyên liệu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tempBN.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm các bước thực hiện", Toast.LENGTH_SHORT).show();
            return false;
        }

        danhSachNguyenLieu = tempNL;
        danhSachSoChe = tempSC;
        danhSachBuocNau = tempBN;
        return true;
    }

    private double safeParseDouble(String text, double defaultVal) {
        try {
            String cleanText = text.trim().replace(",", ".");
            return cleanText.isEmpty() ? defaultVal : Double.parseDouble(cleanText);
        } catch (Exception e) { return defaultVal; }
    }

    private void handleUploadProcess(String id, String ten) {
        if (selectedMainImageUri != null) {
            setLoading(true);
            String oldImageUrl = (isEditMode && currentMonAn != null) ? currentMonAn.getHinh_anh() : null;

            // Nén ảnh thủ công bằng Bitmap để kiểm soát 100% đường dẫn và dung lượng
            new Thread(() -> {
                try {
                    // 1. Đọc Bitmap từ Uri
                    java.io.InputStream inputStream = getContentResolver().openInputStream(selectedMainImageUri);
                    android.graphics.Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();

                    if (originalBitmap == null) {
                        runOnUiThread(() -> uploadImageToStorage(id, ten, selectedMainImageUri, oldImageUrl, false));
                        return;
                    }

                    // 2. Tạo file tạm trong Cache
                    File compressedFile = new File(getCacheDir(), "thumb_" + id + ".jpg");
                    java.io.FileOutputStream out = new java.io.FileOutputStream(compressedFile);

                    // 3. Nén: Chất lượng 70% là mức tối ưu cho App di động (ảnh đẹp, file ~50-100KB)
                    originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out);
                    out.flush();
                    out.close();

                    // 4. Lấy URI an toàn qua FileProvider
                    Uri compressedUri = androidx.core.content.FileProvider.getUriForFile(
                            this, getPackageName() + ".fileprovider", compressedFile);

                    Log.d("MamaCook_Compress", "Nén thành công. Dung lượng: " + (compressedFile.length() / 1024) + " KB");

                    runOnUiThread(() -> uploadImageToStorage(id, ten, compressedUri, oldImageUrl, true));

                } catch (Exception e) {
                    Log.e("MamaCook_Error", "Lỗi nén ảnh, dùng ảnh gốc", e);
                    runOnUiThread(() -> uploadImageToStorage(id, ten, selectedMainImageUri, oldImageUrl, false));
                }
            }).start();

        } else if (isEditMode && currentMonAn != null) {
            saveToFirestore(id, ten, currentMonAn.getHinh_anh());
        } else {
            setLoading(false);
            Toast.makeText(this, "Vui lòng chọn hoặc chụp ảnh món ăn!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm dùng chung để upload ảnh (cả nén và gốc)
    private void uploadImageToStorage(String id, String ten, Uri uri, String oldUrl, boolean isCompressed) {
        String suffix = isCompressed ? "_compressed" : "_direct";
        String fileName = id + suffix + "_" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.StorageReference fileRef = storage.getReference().child("mon_an/" + fileName);
        
        fileRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String newImageUrl = downloadUri.toString();
                        saveToFirestore(id, ten, newImageUrl);
                        
                        // Chỉ xóa ảnh cũ khi upload ảnh mới thành công hoàn toàn
                        if (oldUrl != null && !oldUrl.isEmpty()) {
                            deleteOldImage(oldUrl);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Storage_Error", "Upload failed with uri: " + uri, e);
                    // Nếu upload file nén lỗi (thường do file:// URI), thử lại ngay với ảnh gốc (content:// URI)
                    if (isCompressed) {
                        Log.d("Storage", "Thử lại với ảnh gốc...");
                        uploadImageToStorage(id, ten, selectedMainImageUri, oldUrl, false);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isEmpty()) {
            return;
        }
        try {
            com.google.firebase.storage.StorageReference fileRef;
            if (oldImageUrl.startsWith("http")) {
                // Nếu là URL đầy đủ
                fileRef = storage.getReferenceFromUrl(oldImageUrl);
            } else {
                // Xử lý cả dấu gạch dưới và gạch ngang để tăng tỉ lệ tìm thấy ảnh
                // Nếu không thấy mon_an/banh_trang_nuong.jpg thì thử tìm mon_an/banh-trang-nuong.jpg
                fileRef = storage.getReference().child(oldImageUrl);
            }
            
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d("Storage", "Đã xóa ảnh cũ thành công"))
                    .addOnFailureListener(e -> {
                        // Nếu xóa không được (do sai tên file), thử đổi "_" sang "-" và xóa lại lần nữa
                        if (!oldImageUrl.startsWith("http") && oldImageUrl.contains("_")) {
                            String fallbackPath = oldImageUrl.replace("_", "-");
                            storage.getReference().child(fallbackPath).delete();
                        }
                        Log.e("Storage", "Lỗi xóa ảnh cũ hoặc ảnh không tồn tại: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e("Storage", "Lỗi khi xử lý xóa ảnh: " + e.getMessage());
        }
    }

    private void saveToFirestore(String id, String ten, String imgPath) {
        if (isFinishing()) return;

        MonAn monAn = isEditMode ? currentMonAn : createNewMonAnTemplate(id);
        monAn.setTen_mon(ten);
        monAn.setHinh_anh(imgPath);

        // Parse số an toàn
        monAn.setThoi_gian_nau(safeParseInt(edtThoiGianNau.getText().toString(), 0));
        monAn.setKhau_phan(safeParseInt(edtKhauPhan.getText().toString(), 0));
        
        monAn.setDo_kho(spinnerDoKho.getText().toString());
        monAn.setVung_mien(spinnerVungMien.getText().toString());
        
        // So khớp ID Danh mục
        updateMonAnDanhMuc(monAn);

        monAn.setDanh_sach_nguyen_lieu(danhSachNguyenLieu);
        monAn.setDanh_sach_so_che(danhSachSoChe);
        monAn.setDanh_sach_buoc_nau(danhSachBuocNau);
        monAn.setTu_khoa_tim_kiem(generateKeywords(ten, danhSachNguyenLieu));
        
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        if (!isEditMode) monAn.setNgay_tao(now);
        monAn.setNgay_cap_nhat(now);

        db.collection("mon_an").document(id).set(monAn)
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) return;
                    setLoading(false);
                    Toast.makeText(this, isEditMode ? "Cập nhật thành công!" : "Đã đăng món ăn thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private MonAn createNewMonAnTemplate(String id) {
        MonAn monAn = new MonAn();
        monAn.setId_mon_an(id);
        monAn.setTrang_thai("public");
        monAn.setLuot_xem(0);
        monAn.setRating(0.0);
        monAn.setReviewCount(0);
        monAn.setTotalScore(0.0);
        monAn.setGia_tien(0.0);
        return monAn;
    }

    private void updateMonAnDanhMuc(MonAn monAn) {
        String selected = spinnerDanhMuc.getText().toString().trim();
        for (DanhMuc dm : listDanhMuc) {
            if (dm.getTen_danh_muc() != null && dm.getTen_danh_muc().equalsIgnoreCase(selected)) {
                monAn.setId_danh_muc(dm.getId_danh_muc());
                return;
            }
        }
    }

    private int safeParseInt(String text, int defaultVal) {
        try {
            String cleanText = text.trim();
            return cleanText.isEmpty() ? defaultVal : Integer.parseInt(cleanText);
        } catch (Exception e) { return defaultVal; }
    }

    private List<String> generateKeywords(String name, List<ChiTietNguyenLieu> ingredients) {
        java.util.Set<String> keywords = new java.util.HashSet<>();
        
        // 1. Sinh từ khóa cho Tên món
        if (name != null && !name.isEmpty()) {
            addKeywordPermutations(keywords, name.toLowerCase());
            addKeywordPermutations(keywords, VNCharacterUtils.removeAccents(name).toLowerCase());
        }

        // 2. Sinh từ khóa cho danh sách Nguyên liệu
        if (ingredients != null) {
            for (ChiTietNguyenLieu nl : ingredients) {
                if (nl.ten_nguyen_lieu != null && !nl.ten_nguyen_lieu.isEmpty()) {
                    addKeywordPermutations(keywords, nl.ten_nguyen_lieu.toLowerCase());
                    addKeywordPermutations(keywords, VNCharacterUtils.removeAccents(nl.ten_nguyen_lieu).toLowerCase());
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private void addKeywordPermutations(java.util.Set<String> keywords, String text) {
        if (text == null || text.isEmpty()) return;
        
        // LÀM SẠCH: Loại bỏ các ký tự đặc biệt như (, ), +, ,, . và chỉ giữ lại chữ cái, số, khoảng trắng
        String cleanText = text.replaceAll("[()\\+\\.,\\-]", " ").replaceAll("\\s+", " ").trim();
        
        String[] words = cleanText.split("\\s+");
        
        // Tạo tất cả các cụm từ bắt đầu từ từng vị trí
        for (int i = 0; i < words.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < words.length; j++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(words[j]);
                keywords.add(sb.toString());
            }
        }
    }

    private String removeAccent(String str) {
        return VNCharacterUtils.removeAccents(str).toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
    }

    private void setLoading(boolean loading) {
        progressBarForm.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveForm.setEnabled(!loading);
    }

    private void closeActivityWithAnimation() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
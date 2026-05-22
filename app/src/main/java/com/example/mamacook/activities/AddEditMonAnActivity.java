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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

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
    private Button btnSaveForm, btnSelectImg, btnCaptureImg;
    private ImageButton btnBackForm, btnAddNguyenLieuForm, btnAddSoCheForm, btnAddBuocNauForm;
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
        EditText edtTieuDe, edtMoTa, edtThoiGian;
        BuocNauViewHolder(View view) {
            tvStt = view.findViewById(R.id.tv_stt_buoc_nau);
            edtTieuDe = view.findViewById(R.id.edt_sub_tieu_de_buoc_nau);
            edtMoTa = view.findViewById(R.id.edt_sub_mo_ta_buoc_nau);
            edtThoiGian = view.findViewById(R.id.edt_sub_thoi_gian_buoc);
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
        if (getIntent().hasExtra("mon_an_id")) {
            isEditMode = true;
            tvTitleForm.setText("Chỉnh sửa món ăn");
            btnSaveForm.setText("Cập nhật");

            String dishId = getIntent().getStringExtra("mon_an_id");
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
                    });
        } else {
            isEditMode = false;
            tvTitleForm.setText("Thêm món ăn mới");
            btnSaveForm.setText("Đăng món ăn");
            // Add initial empty rows
            addNguyenLieuFormRow(null);
            addSoCheFormRow(null);
            addBuocNauFormRow(null);
        }
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
            storage.getReference().child(monAn.getHinh_anh()).getDownloadUrl()
                    .addOnSuccessListener(uri -> Glide.with(this).load(uri).into(imgMonAnForm));
        }

        if (monAn.getDanh_sach_nguyen_lieu() != null) {
            for (ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) addNguyenLieuFormRow(nl);
        }
        if (monAn.getDanh_sach_so_che() != null) {
            for (SoChe sc : monAn.getDanh_sach_so_che()) addSoCheFormRow(sc);
        }
        if (monAn.getDanh_sach_buoc_nau() != null) {
            for (BuocNau bn : monAn.getDanh_sach_buoc_nau()) addBuocNauFormRow(bn);
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
        btnAddSoCheForm.setOnClickListener(v -> addSoCheFormRow(null));
        btnAddBuocNauForm.setOnClickListener(v -> addBuocNauFormRow(null));
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
            containerNguyenLieu.removeView(view);
            recalculateSTT(containerNguyenLieu, R.id.tv_stt_nguyen_lieu);
        });
        containerNguyenLieu.addView(view);
        recalculateSTT(containerNguyenLieu, R.id.tv_stt_nguyen_lieu);
    }

    private void addSoCheFormRow(SoChe data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_input_so_che, containerSoChe, false);
        SoCheViewHolder holder = new SoCheViewHolder(view);
        view.setTag(holder);
        if (data != null) {
            holder.edtTieuDe.setText(data.tieu_de);
            holder.edtNoiDung.setText(data.noi_dung);
        }
        view.findViewById(R.id.btn_delete_so_che).setOnClickListener(v -> {
            containerSoChe.removeView(view);
            recalculateSTT(containerSoChe, R.id.tv_stt_so_che);
        });
        containerSoChe.addView(view);
        recalculateSTT(containerSoChe, R.id.tv_stt_so_che);
    }

    private void addBuocNauFormRow(BuocNau data) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_input_buoc_nau, containerBuocNau, false);
        BuocNauViewHolder holder = new BuocNauViewHolder(view);
        view.setTag(holder);
        if (data != null) {
            holder.edtTieuDe.setText(data.tieu_de);
            holder.edtMoTa.setText(data.mo_ta);
            holder.edtThoiGian.setText(String.valueOf(data.thoi_gian_buoc));
        }
        view.findViewById(R.id.btn_delete_buoc_nau).setOnClickListener(v -> {
            containerBuocNau.removeView(view);
            recalculateSTT(containerBuocNau, R.id.tv_stt_buoc_nau);
        });
        containerBuocNau.addView(view);
        recalculateSTT(containerBuocNau, R.id.tv_stt_buoc_nau);
    }

    private void recalculateSTT(LinearLayout container, int tvId) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView tv = container.getChildAt(i).findViewById(tvId);
            if (tv != null) tv.setText(String.valueOf(i + 1));
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Dish");
        selectedMainImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        cameraLauncher.launch(selectedMainImageUri);
    }

    private void validateAndProcessData() {
        String ten = edtTenMon.getText().toString().trim();
        if (ten.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên món ăn", Toast.LENGTH_SHORT).show();
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
        danhSachNguyenLieu.clear();
        danhSachSoChe.clear();
        danhSachBuocNau.clear();

        // 1. Xử lý danh sách nguyên liệu
        for (int i = 0; i < containerNguyenLieu.getChildCount(); i++) {
            NguyenLieuViewHolder holder = (NguyenLieuViewHolder) containerNguyenLieu.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tenNL = holder.edtTen.getText().toString().trim();
            if (tenNL.isEmpty()) continue;

            ChiTietNguyenLieu nl = new ChiTietNguyenLieu();
            nl.ten_nguyen_lieu = tenNL;
            
            // Xử lý an toàn số lượng (hỗ trợ dấu phẩy)
            try {
                String slStr = holder.edtSoLuong.getText().toString().trim().replace(",", ".");
                nl.so_luong = slStr.isEmpty() ? 0 : Double.parseDouble(slStr);
            } catch (Exception e) {
                nl.so_luong = 0;
            }
            
            nl.don_vi = holder.edtDonVi.getText().toString().trim();
            nl.ghi_chu = holder.edtGhiChu.getText().toString().trim();
            danhSachNguyenLieu.add(nl);
        }

        // 2. Xử lý sơ chế
        for (int i = 0; i < containerSoChe.getChildCount(); i++) {
            SoCheViewHolder holder = (SoCheViewHolder) containerSoChe.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tDe = holder.edtTieuDe.getText().toString().trim();
            if (tDe.isEmpty()) continue;
            
            SoChe sc = new SoChe();
            sc.tieu_de = tDe;
            sc.noi_dung = holder.edtNoiDung.getText().toString().trim();
            danhSachSoChe.add(sc);
        }

        // 3. Xử lý bước nấu
        for (int i = 0; i < containerBuocNau.getChildCount(); i++) {
            BuocNauViewHolder holder = (BuocNauViewHolder) containerBuocNau.getChildAt(i).getTag();
            if (holder == null) continue;
            
            String tDe = holder.edtTieuDe.getText().toString().trim();
            if (tDe.isEmpty()) continue;

            BuocNau bn = new BuocNau();
            bn.so_buoc = i + 1;
            bn.tieu_de = tDe;
            bn.mo_ta = holder.edtMoTa.getText().toString().trim();
            
            // Xử lý an toàn thời gian bước
            try {
                String tgStr = holder.edtThoiGian.getText().toString().trim();
                bn.thoi_gian_buoc = tgStr.isEmpty() ? 0 : Integer.parseInt(tgStr);
            } catch (Exception e) {
                bn.thoi_gian_buoc = 0;
            }
            danhSachBuocNau.add(bn);
        }
        return true;
    }

    private void handleUploadProcess(String id, String ten) {
        if (selectedMainImageUri != null) {
            Luban.with(this)
                    .load(selectedMainImageUri)
                    .ignoreBy(100)
                    .setCompressListener(new OnCompressListener() {
                        @Override public void onStart() {}
                        @Override public void onSuccess(File file) {
                            String path = "mon_an/" + id + ".jpg";
                            storage.getReference().child(path).putFile(Uri.fromFile(file))
                                    .addOnSuccessListener(task -> saveToFirestore(id, ten, path))
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        Toast.makeText(AddEditMonAnActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                        @Override public void onError(Throwable e) {
                            setLoading(false);
                            Toast.makeText(AddEditMonAnActivity.this, "Lỗi nén ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }).launch();
        } else if (isEditMode) {
            saveToFirestore(id, ten, currentMonAn.getHinh_anh());
        } else {
            setLoading(false);
            Toast.makeText(this, "Vui lòng chọn ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore(String id, String ten, String imgPath) {
        MonAn monAn = isEditMode ? currentMonAn : new MonAn();
        monAn.setId_mon_an(id);
        monAn.setTen_mon(ten);
        monAn.setHinh_anh(imgPath);

        // Xử lý an toàn Thời gian nấu
        try {
            String val = edtThoiGianNau.getText().toString().trim();
            monAn.setThoi_gian_nau(val.isEmpty() ? 0 : Integer.parseInt(val));
        } catch (Exception e) {
            monAn.setThoi_gian_nau(0);
        }

        // Xử lý an toàn Khẩu phần
        try {
            String val = edtKhauPhan.getText().toString().trim();
            monAn.setKhau_phan(val.isEmpty() ? 0 : Integer.parseInt(val));
        } catch (Exception e) {
            monAn.setKhau_phan(0);
        }

        monAn.setDo_kho(spinnerDoKho.getText().toString());
        monAn.setVung_mien(spinnerVungMien.getText().toString());
        
        // So khớp ID Danh mục từ Text chọn
        String selectedDanhMuc = spinnerDanhMuc.getText().toString().trim();
        if (!selectedDanhMuc.isEmpty()) {
            for (DanhMuc dm : listDanhMuc) {
                if (dm.getTen_danh_muc() != null && dm.getTen_danh_muc().equalsIgnoreCase(selectedDanhMuc)) {
                    monAn.setId_danh_muc(dm.getId_danh_muc());
                    break;
                }
            }
        }

        // Đảm bảo các trường quan trọng không bị null cho món mới
        if (!isEditMode) {
            monAn.setTrang_thai("public");
            monAn.setLuot_xem(0);
            monAn.setRating(0.0);
            monAn.setReviewCount(0);
            monAn.setTotalScore(0.0);
            monAn.setGia_tien(0.0);
        }

        monAn.setDanh_sach_nguyen_lieu(danhSachNguyenLieu != null ? danhSachNguyenLieu : new ArrayList<>());
        monAn.setDanh_sach_so_che(danhSachSoChe != null ? danhSachSoChe : new ArrayList<>());
        monAn.setDanh_sach_buoc_nau(danhSachBuocNau != null ? danhSachBuocNau : new ArrayList<>());
        monAn.setTu_khoa_tim_kiem(generateKeywords(ten));
        
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        if (!isEditMode) monAn.setNgay_tao(now);
        monAn.setNgay_cap_nhat(now);

        // Sử dụng .set() để lưu dữ liệu (Hỗ trợ cả Add và Edit)
        db.collection("mon_an").document(id).set(monAn)
                .addOnSuccessListener(aVoid -> {
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

    private List<String> generateKeywords(String name) {
        List<String> keywords = new ArrayList<>();
        String normalized = removeAccent(name).toLowerCase();
        String[] words = normalized.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
            keywords.add(sb.toString());
        }
        return keywords;
    }

    private String removeAccent(String str) {
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(nfd).replaceAll("")
                .toLowerCase().replaceAll("đ", "d").replaceAll("[^a-z0-9\\s]", "").trim();
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
package com.example.mamacook.activities;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class QRCodeViewerActivity extends AppCompatActivity {

    private ImageView imgQRCode;
    private TextView tvIdMonAn;
    private MaterialButton btnShareFB, btnSaveQR, btnClose;
    private String idMonAn;
    private Bitmap qrBitmap;
    private boolean isProcessingSave = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_viewer);

        idMonAn = getIntent().getStringExtra("ID_MON_AN");
        if (idMonAn == null) {
            Toast.makeText(this, "Không tìm thấy mã món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        generateQRCode();
        setupListeners();
    }

    /**
     * Khởi tạo các thành phần giao diện từ layout XML
     */
    private void initViews() {
        imgQRCode = findViewById(R.id.imgQRCode);
        tvIdMonAn = findViewById(R.id.tvIdMonAn);
        btnShareFB = findViewById(R.id.btnShareFB);
        btnSaveQR = findViewById(R.id.btnSaveQR);
        btnClose = findViewById(R.id.btnClose);

        tvIdMonAn.setText("ID: #" + idMonAn);
    }

    /**
     * Thiết lập các sự kiện lắng nghe cho các nút bấm
     */
    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnSaveQR.setOnClickListener(v -> {
            if (qrBitmap != null) {
                saveImageToGallery(qrBitmap);
            }
        });

        btnShareFB.setOnClickListener(v -> {
            if (qrBitmap != null) {
                shareToFacebook(qrBitmap);
            }
        });
    }

    /**
     * Tạo mã QR dựa trên ID món ăn bằng thư viện ZXing
     */
    private void generateQRCode() {
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            // Dữ liệu trong QR là ID món ăn để app khác quét có thể mở trực tiếp
            BitMatrix matrix = writer.encode(idMonAn, BarcodeFormat.QR_CODE, 800, 800);
            BarcodeEncoder encoder = new BarcodeEncoder();
            qrBitmap = encoder.createBitmap(matrix);
            imgQRCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            Log.e("QRCodeError", e.getMessage());
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Lưu ảnh mã QR vào thư viện ảnh của thiết bị (hỗ trợ Android 10+ qua MediaStore)
     * @param bitmap Hình ảnh mã QR cần lưu
     */
    private void saveImageToGallery(Bitmap bitmap) {
        if (isProcessingSave) return;
        isProcessingSave = true;

        String filename = "MamaCook_QR_" + idMonAn + ".png";
        try {
            Uri imageUriToSave;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MamaCook");
                imageUriToSave = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MamaCook");
                if (!directory.exists()) directory.mkdirs();
                imageUriToSave = Uri.fromFile(new File(directory, filename));
            }

            // [LOGIC HOTFIX] Try-with-resources đảm bảo đóng stream kể cả khi crash
            try (OutputStream fos = getContentResolver().openOutputStream(imageUriToSave)) {
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    Toast.makeText(this, "Đã lưu mã QR vào thư viện", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Log.e("SaveError", e.getMessage());
            Toast.makeText(this, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
        } finally {
            isProcessingSave = false;
        }
    }

    /**
     * Chia sẻ hình ảnh mã QR lên Facebook thông qua Facebook SDK
     * @param bitmap Hình ảnh mã QR cần chia sẻ
     */
    private void shareToFacebook(Bitmap bitmap) {
        SharePhoto photo = new SharePhoto.Builder()
                .setBitmap(bitmap)
                .build();
        SharePhotoContent content = new SharePhotoContent.Builder()
                .addPhoto(photo)
                .build();

        if (ShareDialog.canShow(SharePhotoContent.class)) {
            ShareDialog.show(this, content);
        } else {
            Toast.makeText(this, "Vui lòng cài đặt ứng dụng Facebook để chia sẻ", Toast.LENGTH_SHORT).show();
        }
    }
}

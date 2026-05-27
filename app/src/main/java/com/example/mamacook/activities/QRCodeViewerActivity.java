package com.example.mamacook.activities;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

public class QRCodeViewerActivity extends AppCompatActivity {

    private String dishId;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_viewer);

        dishId = getIntent().getStringExtra("ID_MON_AN");
        if (dishId == null) {
            Toast.makeText(this, "Không tìm thấy ID món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView imgQrCode = findViewById(R.id.img_qr_code);
        TextView tvDishId = findViewById(R.id.tv_dish_id);
        Button btnSave = findViewById(R.id.btn_save_qr);
        Button btnClose = findViewById(R.id.btn_close_qr);

        tvDishId.setText("ID: " + dishId);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.encodeBitmap(dishId, BarcodeFormat.QR_CODE, 512, 512);
            imgQrCode.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }

        btnSave.setOnClickListener(v -> saveImageToGallery());
        btnClose.setOnClickListener(v -> finish());
    }

    private void saveImageToGallery() {
        if (qrBitmap == null) return;

        String filename = "MamaCook_QR_" + dishId + "_" + System.currentTimeMillis() + ".png";
        OutputStream fos;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MamaCook");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                fos = getContentResolver().openOutputStream(uri);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                if (fos != null) fos.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }

                Toast.makeText(this, "Đã lưu mã QR vào thư viện", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
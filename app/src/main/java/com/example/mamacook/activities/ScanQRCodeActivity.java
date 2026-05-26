package com.example.mamacook.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mamacook.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.InputStream;
import java.util.List;

public class ScanQRCodeActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private ImageButton btnFlash;
    private boolean isFlashOn = false;
    private View scannerLine;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    decodeQRCode(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code);

        barcodeScannerView = findViewById(R.id.barcode_scanner);
        btnFlash = findViewById(R.id.btn_flash);
        scannerLine = findViewById(R.id.scanner_line);

        // Khởi tạo CaptureManager để quản lý camera
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        
        // [HOTFIX 2] Lắng nghe kết quả quét từ Camera & Đảm bảo chạy trên UI Thread
        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                runOnUiThread(() -> handleScanResult(result.getText()));
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });

        // Nút Quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Nút Flash
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Nút Thư viện
        findViewById(R.id.btn_gallery).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Hiệu ứng quét chạy lên xuống
        startScannerAnimation();
    }

    private void startScannerAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.95f
        );
        animation.setDuration(2000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        scannerLine.startAnimation(animation);
    }

    private void toggleFlash() {
        // [HOTFIX 5] Kiểm tra phần cứng Flash tránh Crash
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(this, "Thiết bị không hỗ trợ đèn Flash", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFlashOn) {
            barcodeScannerView.setTorchOff();
            btnFlash.setImageResource(R.drawable.ic_flash_on);
        } else {
            barcodeScannerView.setTorchOn();
            btnFlash.setImageResource(R.drawable.ic_flash_off);
        }
        isFlashOn = !isFlashOn;
    }

    private boolean isHandled = false; // Ngăn chặn xử lý nhiều lần

    private void decodeQRCode(Uri uri) {
        if (isHandled) return;
        
        new Thread(() -> {
            // Sử dụng try-with-resources để tự động đóng InputStream
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                if (bitmap == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show());
                    return;
                }

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                MultiFormatReader reader = new MultiFormatReader();
                Result result = reader.decode(binaryBitmap);
                
                runOnUiThread(() -> handleScanResult(result.getText()));

            } catch (Throwable e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    String errorMsg = (e instanceof OutOfMemoryError) ? "Ảnh quá lớn, hãy chọn ảnh khác" : "Không tìm thấy mã QR";
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void handleScanResult(String result) {
        if (isHandled) return;
        
        if (result != null && !result.isEmpty()) {
            isHandled = true; // Đánh dấu đã xử lý
            Intent intent = DetailMonAnActivity.createIntent(this, result);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [HOTFIX 3] Kiểm tra quyền Camera trước khi Resume (Tránh crash khi user tắt quyền trong Settings)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            capture.onResume();
        } else {
            Toast.makeText(this, "Vui lòng cấp quyền Camera để quét mã", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }
}

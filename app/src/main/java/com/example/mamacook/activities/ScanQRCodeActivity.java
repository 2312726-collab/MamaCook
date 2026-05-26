package com.example.mamacook.activities;

import android.content.Intent;
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

import com.example.mamacook.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class ScanQRCodeActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private ImageButton btnBack, btnFlash;
    private View scannerLine;
    private boolean isFlashOn = false;
    private boolean isHandled = false;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    decodeQRCodeFromUri(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code);

        initViews();
        setupScanner();
        startScannerLineAnimation();
    }

    /**
     * Khởi tạo các thành phần giao diện và thiết lập sự kiện nút bấm
     */
    private void initViews() {
        barcodeView = findViewById(R.id.barcodeScannerView);
        btnBack = findViewById(R.id.btnBack);
        btnFlash = findViewById(R.id.btnFlash);
        scannerLine = findViewById(R.id.scannerLine);

        btnBack.setOnClickListener(v -> finish());
        
        btnFlash.setOnClickListener(v -> {
            if (isFlashOn) {
                barcodeView.setTorchOff();
                btnFlash.setImageResource(R.drawable.ic_flash_on);
            } else {
                barcodeView.setTorchOn();
                btnFlash.setImageResource(R.drawable.ic_flash_off);
            }
            isFlashOn = !isFlashOn;
        });

        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });
    }

    /**
     * Cấu hình trình quét mã QR (ZXing) và callback xử lý kết quả
     */
    private void setupScanner() {
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(com.google.zxing.BarcodeFormat.QR_CODE)));
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    handleResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {}
        });
    }

    /**
     * Bắt đầu hiệu ứng đường kẻ chạy lên xuống để mô phỏng quét laser
     */
    private void startScannerLineAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.3f,
                Animation.RELATIVE_TO_PARENT, 0.7f
        );
        animation.setDuration(2000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        scannerLine.startAnimation(animation);
    }

    /**
     * Xử lý kết quả sau khi quét được mã QR (mở màn hình chi tiết món ăn)
     * @param text Nội dung mã QR (thường là ID món ăn)
     */
    private void handleResult(String text) {
        if (isHandled || text == null || text.trim().isEmpty()) return;
        isHandled = true;

        barcodeView.pause();
        // Giả sử text quét được là ID món ăn
        Intent intent = new Intent(this, DetailMonAnActivity.class);
        intent.putExtra("ID_MON_AN", text.trim());
        startActivity(intent);
        finish();
    }

    /**
     * Giải mã QR từ một hình ảnh được chọn trong thư viện
     * @param uri Uri của hình ảnh
     */
    private void decodeQRCodeFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return;

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(binaryBitmap);
            handleResult(result.getText());

        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}

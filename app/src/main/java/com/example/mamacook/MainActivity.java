package com.example.mamacook;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    
    private TextView btnNavRegister;
    private Button btnLoginMain, btnLoginFacebook, btnLoginGoogle; // Khôi phục lại kiểu Button
    private EditText etLoginUser, etLoginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        
        // Ánh xạ thêm các trường nhập liệu
        etLoginUser = findViewById(R.id.et_login_user);
        etLoginPassword = findViewById(R.id.et_login_password);
        btnNavRegister = findViewById(R.id.btn_nav_register);
        btnLoginMain = findViewById(R.id.btn_login_main);
        btnLoginFacebook = findViewById(R.id.btn_login_facebook);
        btnLoginGoogle = findViewById(R.id.btn_login_google);

        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // XỬ LÝ ĐĂNG NHẬP CHÍNH (Email/SĐT + Password)
        if (btnLoginMain != null) {
            btnLoginMain.setOnClickListener(v -> loginUser());
        }

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        if (btnLoginFacebook != null) {
            btnLoginFacebook.setOnClickListener(v -> {
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
            });
        }

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken().getToken());
            }
            @Override public void onCancel() {}
            @Override public void onError(FacebookException error) {
                Toast.makeText(MainActivity.this, "Lỗi Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        if (btnNavRegister != null) {
            btnNavRegister.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
        }
    }

    private void loginUser() {
        String input = etLoginUser.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic nhận diện Email hay SĐT để khớp với dữ liệu lúc đăng ký
        String finalEmail = input;
        if (input.matches("\\d+")) { // Nếu là số
            finalEmail = input + "@mamacook.com";
        }

        mAuth.signInWithEmailAndPassword(finalEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Lỗi Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                saveUserToFirestore(mAuth.getCurrentUser());
            }
        });
    }

    private void handleFacebookAccessToken(String token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                saveUserToFirestore(mAuth.getCurrentUser());
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        String uid = firebaseUser.getUid();
        
        // Kiểm tra xem user đã tồn tại trong Firestore chưa trước khi lưu mới
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                User user = new User();
                user.setId_nguoi_dung(uid);
                user.setHo_ten(firebaseUser.getDisplayName());
                user.setEmail(firebaseUser.getEmail());
                user.setAnh_dai_dien(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "");
                user.setNgay_tao(Timestamp.now());
                user.setTrang_thai_tai_khoan("dang_hoat_dong");

                db.collection("nguoi_dung").document(uid).set(user);
            }
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        });
    }
}

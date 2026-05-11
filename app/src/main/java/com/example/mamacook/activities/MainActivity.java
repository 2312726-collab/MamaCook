package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.example.mamacook.models.User;
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
    
    private TextView btnNavRegister, tvForgot;
    private Button btnLoginMain, btnLoginFacebook, btnLoginGoogle;
    private EditText etLoginUser, etLoginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        
        db = FirebaseFirestore.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        
        etLoginUser = findViewById(R.id.et_login_user);
        etLoginPassword = findViewById(R.id.et_login_password);
        tvForgot = findViewById(R.id.tv_forgot_password);
        btnLoginMain = findViewById(R.id.btn_login_main);
        btnLoginFacebook = findViewById(R.id.btn_login_facebook);
        btnLoginGoogle = findViewById(R.id.btn_login_google);
        btnNavRegister = findViewById(R.id.btn_nav_register);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tvForgot.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class)));
        btnLoginMain.setOnClickListener(v -> loginUser());
        btnNavRegister.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) { handleAuth(FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken())); }
            @Override public void onCancel() {}
            @Override public void onError(FacebookException error) { Toast.makeText(MainActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void loginUser() {
        String input = etLoginUser.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        // TỰ ĐỘNG XỬ LÝ: Nếu là SĐT -> Thêm đuôi ảo để login vào Firebase Auth
        String finalEmail = input;
        if (!input.contains("@")) {
            finalEmail = input.toLowerCase() + "@mamacook.com";
        }

        mAuth.signInWithEmailAndPassword(finalEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Tài khoản hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try { handleAuth(GoogleAuthProvider.getCredential(task.getResult(ApiException.class).getIdToken(), null)); }
            catch (ApiException e) { Toast.makeText(this, "Lỗi Google: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
        }
    }

    private void handleAuth(AuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) saveUserToFirestore(mAuth.getCurrentUser());
        });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        String uid = firebaseUser.getUid();
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                User user = new User();
                user.setId_nguoi_dung(uid);
                user.setHo_ten(firebaseUser.getDisplayName());
                user.setEmail(firebaseUser.getEmail());
                user.setNgay_tao(Timestamp.now());
                user.setTrang_thai_tai_khoan("dang_hoat_dong");
                user.setVai_tro("user");
                db.collection("nguoi_dung").document(uid).set(user);
            }
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        });
    }
}

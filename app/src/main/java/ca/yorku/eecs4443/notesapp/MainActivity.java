package ca.yorku.eecs4443.notesapp;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.*;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button LoginButton;
    private EditText emailEditText, passwordEditText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //finds theme of device/theme user has picked
        ThemePreferenceHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LoginButton = findViewById(R.id.loginButton);
        emailEditText = findViewById(R.id.username);   // ADD THESE
        passwordEditText = findViewById(R.id.password);

        TextView register = findViewById(R.id.register);

        CheckBox show = findViewById(R.id.show);

        show.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show password
                passwordEditText.setTransformationMethod(
                        android.text.method.HideReturnsTransformationMethod.getInstance()
                );
            } else {
                // Hide password
                passwordEditText.setTransformationMethod(
                        android.text.method.PasswordTransformationMethod.getInstance()
                );
            }

            // Keep cursor at end after toggle
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        mAuth = FirebaseAuth.getInstance();

        // Auto-login if already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(MainActivity.this, NoteNavigation.class));
            finish();
        }


        register.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        LoginButton.setOnClickListener(v -> {

            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                emailEditText.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                passwordEditText.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            Toast.makeText(MainActivity.this,
                                    "Logged in as: " + mAuth.getCurrentUser().getEmail(),
                                    Toast.LENGTH_LONG).show();

                            startActivity(new Intent(MainActivity.this, NoteNavigation.class));
                            finish();

                        } else {

                            String errorCode = ((com.google.firebase.auth.FirebaseAuthException)
                                    task.getException()).getErrorCode();

                            switch (errorCode) {

                                case "ERROR_USER_NOT_FOUND":
                                    Toast.makeText(MainActivity.this,
                                            "No account found with this email",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case "ERROR_WRONG_PASSWORD":
                                    Toast.makeText(MainActivity.this,
                                            "Incorrect password",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case "ERROR_INVALID_CREDENTIAL":
                                    // Covers general invalid login cases
                                    Toast.makeText(MainActivity.this,
                                            "Invalid email or password",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case "ERROR_INVALID_EMAIL":
                                    Toast.makeText(MainActivity.this,
                                            "Invalid email format",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                default:
                                    Toast.makeText(MainActivity.this,
                                            "Login failed: " + errorCode,
                                            Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
        });
    }
}
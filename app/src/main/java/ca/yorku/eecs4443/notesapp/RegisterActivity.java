package ca.yorku.eecs4443.notesapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

public class RegisterActivity extends AppCompatActivity {

    private EditText username, password, confirmPassword;
    private Button registerButton;
    private CheckBox show, confirmShow;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // Link XML
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        registerButton = findViewById(R.id.registerButton);
        show = findViewById(R.id.show);
        confirmShow = findViewById(R.id.confirmShow);

        mAuth = FirebaseAuth.getInstance();

        // Show password toggle
        show.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        confirmShow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        // Register logic
        registerButton.setOnClickListener(v -> {

            String email = username.getText().toString().trim(); // using username as email
            String pass = password.getText().toString().trim();
            String confirmPass = confirmPassword.getText().toString().trim();

            // Validation
            if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase register
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();

                            // Go to main app
                            startActivity(new Intent(this, NoteNavigation.class));
                            finish();

                        } else {
                            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
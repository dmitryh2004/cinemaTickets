package com.example.cinematickets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.cinematickets.databinding.ActivityRegistrationBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RegistrationActivity extends AppCompatActivity {
    ActivityRegistrationBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
        db = FirebaseDatabase.getInstance(); // Initialize FirebaseDatabase
        users = db.getReference("Users"); // Get reference to "Users" database

        tryToAuthorize(); //попытка авторизации по предыдущему запуску программы

        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //скрыть action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();

        binding.registrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterWindow();
            }
        });

        binding.signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignInWindow();
            }
        });
    }

    private void tryToAuthorize() {
        SharedPreferences sharedPreferences = getSharedPreferences("AccountData", MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);
        String password = sharedPreferences.getString("password", null);
        if ((email == null) || (password == null))
            return;

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        users.child(auth.getCurrentUser().getUid()).child("admin").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                boolean value = snapshot.getValue(boolean.class);
                                saveAccountDataAndSwitchActivity(value);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                saveAccountDataAndSwitchActivity(false);
                                Snackbar.make(binding.root, "Не удалось загрузить данные о наличии прав администратора.", Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    private void showRegisterWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Регистрация");
        dialog.setMessage("Введите данные для регистрации");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_window = inflater.inflate(R.layout.registration_window, null);
        dialog.setView(register_window);

        final EditText email = register_window.findViewById(R.id.emailField);
        final EditText pass = register_window.findViewById(R.id.passField);
        final EditText name = register_window.findViewById(R.id.nameField);
        final EditText phone = register_window.findViewById(R.id.phoneField);

        dialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.setPositiveButton("Зарегистрироваться", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (TextUtils.isEmpty(email.getText().toString())) {
                    Snackbar.make(binding.root, "Введите электронную почту", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(name.getText().toString())) {
                    Snackbar.make(binding.root, "Введите имя", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(phone.getText().toString())) {
                    Snackbar.make(binding.root, "Введите номер телефона", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (pass.getText().toString().length() < 8) {
                    Snackbar.make(binding.root,
                            "Введите пароль (минимум 8 символов)",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                auth.createUserWithEmailAndPassword(email.getText().toString(),
                                pass.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                User user = new User();
                                user.setEmail(email.getText().toString());
                                user.setName(name.getText().toString());
                                user.setPhone(phone.getText().toString());
                                user.setAdmin(false);
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Snackbar.make(binding.root, "Вы зарегистрированы!",
                                                        Snackbar.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(binding.root, "Ошибка при регистрации: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(binding.root, "Ошибка при регистрации: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
        dialog.show();
    }

    private void showSignInWindow() {
        // Create and show the registration window dialog
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Вход в систему");
        dialog.setMessage("Введите данные для входа");

        LayoutInflater inflater = LayoutInflater.from(this);
        View sign_in_window = inflater.inflate(R.layout.sign_in_window, null);
        dialog.setView(sign_in_window);

        final EditText email = sign_in_window.findViewById(R.id.emailField);
        final EditText pass = sign_in_window.findViewById(R.id.passField);

        dialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.setPositiveButton("Войти", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (TextUtils.isEmpty(email.getText().toString())) {
                    Snackbar.make(binding.root, "Введите электронную почту", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (pass.getText().toString().length() < 5) {
                    Snackbar.make(binding.root,
                            "Введите пароль (минимум 8 символов)",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }
                auth.signInWithEmailAndPassword(email.getText().toString(),
                                pass.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                SharedPreferences sharedPreferences = getSharedPreferences("AccountData", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", email.getText().toString());
                                editor.putString("password", pass.getText().toString());
                                editor.apply();
                                users.child(auth.getCurrentUser().getUid()).child("admin").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        boolean value = snapshot.getValue(boolean.class);
                                        saveAccountDataAndSwitchActivity(value);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        saveAccountDataAndSwitchActivity(false);
                                        Snackbar.make(binding.root, "Не удалось загрузить данные о наличии прав администратора.", Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(binding.root, "Ошибка входа: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
        dialog.show();
    }

    private void saveAccountDataAndSwitchActivity(boolean is_admin) {
        SharedPreferences shPref = getSharedPreferences("Account", MODE_PRIVATE);
        SharedPreferences.Editor editor = shPref.edit();
        editor.putString("current_user_id", auth.getCurrentUser().getUid());
        editor.putBoolean("is_admin", is_admin);
        editor.apply();
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
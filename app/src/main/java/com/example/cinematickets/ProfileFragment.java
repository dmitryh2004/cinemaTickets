package com.example.cinematickets;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.cinematickets.databinding.FragmentProfileBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    FirebaseDatabase db;
    DatabaseReference users;
    StorageReference storage;
    Context context;
    User user;
    String uid = null;

    ProfileNavigation activityReference;

    public interface ProfileNavigation {
        void showBoughtTickets(String uid);
    }
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        try {
            this.activityReference = (ProfileNavigation) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must implement ProfileNavigation");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");
        storage = FirebaseStorage.getInstance().getReference();
        SharedPreferences shPref = getActivity().getSharedPreferences("Account", Context.MODE_PRIVATE);
        uid = shPref.getString("current_user_id", null);
        if (uid == null)
        {
            Snackbar.make(binding.getRoot(), "Не удалось загрузить информацию о профиле.", Snackbar.LENGTH_LONG).show();
            onBackPressed();
        }
        else {
            users.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists())
                    {
                        user = snapshot.getValue(User.class);
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        int balance = snapshot.child("balance").getValue(Integer.class);
                        user.setBalance(balance);
                        binding.profileName.setText(name);
                        binding.profileEmail.setText(email);
                        binding.profilePhone.setText(phone);
                        binding.profileBalance.setText(String.valueOf(balance));

                        StorageReference image = storage.child("profile_photos/" +
                                snapshot.getKey()
                                + "_photo.png");
                        try {
                            File localFile = File.createTempFile("images", "png");
                            image.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                    user.setPhoto(bitmap);
                                    binding.profilePhoto.setImageBitmap(bitmap);
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else {
                        Snackbar.make(binding.getRoot(), "Не удалось загрузить информацию о профиле.",
                                Snackbar.LENGTH_LONG).show();
                        onBackPressed();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            binding.showBoughtTickets.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activityReference.showBoughtTickets(uid);
                }
            });
            binding.quit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), RegistrationActivity.class);
                    SharedPreferences sharedPreferences = getActivity()
                            .getSharedPreferences("AccountData", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("email", null);
                    editor.putString("password", null);
                    editor.apply();
                    startActivity(intent);
                    getActivity().finish();
                }
            });
            binding.editProfileBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditProfileDialog();
                }
            });
            binding.profilePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onImageViewClick(v);
                }
            });
            binding.deposit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDepositClick();
                }
            });
        }
    }

    private void onDepositClick() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Пополнение баланса");
        dialog.setMessage("На данный момент доступно только пополнение по промокоду. Введите его в поле ниже:");

        LayoutInflater inflater = LayoutInflater.from(context);
        View deposit_window = inflater.inflate(R.layout.deposit_window, null);
        dialog.setView(deposit_window);

        final EditText promoField = deposit_window.findViewById(R.id.promocodeField);

        dialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton("Активировать", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String promo = promoField.getText().toString();

                DatabaseReference promos = db.getReference("promocodes");
                promos.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild(promo)) {
                            int amount = snapshot.child(promo).child("amount").getValue(Integer.class);
                            int maxUses = snapshot.child(promo).child("maxUses").getValue(Integer.class);
                            users.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                private void useUnusedPromocode() {
                                    users.child(uid).child("promocodesUsage").child(promo).setValue(1);
                                    user.setBalance(user.getBalance() + amount);
                                    users.child(uid).child("balance").setValue(user.getBalance());
                                    Snackbar.make(binding.getRoot(),
                                            "Промокод применен успешно (число использований: " + 1 + ")",
                                            Snackbar.LENGTH_LONG).show();
                                    binding.profileBalance.setText(String.valueOf(user.getBalance()));
                                }

                                private void usePromocode(int uses) {
                                    users.child(uid).child("promocodesUsage").child(promo).setValue(uses + 1);
                                    user.setBalance(user.getBalance() + amount);
                                    users.child(uid).child("balance").setValue(user.getBalance());
                                    Snackbar.make(binding.getRoot(),
                                            "Промокод применен успешно (число использований: " +
                                                    (uses + 1) + ")", Snackbar.LENGTH_LONG).show();
                                    binding.profileBalance.setText(String.valueOf(user.getBalance()));
                                }
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        if (snapshot.child("admin").getValue(Boolean.class) == false) {
                                            if (snapshot.child("promocodesUsage").exists()) {
                                                if (snapshot.child("promocodesUsage").child(promo).exists()) {
                                                    int uses = snapshot.child("promocodesUsage")
                                                            .child(promo).getValue(Integer.class);
                                                    if (uses < maxUses) {
                                                        usePromocode(uses);
                                                    }
                                                    else {
                                                        Snackbar.make(binding.getRoot(),
                                                                "Уже достигнуто максимальное число использований промокода",
                                                                Snackbar.LENGTH_LONG).show();
                                                    }
                                                }
                                                else {
                                                    useUnusedPromocode();
                                                }
                                            }
                                            else {
                                                useUnusedPromocode();
                                            }
                                        }
                                        else {
                                            user.setBalance(user.getBalance() + amount);
                                            users.child(uid).child("balance").setValue(user.getBalance());
                                            Snackbar.make(binding.getRoot(), "Промокод применен успешно",
                                                    Snackbar.LENGTH_LONG).show();
                                            binding.profileBalance.setText(String.valueOf(user.getBalance()));
                                        }
                                    }
                                    else
                                        dialog.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Snackbar.make(binding.getRoot(), "Ошибка при попытке активации промокода.",
                                            Snackbar.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            });
                        }
                        else {
                            Snackbar.make(binding.getRoot(), "Такого промокода не существует",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });
        dialog.show();
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Редактирование профиля");
        dialog.setMessage("Введите новые данные профиля");

        LayoutInflater inflater = LayoutInflater.from(context);
        View edit_profile_window = inflater.inflate(R.layout.edit_profile_window, null);
        dialog.setView(edit_profile_window);

        final EditText nameField = edit_profile_window.findViewById(R.id.nameField);
        final EditText phoneField = edit_profile_window.findViewById(R.id.phoneField);

        nameField.setText(user.getName());
        phoneField.setText(user.getPhone());

        dialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameField.getText().toString();
                String phone = phoneField.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    Snackbar.make(binding.getRoot(), "Введите имя", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(phone)) {
                    Snackbar.make(binding.getRoot(), "Введите номер телефона", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                user.setName(name);
                user.setPhone(phone);
                binding.profileName.setText(name);
                binding.profilePhone.setText(phone);
                users.child(uid).child("name").setValue(name);
                users.child(uid).child("phone").setValue(phone);
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().invalidateOptionsMenu();
    }

    private void onBackPressed() {
        if (getActivity() != null) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                    @Override
                    public void onDataReceived(List<Cinema> result) {
                        getActivity().onBackPressed();
                    }
                });
            }
        }
    }

    public void onImageViewClick(View view) {
        showImageSelectionDialog();
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Изменить изображение");
        builder.setItems(new CharSequence[]{"Сделать фото", "Выбрать из галереи"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        dispatchTakePictureIntent();
                        break;
                    case 1:
                        dispatchPickImageIntent();
                        break;
                }
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private void dispatchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                user.setPhoto(bitmap);
                binding.profilePhoto.setImageBitmap(bitmap);
                uploadImage(bitmap);
            }
            else if (requestCode == REQUEST_IMAGE_PICK) {
                if (data == null) {
                    Snackbar.make(binding.getRoot(), "Не удалось загрузить изображение.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
                InputStream inputStream = null;
                try {
                    inputStream = context.getContentResolver().openInputStream(data.getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    user.setPhoto(bitmap);
                    binding.profilePhoto.setImageBitmap(bitmap);
                    uploadImage(bitmap);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    private void uploadImage(Bitmap image) {
        StorageReference fileRef = storage.child("profile_photos/" + uid + "_photo.png");
        String filePath = null;
        try {
            // Создаем временный файл
            File tempFile = File.createTempFile("tempImage", ".png", context.getCacheDir());

            // Получаем OutputStream для файла
            FileOutputStream fos = new FileOutputStream(tempFile);

            // Сохраняем Bitmap в файл
            user.getPhoto().compress(Bitmap.CompressFormat.PNG, 100, fos);

            // Закрываем OutputStream
            fos.close();

            // Вы можете использовать tempFile для загрузки в Firebase Storage или любой другой операции с файлами
            filePath = tempFile.getAbsolutePath();

            // Вывод пути к временному файлу для дальнейшего использования
            Log.d("TempFile", "Temp file path: " + filePath);

            Uri fileUri = Uri.fromFile(new File(filePath));

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d("FirebaseStorage", "File uploaded successfully");
                    })
                    .addOnFailureListener(exception -> {
                        Log.e("FirebaseStorage", "Error uploading file", exception);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
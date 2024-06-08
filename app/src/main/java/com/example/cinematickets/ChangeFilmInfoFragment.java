package com.example.cinematickets;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cinematickets.databinding.FragmentChangeFilmInfoBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChangeFilmInfoFragment extends Fragment {
    private FragmentChangeFilmInfoBinding binding;
    private Film film;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    FirebaseDatabase db;
    StorageReference storage;
    DatabaseReference films;
    DatabaseReference cinemas;
    DatabaseReference users;

    Context context;

    public ChangeFilmInfoFragment(Film film) {
        this.film = film;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChangeFilmInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();
        films = db.getReference("films");
        cinemas = db.getReference("Cinemas");
        users = db.getReference("Users");
        storage = FirebaseStorage.getInstance().getReference();
        if (film != null) {
            binding.deleteFilm.setVisibility(View.VISIBLE);
            binding.filmNameEditText.setText(film.getName());
            binding.filmDescriptionEditText.setText(film.getDesc());
            binding.filmDescriptionURLEditText.setText(film.getDescURL());
            binding.filmShortDescEditText.setText(film.getShortDesc());
            binding.filmGenreEditText.setText(film.getGenre());
            binding.filmPublishYearEditText.setText(String.valueOf(film.getPublish_year()));
            binding.filmRatingEditText.setText(String.valueOf(film.getRating()));
            binding.changeFilmInfoPoster.setImageBitmap(film.getPoster());
        }
        else {
            film = new Film();
            binding.changeFilmInfoTitle.setText(R.string.add_film_info_title);
        }
        binding.changeFilmInfoPoster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSelectionDialog();
            }
        });
        binding.saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
        binding.discardChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        binding.deleteFilm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFilm();
            }
        });
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
                film.setPoster(bitmap);
                binding.changeFilmInfoPoster.setImageBitmap(bitmap);
            }
            else if (requestCode == REQUEST_IMAGE_PICK) {
                if (data == null) {
                    Snackbar.make(binding.getRoot(), "Не удалось загрузить изображение.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                InputStream inputStream = null;
                try {
                    inputStream = context.getContentResolver().openInputStream(data.getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    film.setPoster(bitmap);
                    binding.changeFilmInfoPoster.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public interface OnDataReceivedListener {
        void onDataReceived(String result);
    }

    private void getFirebaseFilmID(Film film, OnDataReceivedListener callback) {
        films.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String result = null;
                int maxID = 0;
                if (snapshot.exists())
                {
                    //get new film id if it not exists
                    if (film.getId() == 0)
                    {
                        for (DataSnapshot snapshot1: snapshot.getChildren())
                        {
                            if (snapshot1.exists()) {
                                int temp = snapshot1.child("id").getValue(Integer.class);
                                maxID = Math.max(maxID, temp);
                            }
                        }
                        film.setId(maxID + 1);
                    }

                    for (DataSnapshot snapshot1: snapshot.getChildren())
                    {
                        if (snapshot1.exists()) {
                            int temp = snapshot1.child("id").getValue(Integer.class);
                            if (film.getId() == temp) {
                                result = snapshot1.getKey();
                                break;
                            }
                        }
                    }
                    if (result == null) {
                        result = "film" + String.valueOf(film.getId());
                        films.child(result).setValue("new node");
                    }
                }
                callback.onDataReceived(result);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void saveChanges() {
        film.setName(binding.filmNameEditText.getText().toString());
        film.setDesc(binding.filmDescriptionEditText.getText().toString());
        film.setDescURL(binding.filmDescriptionURLEditText.getText().toString());
        film.setGenre(binding.filmGenreEditText.getText().toString());
        film.setShortDesc(binding.filmShortDescEditText.getText().toString());
        film.setPublish_year(Integer.parseInt(binding.filmPublishYearEditText.getText().toString()));
        film.setRating(Float.parseFloat(binding.filmRatingEditText.getText().toString()));
        getFirebaseFilmID(film, new OnDataReceivedListener() {
            @Override
            public void onDataReceived(String result) {
                if (result != null) {
                    films.child(result).child("id").setValue(film.getId());
                    films.child(result).child("name").setValue(film.getName());
                    films.child(result).child("desc").setValue(film.getDesc());
                    if (!film.getDescURL().isEmpty())
                        films.child(result).child("descURL").setValue(film.getDescURL());
                    films.child(result).child("genre").setValue(film.getGenre());
                    films.child(result).child("shortDesc").setValue(film.getShortDesc());
                    films.child(result).child("publish_year").setValue(film.getPublish_year());
                    films.child(result).child("rating").setValue(film.getRating());
                    StorageReference fileRef = storage.child("posters/" + result + "_poster.png");

                    if (film.getPoster() != null)
                    {
                        String filePath = null;
                        try {
                            // Создаем временный файл
                            File tempFile = File.createTempFile("tempImage", ".png", context.getCacheDir());

                            // Получаем OutputStream для файла
                            FileOutputStream fos = new FileOutputStream(tempFile);

                            // Сохраняем Bitmap в файл
                            film.getPoster().compress(Bitmap.CompressFormat.PNG, 100, fos);

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
                    onBackPressed();
                }
                else {
                    Snackbar.make(binding.getRoot(), "Ошибка при сохранении данных", Snackbar.LENGTH_LONG).show();
                    onBackPressed();
                }

            }
        });
    }

    private void deleteFilm() {
        films.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nodeName = null;
                if (snapshot.exists())
                {
                    for (DataSnapshot snapshot1: snapshot.getChildren())
                    {
                        if (snapshot1.exists()) {
                            int temp = snapshot1.child("id").getValue(Integer.class);
                            if (film.getId() == temp) {
                                nodeName = snapshot1.getKey();
                                break;
                            }
                        }
                    }
                    films.child(nodeName).removeValue();

                    // отменить все показы этого фильма и вернуть деньги владельцам билетов
                    ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                        @Override
                        public void onDataReceived(List<Cinema> result) {
                            Map<String, Integer> ticketReturn = new HashMap<>();
                            for (Cinema cinema: result) {
                                for (Show show: cinema.getShows()) {
                                    if (show.getFilm_id() == film.getId()) {
                                        for (Seat seat: show.getSeats()) {
                                            String owner = seat.getOwner();
                                            if (owner != null) {
                                                if (ticketReturn.containsKey(owner)) {
                                                    int currentReturn = ticketReturn.get(owner);
                                                    ticketReturn.put(owner, currentReturn + seat.getPrice());
                                                }
                                                else {
                                                    ticketReturn.put(owner, seat.getPrice());
                                                }
                                            }
                                        }
                                        cinemas.child("cinema" + cinema.getId()).child("shows").child("show" + show.getId()).removeValue();
                                    }
                                }
                            }
                            for (String key: ticketReturn.keySet()) {
                                users.child(key).child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            int currentBalance = snapshot.getValue(Integer.class);
                                            users.child(key).child("balance").setValue(ticketReturn.get(key) + currentBalance);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                    });
                }
                onBackPressed();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
}
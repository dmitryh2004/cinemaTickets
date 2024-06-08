package com.example.cinematickets;

import static com.example.cinematickets.MainActivity.parseDateTime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cinematickets.databinding.FragmentFilmsListBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilmsListFragment extends Fragment implements FilmsListAdapter.ItemClickListener,
FilmsListAdapter.ItemEditListener {

    public interface FilmsListNavigation {
        public void showFilmDescription(Film film);
        public void showFilmChangeInfoFragment(Film film);
    }

    FilmsListNavigation activityReference;
    private FragmentFilmsListBinding binding;
    FirebaseAuth auth;
    String uid;
    FirebaseDatabase db;
    StorageReference storage;
    DatabaseReference films;
    DatabaseReference cinemas;

    boolean current_user_is_admin = false;

    FilmsListAdapter adapter;

    List<Film> filmsList = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            activityReference = (FilmsListNavigation) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must be able to show film desc (FilmsListNavigation)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFilmsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        SharedPreferences sPref = getContext().getSharedPreferences("Account", Context.MODE_PRIVATE);
        current_user_is_admin = sPref.getBoolean("is_admin", false);
        if (current_user_is_admin) {
            binding.addCinemaBtn.setVisibility(View.VISIBLE);
        }
        uid = sPref.getString("current_user_id", null);
        auth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
        db = FirebaseDatabase.getInstance(); // Initialize FirebaseDatabase
        storage = FirebaseStorage.getInstance().getReference();
        films = db.getReference("films");
        cinemas = db.getReference("Cinemas");
        binding.addCinemaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityReference.showFilmChangeInfoFragment(null);
            }
        });
        binding.updateBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                    @Override
                    public void onDataReceived(List<Cinema> result) {
                        updateFragment();
                    }
                });
            }
        });
        ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                updateFragment();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                updateFragment();
            }
        });
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().invalidateOptionsMenu();
    }

    private void updateFragment() {

        films.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.filmsNotFoundLayout.setVisibility(View.VISIBLE);
                    binding.filmsList.setVisibility(View.GONE);
                }
                else {
                    filmsList = new ArrayList<>();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Film film = dataSnapshot.getValue(Film.class);
                        Log.d("load_films", "key: " + dataSnapshot.getKey());
                        StorageReference image = storage.child("posters/" +
                                dataSnapshot.getKey()
                                + "_poster.png");
                        try {
                            File localFile = File.createTempFile("images", "png");
                            image.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                    film.setPoster(bitmap);
                                    int position = adapter.getItemPositionByID(film.getId());
                                    adapter.replaceFilm(position, film);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        ArrayList<String> earliest = getEarliestDate(film);
                        String earliestCinema = earliest.get(1);
                        String earliestDate = earliest.get(0);

                        if (!earliestDate.equals("31.12.99 23:59")) {
                            film.setEarliestDate(LocalDateTime.parse(earliestDate, DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")));
                        }
                        if (!earliestCinema.isEmpty()) {
                            film.setEarliestCinemaID(Integer.parseInt(earliestCinema));
                        }
                        filmsList.add(film);
                    }
                    adapter = new FilmsListAdapter(getContext(), filmsList, current_user_is_admin, getCinemaList());
                    adapter.setItemClickListener(FilmsListFragment.this);
                    adapter.setItemEditListener(FilmsListFragment.this);
                    binding.filmsList.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.filmsNotFoundLayout.setVisibility(View.VISIBLE);
                binding.filmsList.setVisibility(View.GONE);
            }
        });
    }

    public List<Cinema> getCinemaList() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).cinemaList;
        }
        return null;
    }
    private ArrayList<String> getEarliestDate(Film film) {
        List<Cinema> cinemaList = getCinemaList();
        String earliestCinema = "0";
        LocalDateTime earliestDate = parseDateTime("31.12.99 23:59", "dd.MM.yy HH:mm");
        int filmID = film.getId();
        Log.d("debug", "filmID: " + filmID);
        for (Cinema cinema: cinemaList) {
            for (Show show: cinema.getShows()) {
                Log.d("debug", "cinemaName:" + cinema.getName() + ", showFilm: " + show.getFilm_id());
                if ((show.getFilm_id() == filmID) && (show.hasAvailableSeats(uid))) {
                    LocalDateTime temp = parseDateTime(show.getDate(), "dd.MM.yy HH:mm");
                    if (temp.isBefore(earliestDate) && !temp.isBefore(LocalDateTime.now())) {
                        earliestDate = temp;
                        earliestCinema = String.valueOf(cinema.getId());
                    }
                }
            }
        }
        String earliestDateTime = earliestDate.format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
        ArrayList<String> result = new ArrayList<> (Arrays.asList(earliestDateTime, earliestCinema));
        return result;
    }

    @Override
    public void onItemClick(Film clickedItem) {
        activityReference.showFilmDescription(clickedItem);
    }

    @Override
    public void onItemEditClick(Film clickedItem) {
        activityReference.showFilmChangeInfoFragment(clickedItem);
    }
}
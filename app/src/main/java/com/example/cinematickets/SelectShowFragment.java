package com.example.cinematickets;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.cinematickets.databinding.FragmentSelectShowBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SelectShowFragment extends Fragment {
    private Context context;
    private FragmentSelectShowBinding binding;
    private FirebaseDatabase db;
    private DatabaseReference users;
    private DatabaseReference films;
    private DatabaseReference cinemas;
    private Cinema cinema;
    private Show currentShow;
    private List<Show> shows;
    private List<Film> filmsList = new ArrayList<>();

    public interface SelectShowNavigation {
        void editShow(Cinema cinema, Show show);
    }

    private SelectShowNavigation activityReference;

    public SelectShowFragment(Cinema cinema) {
        this.cinema = cinema;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        try {
            this.activityReference = (SelectShowNavigation) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must implement SelectShowNavigation");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSelectShowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");
        films = db.getReference("films");
        cinemas = db.getReference("Cinemas");
        films.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                        Film film = dataSnapshot.getValue(Film.class);
                        filmsList.add(film);
                    }
                    ((MainActivity) activityReference).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                        @Override
                        public void onDataReceived(List<Cinema> result) {
                            updateFragment();
                        }
                    });
                }
                else {
                    Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.",
                            Snackbar.LENGTH_LONG).show();
                    onBackPressed();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.",
                        Snackbar.LENGTH_LONG).show();
                onBackPressed();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) activityReference).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                for (Cinema temp: result) {
                    if (temp.getId() == cinema.getId()) {
                        cinema = temp;
                        break;
                    }
                }
                updateFragment();
            }
        });
    }

    private void updateFragment() {
        binding.cinemaName.setText(cinema.getName());
        shows = cinema.getShows();
        for (Show show: shows) {
            for (Film film: filmsList) {
                if (film.getId() == show.getFilm_id()) {
                    show.setFilmName(film.getName());
                    break;
                }
            }
            for (Showroom showroom: cinema.getShowrooms()) {
                if (showroom.getId() == show.getRoom_id()) {
                    show.setShowroomName(showroom.getName());
                    break;
                }
            }
        }
        shows.sort(new Comparator<Show>() {
            @Override
            public int compare(Show o1, Show o2) {
                LocalDateTime date1 = LocalDateTime.parse(o1.getDate(), DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
                LocalDateTime date2 = LocalDateTime.parse(o2.getDate(), DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
                return date1.compareTo(date2);
            }
        });
        ShowSpinnerAdapter adapter = new ShowSpinnerAdapter(this.context,
                R.layout.show_spinner_item,
                shows,
                null);
        adapter.setDropDownViewResource(R.layout.show_spinner_dropdown_item);
        binding.ShowSpinner.setAdapter(adapter);
        binding.ShowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentShow = (Show) parent.getItemAtPosition(position);
                binding.editShowBtn.setEnabled(true);
                binding.editShowBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activityReference.editShow(cinema, currentShow);
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentShow = null;
                binding.editShowBtn.setEnabled(false);
            }
        });
        binding.addShowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityReference.editShow(cinema, null);
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
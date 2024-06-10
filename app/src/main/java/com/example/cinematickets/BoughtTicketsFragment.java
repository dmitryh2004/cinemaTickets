package com.example.cinematickets;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.AdapterView;

import com.example.cinematickets.databinding.FragmentBoughtTicketsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BoughtTicketsFragment extends Fragment {
    private FirebaseDatabase db;
    private DatabaseReference films;

    public interface BoughtTicketsNavigation {
        void showAvailableSeats(Cinema cinema, Show show);
    }
    private final String uid;
    private Film currentFilm;
    private Cinema currentCinema;
    private Show currentShow;
    
    private List<Film> filmList = new ArrayList<>();
    private List<Cinema> cinemaList = new ArrayList<>();
    private List<Show> showList = new ArrayList<>();
    private FragmentBoughtTicketsBinding binding;

    private BoughtTicketsNavigation activityReference;

    public BoughtTicketsFragment(String uid) {
        this.uid = uid;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            activityReference = (BoughtTicketsNavigation) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must implement BoughtTicketsNavigation");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBoughtTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance(); // Initialize FirebaseDatabase
        films = db.getReference("films");
        binding.showBoughtTicketsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((currentCinema == null) && (currentShow == null)) {
                    Snackbar.make(binding.getRoot(), "Выберите кинотеатр и сеанс",
                            Snackbar.LENGTH_LONG).show();
                }
                else {
                    activityReference.showAvailableSeats(currentCinema, currentShow);
                }
            }
        });
        ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                getDataFromDatabase(result);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                getDataFromDatabase(result);
            }
        });
    }
    
    private boolean hasCinema(List<Cinema> cinemaList, Cinema cinema) {
        for (Cinema item: cinemaList) {
            if (item.getId() == cinema.getId()) return true;
        }
        return false;
    }

    private boolean hasShow(List<Show> showList, Show show) {
        for (Show item: showList) {
            if (item.getId() == show.getId()) return true;
        }
        return false;
    }

    private boolean hasFilm(List<Film> filmList, Film film) {
        for (Film item: filmList) {
            if (item.getId() == film.getId()) return true;
        }
        return false;
    }

    private void getDataFromDatabase(List<Cinema> result) {
        List<Film> temp = new ArrayList<>();
        filmList = new ArrayList<>();
        cinemaList = new ArrayList<>();
        showList = new ArrayList<>();
        films.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                        Film film = dataSnapshot.getValue(Film.class);
                        if (!hasFilm(temp, film))
                            temp.add(film);
                    }
                    for (Cinema cinema: result) {
                        for (Show show: cinema.getShows()) {
                            for (Seat seat: show.getSeats()) {
                                if (seat.isSold() && (Objects.equals(seat.getOwner(), uid))) {
                                    //find film in temp
                                    for (Film film: temp) {
                                        if (show.getFilm_id() == film.getId()) {
                                            if (!hasCinema(cinemaList, cinema))
                                                cinemaList.add(cinema);
                                            if (!hasShow(showList, show))
                                                showList.add(show);
                                            if (!hasFilm(filmList, film))
                                                filmList.add(film);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (filmList.size() == 0) {
                        binding.boughtTicketsLayout.setVisibility(View.GONE);
                        binding.boughtTicketsNotFoundLayout.setVisibility(View.VISIBLE);
                    }
                    updateFragment();
                }
                else {
                    binding.boughtTicketsLayout.setVisibility(View.GONE);
                    binding.boughtTicketsNotFoundLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateFragment() {
        FilmSpinnerAdapter filmSpinnerAdapter = new FilmSpinnerAdapter(getContext(),
                R.layout.film_spinner_item, filmList);
        filmSpinnerAdapter.setDropDownViewResource(R.layout.film_spinner_dropdown_item);
        binding.filmSpinner.setAdapter(filmSpinnerAdapter);
        binding.filmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Film selectedFilm = (Film) parent.getItemAtPosition(position);
                currentFilm = selectedFilm;
                List<Cinema> cinemasWithSelectedFilm = new ArrayList<>();
                for (Cinema cinema: cinemaList) {
                    for (Show show: cinema.getShows()) {
                        if (show.getFilm_id() == currentFilm.getId()) {
                            if (!cinemasWithSelectedFilm.contains(cinema))
                                cinemasWithSelectedFilm.add(cinema);
                            break;
                        }
                    }
                }
                CinemaSpinnerAdapter cinemaSpinnerAdapter = new CinemaSpinnerAdapter(getContext(),
                        R.layout.cinema_spinner_item, cinemasWithSelectedFilm);
                cinemaSpinnerAdapter.setDropDownViewResource(R.layout.cinema_spinner_dropdown_item);
                binding.cinemaSpinner.setAdapter(cinemaSpinnerAdapter);
                binding.cinemaSpinner.setVisibility(View.VISIBLE);
                binding.cinemaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Cinema selectedCinema = (Cinema) parent.getItemAtPosition(position);
                        currentCinema = selectedCinema;
                        updateWebView();
                        List<Show> showsWithSelectedFilm = new ArrayList<>();
                        for (Show show: selectedCinema.getShows()) {
                            if (show.getFilm_id() == currentFilm.getId()) {
                                if (!showsWithSelectedFilm.contains(show))
                                    showsWithSelectedFilm.add(show);
                            }
                        }
                        ShowSpinnerAdapter showSpinnerAdapter = new ShowSpinnerAdapter(getContext(),
                                R.layout.show_spinner_item, showsWithSelectedFilm, uid);
                        showSpinnerAdapter.setDropDownViewResource(R.layout.show_spinner_dropdown_item);
                        binding.showSpinner.setAdapter(showSpinnerAdapter);
                        binding.showSpinner.setVisibility(View.VISIBLE);
                        binding.showSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                Show selectedShow = (Show) parent.getItemAtPosition(position);
                                currentShow = selectedShow;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                currentShow = null;
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        currentCinema = null;
                        binding.showSpinner.setVisibility(View.INVISIBLE);
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilm = null;
                binding.cinemaSpinner.setVisibility(View.INVISIBLE);
                binding.showSpinner.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void updateWebView() {
        float longitude = currentCinema.getLongitude();
        float latitude = currentCinema.getLatitude();
        StringBuilder builder = new StringBuilder();
        builder.append("https://yandex.ru/maps/?ll=");
        builder.append(longitude);
        builder.append("%2C");
        builder.append(latitude);
        builder.append("&mode=search&sll=");
        builder.append(longitude);
        builder.append("%2C");
        builder.append(latitude);
        builder.append("&text=");
        builder.append(latitude);
        builder.append("%2C");
        builder.append(longitude);
        builder.append("&z=16");
        String path = builder.toString();

        binding.CinemaMapView.setWebViewClient(new WebViewClient());
        binding.CinemaMapView.getSettings().setJavaScriptEnabled(true);
        binding.CinemaMapView.loadUrl(path);
    }
}
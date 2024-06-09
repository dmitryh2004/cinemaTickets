package com.example.cinematickets;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.example.cinematickets.databinding.FragmentSelectCinemaBinding;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SelectCinemaFragment extends Fragment {
    public interface SelectCinemaNavigation {
        void showAvailableSeats(Cinema cinema, Show show);
    }

    private Context context;
    FragmentSelectCinemaBinding binding;
    Film film;
    String uid;

    Cinema selectedCinema;
    Show selectedShow;

    SelectCinemaNavigation activityReference;

    SelectCinemaFragment(Film film) {
        this.film = film;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        try {
            activityReference = (SelectCinemaNavigation) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must implement SelectCinemaNavigation");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sPref = getContext().getSharedPreferences("Account", Context.MODE_PRIVATE);
        uid = sPref.getString("current_user_id", null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSelectCinemaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (film != null)
            getCinemaList(new OnCinemasLoadedCallback() {
                @Override
                public void onComplete(List<Cinema> result) {
                    updateFragment(result);
                }
            });
        else {
            Snackbar.make(view, "Не удалось загрузить информацию о фильме.", Snackbar.LENGTH_LONG).show();
            onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getCinemaList(new OnCinemasLoadedCallback() {
            @Override
            public void onComplete(List<Cinema> result) {
                updateFragment(result);
            }
        });
    }

    public interface OnCinemasLoadedCallback {
        void onComplete(List<Cinema> result);
    }
    public void getCinemaList(OnCinemasLoadedCallback callback) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                @Override
                public void onDataReceived(List<Cinema> result) {
                    callback.onComplete(result);
                }
            });
        }
    }

    public void updateFragment(List<Cinema> cinemaList) {
        ArrayList<Cinema> cinemasWithFilm = new ArrayList<>();
        if (cinemaList != null)
        {
            for (Cinema cinema: cinemaList) {
                for (Show show: cinema.getShows()) {
                    if ((show.getFilm_id() == film.getId()) && show.hasAvailableSeats(uid)) {
                        cinemasWithFilm.add(cinema);
                        break;
                    }
                }
            }
        }
        if (cinemasWithFilm.size() == 0) {
            binding.selectCinemaLayout.setVisibility(View.GONE);
            binding.selectCinemaNotFoundLayout.setVisibility(View.VISIBLE);
        }
        else {
            Spinner cinemaSpinner = binding.cinemaSpinner;
            CinemaSpinnerAdapter adapter = new CinemaSpinnerAdapter(getContext(), R.layout.cinema_spinner_item, cinemasWithFilm);
            adapter.setDropDownViewResource(R.layout.cinema_spinner_dropdown_item);
            cinemaSpinner.setAdapter(adapter);
            cinemaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Cinema selectedItem = (Cinema) parent.getItemAtPosition(position);
                    selectedCinema = selectedItem;
                    updateWebView();
                    ArrayList<Show> showsWithFilm = new ArrayList<>();
                    for (Show show: selectedItem.getShows()) {
                        if ((show.getFilm_id() == film.getId()) && show.hasAvailableSeats(uid)) {
                            showsWithFilm.add(show);
                        }
                    }
                    showsWithFilm.sort(new Comparator<Show>() {
                        @Override
                        public int compare(Show o1, Show o2) {
                            LocalDateTime date1 = LocalDateTime.parse(o1.getDate(), DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
                            LocalDateTime date2 = LocalDateTime.parse(o2.getDate(), DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
                            return date1.compareTo(date2);
                        }
                    });
                    Spinner showSpinner = binding.showSpinner;
                    ShowSpinnerAdapter adapter = new ShowSpinnerAdapter(getContext(), R.layout.show_spinner_item, showsWithFilm, uid);
                    adapter.setDropDownViewResource(R.layout.show_spinner_dropdown_item);
                    showSpinner.setAdapter(adapter);
                    showSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedShow = (Show) parent.getItemAtPosition(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedShow = null;
                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedCinema = null;
                    binding.showSpinner.setVisibility(View.INVISIBLE);
                }
            });
            binding.buyTicketsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activityReference.showAvailableSeats(selectedCinema, selectedShow);
                }
            });
        }
    }

    private void updateWebView() {
        float longitude = selectedCinema.getLongitude();
        float latitude = selectedCinema.getLatitude();
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
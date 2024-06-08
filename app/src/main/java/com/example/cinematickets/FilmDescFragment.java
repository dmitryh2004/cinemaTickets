package com.example.cinematickets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cinematickets.databinding.FragmentFilmDescBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FilmDescFragment extends Fragment {
    public interface FilmDescNavigation {
        void showSeats(Film film);
    }

    FilmDescNavigation activityReference;

    private FragmentFilmDescBinding binding;
    Film film;

    public FilmDescFragment(Film film) {
        this.film = film;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            activityReference = (FilmDescNavigation) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must be able to show film desc (FilmDescNavigation)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFilmDescBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (film != null)
            updateFragment();
        else
            Snackbar.make(view, "Не удалось загрузить информацию о фильме.", Snackbar.LENGTH_LONG).show();
    }

    public void setFilm(Film film) {
        this.film = film;
    }

    public void updateFragment() {
        binding.filmName.setText(film.getName());
        String URL = film.getDescURL();
        if (URL != null) {
            binding.showDescBtn.setVisibility(View.VISIBLE);
            binding.showDescBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                    startActivity(browserIntent);
                }
            });
        }
        binding.filmDescPublishYear.setText(String.valueOf(film.getPublish_year()));
        binding.filmDescRating.setText(String.valueOf(film.getRating()));
        binding.filmDescGenre.setText(film.getGenre());
        binding.filmDescription.setText(film.getDesc());
        binding.filmShowSeats.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 filmShowSeatsOnClick();
             }
        });

        ImageView imageView = binding.filmPoster;

        Bitmap poster = film.getPoster();
        if (poster != null)
            imageView.setImageBitmap(poster);
    }

    public void filmShowSeatsOnClick() {
        activityReference.showSeats(film);
    }
}
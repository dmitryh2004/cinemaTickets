package com.example.cinematickets;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class FilmSpinnerAdapter extends ArrayAdapter<Film> {
    List<Film> filmList;
    int resource;
    Context context;

    public FilmSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<Film> objects) {
        super(context, resource, objects);
        this.filmList = objects;
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);

        TextView tv = (TextView) (convertView.findViewById(R.id.textView));
        Film film = filmList.get(position);
        tv.setText(film.getName() + " (" + film.getGenre() + ")");

        return convertView;
    }
}

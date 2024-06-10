package com.example.cinematickets;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FilmsListAdapter extends BaseAdapter {
    public interface ItemClickListener {
        public void onItemClick(Film clickedItem);
    }

    public interface ItemEditListener {
        public void onItemEditClick(Film clickedItem);
    }
    private final Context context;
    List<Film> filmList;
    List<Cinema> cinemaList;
    private final boolean current_user_is_admin;

    ItemClickListener itemClickListener;
    ItemEditListener itemEditListener;

    public FilmsListAdapter(Context context, List<Film> filmList, boolean current_user_is_admin, List<Cinema> cinemaList) {
        this.context = context;
        this.filmList = filmList;
        this.current_user_is_admin = current_user_is_admin;
        this.cinemaList = cinemaList;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setItemEditListener(ItemEditListener itemEditListener) {
        this.itemEditListener = itemEditListener;
    }

    @Override
    public int getCount() {
        return filmList.size();
    }

    @Override
    public Film getItem(int position) {
        return filmList.get(position);
    }

    public int getItemPositionByID(int ID) {
        for (int i = 0; i < getCount(); i++) {
            Film film = filmList.get(i);
            if (film.getId() == ID)
                return i;
        }
        return -1;
    }

    public void replaceFilm(int position, Film film) {
        List<Film> temp = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) {
            if (i == position) {
                temp.add(film);
            }
            else {
                temp.add(filmList.get(i));
            }
        }
        filmList = temp;
    }

    @Override
    public long getItemId(int position) {
        return filmList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null)
            view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.films_list_item, parent, false);

        Film f = getItem(position);
        ((TextView) view.findViewById(R.id.filmName)).setText(f.getName());
        ((TextView) view.findViewById(R.id.filmDesc)).setText(f.getShortDesc());
        ((Button) view.findViewById(R.id.filmShowMore)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClick(getItem(position));
            }
        });
        ((Button) view.findViewById(R.id.editCinemaBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemEditListener.onItemEditClick(getItem(position));
            }
        });

        if (f.getEarliestDate() != null)
            ((TextView) view.findViewById(R.id.filmDate)).setText(f.getEarliestDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")));

        Cinema earliestCinema = null;
        for (Cinema cinema: cinemaList) {
            if (cinema.getId() == f.getEarliestCinemaID()) {
                earliestCinema = cinema;
                break;
            }
        }
        if (earliestCinema != null)
            ((TextView) view.findViewById(R.id.filmCinema)).setText(earliestCinema.getName());

        ImageView imageView = (ImageView) view.findViewById(R.id.filmPoster);

        Bitmap poster = f.getPoster();
        if (poster != null) {
            imageView.setImageBitmap(poster);
            ImageView placeholder = (ImageView) view.findViewById(R.id.filmPosterAnimation);
            if (placeholder.getAnimation() == null) {
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.film_image_placeholder_animation);
                anim.setFillAfter(true);
                placeholder.startAnimation(anim);
            }
            else {
                placeholder.setVisibility(View.GONE);
            }
        }
        if (current_user_is_admin)
            ((Button) view.findViewById(R.id.editCinemaBtn)).setVisibility(View.VISIBLE);

        return view;
    }
}

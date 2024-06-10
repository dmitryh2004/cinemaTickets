package com.example.cinematickets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.cinematickets.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FilmsListFragment.FilmsListNavigation,
        FilmDescFragment.FilmDescNavigation, SelectCinemaFragment.SelectCinemaNavigation,
ProfileFragment.ProfileNavigation, BoughtTicketsFragment.BoughtTicketsNavigation,
        ChooseCinemaToEditFragment.ChooseCinemaToEditNavigation,
        SelectShowFragment.SelectShowNavigation  {
    FirebaseDatabase db;
    DatabaseReference users, cinemas;
    ActivityMainBinding binding;

    List<Cinema> cinemaList = new ArrayList<>();

    FilmsListFragment filmsListFragment;
    FilmDescFragment filmDescFragment;
    ChangeFilmInfoFragment changeFilmInfoFragment;
    SelectCinemaFragment selectCinemaFragment;
    BuyTicketsFragment buyTicketsFragment;

    AuthorFragment authorFragment;
    ProgramFragment programFragment;
    ProfileFragment profileFragment;

    BoughtTicketsFragment boughtTicketsFragment;
    ChooseCinemaToEditFragment chooseCinemaToEditFragment;
    SelectShowFragment selectShowFragment;
    EditShowFragment editShowFragment;

    boolean isAdmin = false;

    private final static int
            contentViewID = 10101010;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");
        cinemas = db.getReference("Cinemas");

        filmsListFragment = new FilmsListFragment();
        loadCinemas(new onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                cinemaList = result;
            }
        });

        binding.activityMainFragmentContainer.setId(contentViewID);

        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.add(contentViewID, filmsListFragment);
        fTrans.commit();

        SharedPreferences sharedPreferences = getSharedPreferences("Account", MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("is_admin", false);
    }

    @Override
    public void showFilmDescription(Film film) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        filmDescFragment = new FilmDescFragment(film);
        fTrans.replace(contentViewID, filmDescFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public void showFilmChangeInfoFragment(Film film) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        changeFilmInfoFragment = new ChangeFilmInfoFragment(film);
        fTrans.replace(contentViewID, changeFilmInfoFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(contentViewID);
        if (!isAdmin ||
            !(currentFragment instanceof FilmsListFragment))
            menu.getItem(3).setVisible(false);
        if ((currentFragment instanceof ProfileFragment) ||
                (currentFragment instanceof AuthorFragment) ||
                (currentFragment instanceof ProgramFragment))
            menu.getItem(2).setVisible(false);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.aboutProgram)
        {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            programFragment = new ProgramFragment(isAdmin);
            fTrans.replace(contentViewID, programFragment);
            fTrans.addToBackStack(null);
            fTrans.commit();
        }
        else if (item.getItemId() == R.id.aboutAuthor) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            authorFragment = new AuthorFragment();
            fTrans.replace(contentViewID, authorFragment);
            fTrans.addToBackStack(null);
            fTrans.commit();
        }
        else if (item.getItemId() == R.id.profile) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            profileFragment = new ProfileFragment();
            fTrans.replace(contentViewID, profileFragment);
            fTrans.addToBackStack(null);
            fTrans.commit();
        }
        else if (item.getItemId() == R.id.editCinemas) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            chooseCinemaToEditFragment = new ChooseCinemaToEditFragment();
            fTrans.replace(contentViewID, chooseCinemaToEditFragment);
            fTrans.addToBackStack(null);
            fTrans.commit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showSeats(Film film) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        selectCinemaFragment = new SelectCinemaFragment(film);
        fTrans.replace(contentViewID, selectCinemaFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public void showAvailableSeats(Cinema cinema, Show show) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        buyTicketsFragment = new BuyTicketsFragment(cinema, show);
        fTrans.replace(contentViewID, buyTicketsFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public void showBoughtTickets(String uid) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        boughtTicketsFragment = new BoughtTicketsFragment(uid);
        fTrans.replace(contentViewID, boughtTicketsFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public void showShows(Cinema cinema) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        selectShowFragment = new SelectShowFragment(cinema);
        fTrans.replace(contentViewID, selectShowFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    @Override
    public void editShow(Cinema cinema, Show show) {
        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        editShowFragment = new EditShowFragment(cinema, show);
        fTrans.replace(contentViewID, editShowFragment);
        fTrans.addToBackStack(null);
        fTrans.commit();
    }

    public interface onCinemasDataReceivedCallback {
        void onDataReceived(List<Cinema> result);
    }

    void loadCinemas(onCinemasDataReceivedCallback callback) {
        List<Cinema> result = new ArrayList<>();
        cinemas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                }
                else {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Cinema temp = new Cinema();
                        temp.setId(dataSnapshot.child("id").getValue(Integer.class));
                        temp.setLongitude(dataSnapshot.child("lon").getValue(Float.class));
                        temp.setLatitude(dataSnapshot.child("lat").getValue(Float.class));
                        temp.setAddress(dataSnapshot.child("address").getValue(String.class));
                        temp.setName(dataSnapshot.child("name").getValue(String.class));

                        for (DataSnapshot dataSnapshot1: dataSnapshot.child("showrooms").getChildren()) {
                            Showroom temp1 = dataSnapshot1.getValue(Showroom.class);
                            temp.addShowroom(temp1);
                        }
                        for (DataSnapshot dataSnapshot2: dataSnapshot.child("shows").getChildren()) {
                            Show temp1 = new Show();
                            temp1.setId(dataSnapshot2.child("id").getValue(Integer.class));
                            temp1.setDate(dataSnapshot2.child("date").getValue(String.class));
                            LocalDateTime time = parseDateTime(temp1.getDate(), "dd.MM.yy HH:mm");
                            if (time.isBefore(LocalDateTime.now()))
                                continue;
                            temp1.setFilm_id(dataSnapshot2.child("film_id").getValue(Integer.class));
                            temp1.setRoom_id(dataSnapshot2.child("room_id").getValue(Integer.class));
                            for (DataSnapshot dataSnapshot3: dataSnapshot2.child("seats").getChildren()) {
                                Seat temp2 = dataSnapshot3.getValue(Seat.class);
                                temp1.addSeat(temp2);
                            }
                            temp.addShow(temp1);
                        }
                        result.add(temp);
                    }
                    cinemaList = result;
                }
                callback.onDataReceived(result);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public static LocalDateTime parseDateTime(String dateTimeString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(dateTimeString, formatter);
    }

}
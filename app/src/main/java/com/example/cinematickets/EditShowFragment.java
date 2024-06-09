package com.example.cinematickets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cinematickets.databinding.FragmentEditShowBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditShowFragment extends Fragment {
    FragmentEditShowBinding binding;
    Context context;
    Cinema cinema;
    Show show;
    List<Film> filmList = new ArrayList<>();
    private FirebaseDatabase db;
    private DatabaseReference users, films, cinemas;
    private Film selectedFilm = null;
    private Showroom selectedShowroom = null;
    String uid;
    private int rows = -1, cols = -1;
    private int currentSeatRow = -1, currentSeatCol = -1;
    private boolean seatsRendered = false;
    private Resources.Theme theme;
    private List<List<ImageView>> seatsView;
    private List<User> userList = new ArrayList<>();

    public EditShowFragment(Cinema cinema, Show show) {
        this.cinema = cinema;
        this.show = show;
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
        binding = FragmentEditShowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        SharedPreferences shPref = getContext().getSharedPreferences("Account", Context.MODE_PRIVATE);
        uid = shPref.getString("current_user_id", null);
        if (uid == null) {
            Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.", Snackbar.LENGTH_LONG).show();
            onBackPressed();
        }

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
                        filmList.add(film);
                    }
                    updateFragment();
                }
                else {
                    Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.", Snackbar.LENGTH_LONG).show();
                    onBackPressed();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.", Snackbar.LENGTH_LONG).show();
                onBackPressed();
            }
        });
        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        userList.add(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateFragment() {
        if (show == null) {
            binding.deleteShowBtn.setVisibility(View.GONE);
            loadAddShowFragment();
        }
        else {
            for (Film film: filmList) {
                if (show.getFilm_id() == film.getId()) {
                    selectedFilm = film;
                    break;
                }
            }
            for (Showroom showroom: cinema.getShowrooms()) {
                if (showroom.getId() == show.getRoom_id()) {
                    selectedShowroom = showroom;
                    break;
                }
            }
            if ((selectedFilm == null) || (selectedShowroom == null))
            {
                Snackbar.make(binding.getRoot(), "Произошла ошибка при загрузке данных.", Snackbar.LENGTH_LONG).show();
                onBackPressed();
            }
            loadEditShowFragment();
        }
    }

    private void loadEditShowFragment() {
        binding.seatsHSV.sv = binding.seatsVSV;
        binding.editExistingShowLayout.setVisibility(View.VISIBLE);
        this.rows = selectedShowroom.getRows();
        this.cols = selectedShowroom.getCols();
        if (!seatsRendered) populateLinearLayout(binding.CinemaSeatsLayout, rows, cols);
        else updateLinearLayout(rows, cols);
        binding.changeShowDateEditText.setText(show.getDate());
        binding.cancelChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        binding.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //проверка на правильность даты
                String dateTimeInput = binding.changeShowDateEditText.getText().toString();
                try {
                    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
                    LocalDateTime time = LocalDateTime.parse(dateTimeInput, timeFormat);
                    if (time.isBefore(LocalDateTime.now())) {
                        Snackbar.make(binding.getRoot(), "Вы не можете запланировать показ в прошлом.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    show.setDate(dateTimeInput);

                    //сохранение в бд
                    DatabaseReference showRef = cinemas.child("cinema" + cinema.getId()).child("shows").child("show" + show.getId());
                    showRef.child("id").setValue(show.getId());
                    showRef.child("room_id").setValue(show.getRoom_id());
                    showRef.child("film_id").setValue(show.getFilm_id());
                    showRef.child("date").setValue(show.getDate());
                    for (Seat seat: show.getSeats()) {
                        DatabaseReference seatRef = showRef.child("seats").child("seat" + seat.getId());
                        seatRef.setValue(seat);
                    }
                    Snackbar.make(binding.getRoot(), "Показ сохранен успешно!", Snackbar.LENGTH_LONG).show();
                    onBackPressed();
                }
                catch (DateTimeParseException e) {
                    Snackbar.make(binding.getRoot(), "Введенная дата не соответствует шаблону",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
        binding.deleteShowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDialog confirmDialog = new ConfirmDialog(context, new ConfirmDialog.ConfirmDialogCallback() {
                    @Override
                    public void onConfirmation() {
                        Map<String, Integer> ticketReturn = new HashMap<>();
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
                        for (Show temp: cinema.getShows()) {
                            if (show.getId() == temp.getId()) {
                                cinema.getShows().remove(temp);
                                break;
                            }
                        }
                        cinemas.child("cinema" + cinema.getId()).child("shows").child("show" + show.getId()).removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                Snackbar.make(binding.getRoot(), "Показ удален успешно. Деньги за билеты возвращены пользователям.", Snackbar.LENGTH_LONG).show();
                                onBackPressed();
                            }
                        });
                    }
                });
                confirmDialog.showDialog("Подтвердите удаление", "Вы действительно хотите удалить этот сеанс?");
            }
        });
    }

    private void loadAddShowFragment() {
        binding.addNewShowLayout.setVisibility(View.VISIBLE);
        FilmSpinnerAdapter filmSpinnerAdapter = new FilmSpinnerAdapter(context,
                R.layout.film_spinner_item,
                filmList);
        filmSpinnerAdapter.setDropDownViewResource(R.layout.film_spinner_dropdown_item);
        binding.filmSpinner.setAdapter(filmSpinnerAdapter);
        binding.filmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilm = (Film) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFilm = null;
            }
        });
        ShowroomSpinnerAdapter showroomSpinnerAdapter = new ShowroomSpinnerAdapter(context,
                R.layout.showroom_spinner_item,
                cinema.getShowrooms());
        showroomSpinnerAdapter.setDropDownViewResource(R.layout.showroom_spinner_dropdown_item);
        binding.showroomSpinner.setAdapter(showroomSpinnerAdapter);
        binding.showroomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedShowroom = (Showroom) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedShowroom = null;
            }
        });
        binding.addShowNextStep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String basePriceInput = binding.basePriceEditText.getText().toString();
                String dateTimeInput = binding.showDateEditText.getText().toString();
                try {
                    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
                    LocalDateTime time = LocalDateTime.parse(dateTimeInput, timeFormat);
                    if (time.isBefore(LocalDateTime.now())) {
                        Snackbar.make(binding.getRoot(), "Вы не можете запланировать показ в прошлом.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    int basePrice = Integer.parseInt(basePriceInput);
                    if (basePrice <= 0) {
                        Snackbar.make(binding.getRoot(), "Цена по умолчанию должна быть положительным целым числом.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    show = new Show();
                    int largestId = 0;
                    for (Show temp: cinema.getShows()) {
                        largestId = Math.max(largestId, temp.getId());
                    }
                    show.setId(largestId + 1);
                    show.setFilm_id(selectedFilm.getId());
                    show.setRoom_id(selectedShowroom.getId());
                    show.setDate(dateTimeInput);
                    // generate seats
                    int rows = selectedShowroom.getRows();
                    int cols = selectedShowroom.getCols();
                    List<Seat> seats = new ArrayList<>();
                    for (int i = 0; i < rows; i++) {
                        for (int j = 0; j < cols; j++) {
                            Seat temp = new Seat();
                            temp.setId(seats.size() + 1);
                            temp.setSold(false);
                            temp.setRow(i + 1);
                            temp.setCol(j + 1);
                            temp.setPrice(basePrice);
                            seats.add(temp);
                        }
                    }
                    show.setSeats(seats);

                    //switch frames
                    binding.addNewShowLayout.setVisibility(View.GONE);
                    loadEditShowFragment();
                }
                catch (DateTimeParseException e) {
                    Snackbar.make(binding.getRoot(), "Введенная дата не соответствует шаблону",
                            Snackbar.LENGTH_LONG).show();
                }
                catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), "Введена неправильная цена по умолчанию",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
        binding.cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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

    private void populateLinearLayout(LinearLayout parentLayout, int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        int paddingInDp = 5; // отступ в dp
        final float scale = context.getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f); // перевод dp в пиксели
        theme = context.getTheme();
        seatsView = new ArrayList<>();

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerLayoutParams);
        for (int i = 0; i < cols + 1; i++) {
            TextView textView = new TextView(context);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    75, 75
            );
            if (i > 0)
                textView.setText(String.valueOf(i));
            textParams.setMargins(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
            textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            textView.setLayoutParams(textParams);
            headerLayout.addView(textView);
        }
        parentLayout.addView(headerLayout);
        for (int i = 0; i < rows; i++) {
            seatsView.add(new ArrayList<ImageView>());
            LinearLayout innerLayout = new LinearLayout(context);
            innerLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            innerLayout.setLayoutParams(layoutParams);

            TextView textView = new TextView(context);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    75, 75
            );
            textView.setText(String.valueOf(i + 1));
            textParams.setMargins(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
            textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            textView.setLayoutParams(textParams);
            innerLayout.addView(textView);
            for (int j = 0; j < cols; j++) {
                ImageView imageView = new ImageView(context);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                        75, 75
                );
                imageParams.setMargins(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
                imageView.setLayoutParams(imageParams);
                Seat seat = null;
                int index = 0;
                for (Seat temp: show.getSeats()) {
                    if ((temp.row == i + 1) && (temp.col == j + 1)) {
                        seat = temp;
                        index = show.getSeats().indexOf(seat);
                    }
                }
                if ((i == currentSeatRow) && (j == currentSeatCol)) {
                    if (seat.isSold()) {
                        if (seat.getOwner().equals(uid)) {
                            imageView.setBackgroundColor(R.drawable.seat_border_sold);
                        }
                        else {
                            imageView.setBackgroundColor(R.drawable.seat_border_sold_other_owner);
                        }
                    }
                    else
                        imageView.setBackgroundResource(R.drawable.seat_border_not_sold);
                }
                else {
                    if (seat.isSold()) {
                        if (seat.getOwner().equals(uid)) {
                            imageView.setBackgroundColor(getResources().getColor(R.color.yellow, theme));
                        }
                        else {
                            imageView.setBackgroundColor(getResources().getColor(R.color.red, theme));
                        }
                    }
                    else
                        imageView.setBackgroundColor(getResources().getColor(R.color.green, theme));
                }
                int row = i, col = j, seatIndex = index;
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectSeat(row, col, seatIndex);
                    }
                });
                innerLayout.addView(imageView);
                seatsView.get(i).add(imageView);
            }

            parentLayout.addView(innerLayout);
        }
        seatsRendered = true;
    }

    private void updateLinearLayout(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                ImageView imageView = seatsView.get(i).get(j);
                Seat seat = null;
                int index = 0;
                for (Seat temp: show.getSeats()) {
                    if ((temp.row == i + 1) && (temp.col == j + 1)) {
                        seat = temp;
                        index = show.getSeats().indexOf(seat);
                    }
                }
                if ((i == currentSeatRow) && (j == currentSeatCol)) {
                    if (seat.isSold()) {
                        if (seat.getOwner().equals(uid)) {
                            imageView.setBackgroundColor(R.drawable.seat_border_sold);
                        }
                        else {
                            imageView.setBackgroundColor(R.drawable.seat_border_sold_other_owner);
                        }
                    }
                    else
                        imageView.setBackgroundResource(R.drawable.seat_border_not_sold);
                }
                else {
                    if (seat.isSold()) {
                        if (seat.getOwner().equals(uid)) {
                            imageView.setBackgroundColor(getResources().getColor(R.color.yellow, theme));
                        }
                        else {
                            imageView.setBackgroundColor(getResources().getColor(R.color.red, theme));
                        }
                    }
                    else
                        imageView.setBackgroundColor(getResources().getColor(R.color.green, theme));
                }
                int row = i, col = j, seatIndex = index;
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectSeat(row, col, seatIndex);
                    }
                });
            }
        }
    }

    private void selectSeat(int row, int col, int seatIndex) {
        Seat seat = show.getSeats().get(seatIndex);
        int price = seat.getPrice();
        String owner = seat.getOwner();
        currentSeatRow = row;
        currentSeatCol = col;
        updateLinearLayout(rows, cols);
        if (owner != null)
        {
            users.child(owner).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String ownerName = snapshot.getValue(String.class);
                        binding.seatOwner.setText(ownerName);
                    }
                    else {
                        binding.seatOwner.setText(owner);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        else {
            binding.seatOwner.setText(R.string.editShowOwnerDefault);
        }

        binding.chosenSeat.setText("ряд " + (currentSeatRow + 1) + ", место " + (currentSeatCol + 1));
        binding.seatPriceEditText.setText(String.valueOf(price));
        binding.savePriceBtn.setEnabled(true);
        binding.annulateTicketBtn.setEnabled(true);

        binding.savePriceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String priceInput = binding.seatPriceEditText.getText().toString();
                    int price = Integer.parseInt(priceInput);
                    if (price > 0) {
                        show.getSeats().get(seatIndex).setPrice(price);
                        Snackbar.make(binding.getRoot(), "Цена места (ряд " + (currentSeatRow + 1) + ", место " + (currentSeatCol + 1) + ") успешно изменена.",
                                Snackbar.LENGTH_LONG).show();
                        updateFragment();
                    }
                    else
                        Snackbar.make(binding.getRoot(), "Цена должна быть положительным целым числом.",
                                Snackbar.LENGTH_LONG).show();
                }
                catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), "Неправильно указана цена места.",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
        binding.annulateTicketBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDialog confirmDialog = new ConfirmDialog(context, new ConfirmDialog.ConfirmDialogCallback() {
                    @Override
                    public void onConfirmation() {
                        show.getSeats().get(seatIndex).setOwner(null);
                        show.getSeats().get(seatIndex).setSold(false);
                        Snackbar.make(binding.getRoot(), "Билет на место (ряд " + (currentSeatRow + 1) + ", место " + (currentSeatCol + 1) + ") аннулирован. Сохраните показ для применения изменений.",
                                Snackbar.LENGTH_LONG).show();
                        updateFragment();
                    }
                });
                confirmDialog.showDialog("Подтвердите действие", "Вы действительно хотите аннулировать билет на место (ряд " + (currentSeatRow + 1) + ", место " + (currentSeatCol + 1) + ")?");
            }
        });
    }
}
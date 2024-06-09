package com.example.cinematickets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cinematickets.databinding.FragmentBuyTicketsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BuyTicketsFragment extends Fragment {
    FirebaseDatabase db;
    DatabaseReference users;
    DatabaseReference cinemas;
    User user;
    String uid;
    private Cinema cinema;
    private Show show;
    private Context context;
    FragmentBuyTicketsBinding binding;
    List<List<ImageView>> seatsView;
    int currentSeatRow = -1, currentSeatCol = -1;
    int rows = 0, cols = 0;
    private boolean seatsRendered = false;
    Resources.Theme theme;
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
                    if (seat.isSold())
                        imageView.setBackgroundResource(R.drawable.seat_border_sold);
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
                if (!(seat.isSold() && !seat.getOwner().equals(uid))) {
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectSeat(row, col, seatIndex);
                        }
                    });
                }
                innerLayout.addView(imageView);
                seatsView.get(i).add(imageView);
            }

            parentLayout.addView(innerLayout);
        }
        seatsRendered = true;
    }

    BuyTicketsFragment(Cinema cinema, Show show) {
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
        binding = FragmentBuyTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();
        SharedPreferences shPref = getContext().getSharedPreferences("Account", Context.MODE_PRIVATE);
        uid = shPref.getString("current_user_id", "");
        users = db.getReference("Users");
        users.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                    int balance = snapshot.child("balance").getValue(Integer.class);
                    user.setBalance(balance);
                    binding.buyTicketsYourBalance.setText(String.valueOf(balance));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        cinemas = db.getReference("Cinemas");
        if ((cinema != null) && (show != null))
            updateFragment();
        else {
            Snackbar.make(view, "Не удалось загрузить информацию о показе фильма.", Snackbar.LENGTH_LONG).show();
            onBackPressed();
        }
    }

    private interface OnDatabaseReadCallback {
        void onComplete(Cinema result);
    }

    private interface OnDatabaseWriteCallback {
        void onComplete();
    }

    private void readFromDatabase(OnDatabaseReadCallback callback) {
        final List<Cinema>[] cinemasList = new List[]{new ArrayList<Cinema>()};
        ((MainActivity) getActivity()).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                cinemasList[0] = result;
                for (Cinema cinemaInstance: cinemasList[0]) {
                    if (cinemaInstance.getId() == cinema.getId()) {
                        callback.onComplete(cinemaInstance);
                    }
                }
            }
        });
    }
    private void writeToDatabase(OnDatabaseWriteCallback callback) {
        DatabaseReference cinemaReference = cinemas.child("cinema" + cinema.getId());
        DatabaseReference showReference = cinemaReference.child("shows").child("show" + show.getId());
        List<Seat> seats = show.getSeats();
        for (Seat seat: seats)
        {
            DatabaseReference seatReference = showReference.child("seats").child("seat" + seat.getId());
            seatReference.setValue(seat);
        }
        users.child(uid).child("balance").setValue(user.getBalance());
        callback.onComplete();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFragment();
    }

    private void updateFragment() {
        readFromDatabase(new OnDatabaseReadCallback() {
            @Override
            public void onComplete(Cinema result) {
                boolean updated = false;
                cinema = result;
                for (Show showInstance: cinema.getShows()) {
                    if (showInstance.getId() == show.getId()) {
                        show = showInstance;
                        updated = true;
                    }
                }
                if (!updated) {
                    Snackbar.make(binding.getRoot(), "Не удалось загрузить информацию о показе фильма.", Snackbar.LENGTH_LONG).show();
                    onBackPressed();
                }
                else {
                    int cols = 0;
                    int rows = 0;
                    String showroomName = "";
                    int showroomId = show.getRoom_id();
                    for (Showroom showroom: cinema.getShowrooms()) {
                        if (showroomId == showroom.getId()) {
                            rows = showroom.getRows();
                            cols = showroom.getCols();
                            showroomName = showroom.getName();
                            break;
                        }
                    }
                    if ((cols == 0) || (rows == 0)) {
                        Snackbar.make(binding.getRoot(), "Не удалось загрузить информацию о показе фильма.", Snackbar.LENGTH_LONG).show();
                        onBackPressed();
                    }
                    else {
                        binding.seatsHSV.sv = binding.seatsVSV;
                        binding.buyTicketsYourBalance.setText(String.valueOf(user.getBalance()));
                        binding.buyTicketsCinema.setText(cinema.getName());
                        binding.buyTicketsShow.setText(show.getDate());
                        binding.buyTicketsShowroom.setText(showroomName);
                        if (!seatsRendered)
                            populateLinearLayout(binding.CinemaSeatsLayout, rows, cols);
                        else
                            updateLinearLayout(rows, cols);
                        if ((currentSeatCol != -1) && (currentSeatRow != -1)) {
                            int seatIndex = 0;
                            for (Seat seat: show.getSeats()) {
                                if ((seat.getRow() == currentSeatRow + 1) && (seat.getCol() == currentSeatCol + 1)) {
                                    seatIndex = show.getSeats().indexOf(seat);
                                }
                            }
                            selectSeat(currentSeatRow, currentSeatCol, seatIndex);
                        }
                    }
                }
            }
        });
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
                    if (seat.isSold())
                        imageView.setBackgroundResource(R.drawable.seat_border_sold);
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
                if (!(seat.isSold() && !seat.getOwner().equals(uid))) {
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectSeat(row, col, seatIndex);
                        }
                    });
                }
                else {
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {}
                    });
                }
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
        binding.buyTicketsPrice.setText(String.valueOf(price));
        binding.buyTicketsChosenSeat.setText("ряд " + (row + 1) + ", место " + (col + 1));
        binding.buyTicketsCanAfford.setVisibility(View.VISIBLE);

        if (seat.isSold() && owner.equals(uid)) {
            binding.buyTicketsCanAfford.setVisibility(View.INVISIBLE);
            binding.buyTicketsBtn.setVisibility(View.GONE);
            binding.returnTicketsBtn.setVisibility(View.VISIBLE);
            binding.returnTicketsBtn.setEnabled(true);
            binding.returnTicketsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seat.setSold(false);
                    seat.setOwner(null);
                    user.setBalance(user.getBalance() + seat.getPrice());
                    binding.returnTicketsBtn.setEnabled(false);
                    binding.buyTicketsBtn.setVisibility(View.VISIBLE);
                    binding.returnTicketsBtn.setVisibility(View.GONE);
                    binding.buyTicketsCanAfford.setVisibility(View.VISIBLE);
                    if (price <= user.getBalance()) {
                        binding.buyTicketsBtn.setEnabled(true);
                        binding.buyTicketsCanAfford.setText(R.string.buyTicketsCanAfford);
                    }
                    else {
                        binding.buyTicketsBtn.setEnabled(false);
                        binding.buyTicketsCanAfford.setText(R.string.buyTicketsCannotAfford);
                    }
                    Snackbar.make(binding.getRoot(), "Билет успешно сдан в кассу", Snackbar.LENGTH_LONG).show();
                    writeToDatabase(new OnDatabaseWriteCallback() {
                        @Override
                        public void onComplete() {
                            updateFragment();
                        }
                    });
                }
            });
        }
        else {
            if (price <= user.getBalance())
            {
                binding.buyTicketsBtn.setVisibility(View.VISIBLE);
                binding.returnTicketsBtn.setVisibility(View.GONE);
                binding.buyTicketsCanAfford.setTextColor(getResources().getColor(R.color.green, theme));
                binding.buyTicketsCanAfford.setText(R.string.buyTicketsCanAfford);
                binding.buyTicketsBtn.setEnabled(true);
                binding.buyTicketsBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        seat.setSold(true);
                        seat.setOwner(uid);
                        user.setBalance(user.getBalance() - seat.getPrice());
                        binding.buyTicketsBtn.setEnabled(false);
                        binding.buyTicketsBtn.setVisibility(View.GONE);
                        binding.returnTicketsBtn.setVisibility(View.VISIBLE);
                        binding.returnTicketsBtn.setEnabled(true);
                        binding.buyTicketsCanAfford.setVisibility(View.INVISIBLE);
                        Snackbar.make(binding.getRoot(), "Билет успешно куплен", Snackbar.LENGTH_LONG).show();
                        writeToDatabase(new OnDatabaseWriteCallback() {
                            @Override
                            public void onComplete() {
                                updateFragment();
                            }
                        });
                    }
                });
            }
            else {
                binding.buyTicketsCanAfford.setTextColor(getResources().getColor(R.color.red, theme));
                binding.buyTicketsCanAfford.setText(R.string.buyTicketsCannotAfford);
            }
        }
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
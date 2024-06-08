package com.example.cinematickets;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;

import com.example.cinematickets.databinding.FragmentChooseCinemaToEditBinding;
import com.example.cinematickets.databinding.FragmentProfileBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseCinemaToEditFragment extends Fragment {
    private FragmentChooseCinemaToEditBinding binding;
    private Context context;

    private List<Cinema> cinemaList;
    private Cinema currentCinema;
    private Showroom currentShowroom;

    private FirebaseDatabase db;
    private DatabaseReference users, cinemas;

    public interface ChooseCinemaToEditNavigation {
        void showShows(Cinema cinema);
    }

    private ChooseCinemaToEditNavigation activityReference;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        try {
            this.activityReference = (ChooseCinemaToEditNavigation) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage() + ": must implement ChooseCinemaToEditNavigation");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChooseCinemaToEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");
        cinemas = db.getReference("Cinemas");
        ((MainActivity) activityReference).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
            @Override
            public void onDataReceived(List<Cinema> result) {
                cinemaList = result;
                updateFragment();
            }
        });
    }

    private void updateFragment() {
        CinemaSpinnerAdapter cinemaSpinnerAdapter = new CinemaSpinnerAdapter(context,
                R.layout.cinema_spinner_item,
                cinemaList);
        cinemaSpinnerAdapter.setDropDownViewResource(R.layout.cinema_spinner_dropdown_item);
        binding.cinemaSpinner.setAdapter(cinemaSpinnerAdapter);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCinema = (Cinema) parent.getItemAtPosition(position);
                binding.deleteCinemaBtn.setVisibility(View.VISIBLE);
                updateCinemaLayout();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCinema = null;
                updateCinemaLayout();
            }
        };
        binding.cinemaSpinner.setOnItemSelectedListener(listener);
        binding.addCinemaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.cinemaSpinnerLayout.setVisibility(View.GONE);
                binding.deleteCinemaBtn.setVisibility(View.GONE);
                binding.editCinemaLayoutTitle.setVisibility(View.GONE);
                currentCinema = new Cinema();
                int largestId = 0;
                for (Cinema cinema: cinemaList) {
                    largestId = Math.max(largestId, cinema.getId());
                }
                currentCinema.setId(largestId + 1);
                updateCinemaLayout();
            }
        });
        binding.cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.cinemaSpinnerLayout.setVisibility(View.VISIBLE);
                binding.editCinemaLayoutTitle.setVisibility(View.VISIBLE);
                Snackbar.make(binding.getRoot(), "Изменения для кинотеатра " + currentCinema.getName() + " отменены.", Snackbar.LENGTH_LONG).show();
                currentCinema = null;
                updateFragment();
            }
        });
        binding.editShowsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCinema.setName(binding.cinemaNameEditText.getText().toString());
                currentCinema.setAddress(binding.cinemaAddressEditText.getText().toString());
                currentCinema.setLatitude(Float.parseFloat(binding.cinemaLatitudeEditText.getText().toString()));
                currentCinema.setLongitude(Float.parseFloat(binding.cinemaLongitudeEditText.getText().toString()));
                DatabaseReference cinemaRef = cinemas.child("cinema" + currentCinema.getId());
                cinemaRef.child("id").setValue(currentCinema.getId());
                cinemaRef.child("name").setValue(currentCinema.getName());
                cinemaRef.child("address").setValue(currentCinema.getAddress());
                cinemaRef.child("lat").setValue(currentCinema.getLatitude());
                cinemaRef.child("lon").setValue(currentCinema.getLongitude());
                for (Showroom showroom: currentCinema.getShowrooms()) {
                    String showroomName = "showroom" + showroom.getId();
                    cinemaRef.child("showrooms").child(showroomName).setValue(showroom);
                }
                Snackbar.make(binding.getRoot(), "Данные о кинотеатре " +
                                currentCinema.getName() + " успешно сохранены.",
                        Snackbar.LENGTH_LONG).show();
                activityReference.showShows(currentCinema);
            }
        });
        binding.deleteCinemaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDialog confirmDialog = new ConfirmDialog(context, new ConfirmDialog.ConfirmDialogCallback() {
                    @Override
                    public void onConfirmation() {
                        Map<String, Integer> ticketReturn = new HashMap<>();
                        for (Show show: currentCinema.getShows()) { // вернуть деньги всем владельцам купленных мест
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
                        // найти и удалить записи о кинотеатре из базы данных
                        cinemas.child("cinema" + currentCinema.getId()).removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                Snackbar.make(binding.getRoot(), "Сведения о кинотеатре удалены успешно.", Snackbar.LENGTH_LONG).show();
                            }
                        });
                        // повторно загрузить данные о кинотеатрах
                        ((MainActivity) activityReference).loadCinemas(new MainActivity.onCinemasDataReceivedCallback() {
                            @Override
                            public void onDataReceived(List<Cinema> result) {
                                cinemaList = result;
                                updateFragment();
                            }
                        });
                    }
                });
                confirmDialog.showDialog("Подтвердите удаление", "Вы действительно хотите удалить кинотеатр " + currentCinema.getName() + " и все связанные с ним кинозалы и сеансы?");
            }
        });
        binding.saveCinemaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCinema.setName(binding.cinemaNameEditText.getText().toString());
                currentCinema.setAddress(binding.cinemaAddressEditText.getText().toString());
                currentCinema.setLatitude(Float.parseFloat(binding.cinemaLatitudeEditText.getText().toString().replace(",", ".")));
                currentCinema.setLongitude(Float.parseFloat(binding.cinemaLongitudeEditText.getText().toString().replace(",", ".")));
                DatabaseReference cinemaRef = cinemas.child("cinema" + currentCinema.getId());
                cinemaRef.child("id").setValue(currentCinema.getId());
                cinemaRef.child("name").setValue(currentCinema.getName());
                cinemaRef.child("address").setValue(currentCinema.getAddress());
                cinemaRef.child("lat").setValue(currentCinema.getLatitude());
                cinemaRef.child("lon").setValue(currentCinema.getLongitude());
                for (Showroom showroom: currentCinema.getShowrooms()) {
                    String showroomName = "showroom" + showroom.getId();
                    cinemaRef.child("showrooms").child(showroomName).setValue(showroom);
                }
                Snackbar.make(binding.getRoot(), "Данные о кинотеатре " +
                                currentCinema.getName() + " успешно сохранены.",
                        Snackbar.LENGTH_LONG).show();
                onBackPressed();
            }
        });
    }

    private void updateCinemaLayout() {
        if (currentCinema == null) {
            binding.editCinemaLayout.setVisibility(View.GONE);
        }
        else {
            binding.editCinemaLayout.setVisibility(View.VISIBLE);
            if (currentCinema.getName() != null) {
                binding.cinemaNameEditText.setText(currentCinema.getName());
            }
            else {
                binding.cinemaNameEditText.setText(null);
            }
            if (currentCinema.getAddress() != null) {
                binding.cinemaAddressEditText.setText(currentCinema.getAddress());
            }
            else {
                binding.cinemaAddressEditText.setText(null);
            }
            binding.cinemaLatitudeEditText.setText(String.format("%.6f", currentCinema.getLatitude()).replace(",", "."));
            binding.cinemaLongitudeEditText.setText(String.format("%.6f", currentCinema.getLongitude()).replace(",", "."));

            ShowroomSpinnerAdapter showroomSpinnerAdapter = new ShowroomSpinnerAdapter(this.context,
                    R.layout.showroom_spinner_item,
                    currentCinema.getShowrooms());
            showroomSpinnerAdapter.setDropDownViewResource(R.layout.showroom_spinner_dropdown_item);
            binding.showroomSpinner.setAdapter(showroomSpinnerAdapter);
            binding.showroomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentShowroom = (Showroom) parent.getItemAtPosition(position);
                    binding.editShowroomLayout.setVisibility(View.VISIBLE);
                    binding.showroomNameEditText.setText(currentShowroom.getName());
                    binding.showroomCols.setText(String.valueOf(currentShowroom.getCols()));
                    binding.showroomRows.setText(String.valueOf(currentShowroom.getRows()));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    binding.editShowroomLayout.setVisibility(View.GONE);
                }
            });
            binding.addShowroomBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle("Добавить новый кинозал");
                    dialog.setMessage("Введите данные о новом кинозале");

                    LayoutInflater inflater = LayoutInflater.from(context);
                    View addNewShowroomWindow = inflater.inflate(R.layout.add_new_showroom_window, null);
                    dialog.setView(addNewShowroomWindow);

                    final EditText showroomName = addNewShowroomWindow.findViewById(R.id.showroomName);
                    final EditText showroomRows = addNewShowroomWindow.findViewById(R.id.showroomRows);
                    final EditText showroomCols = addNewShowroomWindow.findViewById(R.id.showroomCols);

                    dialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Showroom temp = new Showroom();
                            int largestId = 0;
                            for (Showroom showroom: currentCinema.getShowrooms()) {
                                largestId = Math.max(largestId, showroom.getId());
                            }
                            temp.setId(largestId + 1);
                            temp.setName(showroomName.getText().toString());
                            temp.setRows(Integer.parseInt(showroomRows.getText().toString()));
                            temp.setCols(Integer.parseInt(showroomCols.getText().toString()));
                            currentCinema.getShowrooms().add(temp);

                            if (!binding.cinemaNameEditText.getText().toString().isEmpty())
                                currentCinema.setName(binding.cinemaNameEditText.getText().toString());
                            if (!binding.cinemaAddressEditText.getText().toString().isEmpty())
                                currentCinema.setAddress(binding.cinemaAddressEditText.getText().toString());
                            if (!binding.cinemaLatitudeEditText.getText().toString().isEmpty())
                                currentCinema.setLatitude(Float.parseFloat(binding.cinemaLatitudeEditText.getText().toString().replace(",", ".")));
                            if (!binding.cinemaLongitudeEditText.getText().toString().isEmpty())
                                currentCinema.setLongitude(Float.parseFloat(binding.cinemaLongitudeEditText.getText().toString().replace(",", ".")));

                            updateCinemaLayout();
                        }
                    });
                    dialog.show();
                }
            });
            binding.deleteShowroomBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConfirmDialog confirmDialog = new ConfirmDialog(context, new ConfirmDialog.ConfirmDialogCallback() {
                        @Override
                        public void onConfirmation() {
                            int index = -1;
                            for (Showroom showroom: currentCinema.getShowrooms()) {
                                if (showroom.getId() == currentShowroom.getId()) {
                                    index = currentCinema.getShowrooms().indexOf(showroom);
                                    break;
                                }
                            }
                            if (index != -1) {
                                Map<String, Integer> ticketReturn = new HashMap<>();
                                for (Show show: currentCinema.getShows()) {
                                    if (show.getRoom_id() == currentShowroom.getId()) {
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
                                        cinemas.child("cinema" + currentCinema.getId()).child("shows").child("show" + show.getId()).removeValue();
                                        currentCinema.getShows().remove(show);
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
                                currentCinema.getShowrooms().remove(index);
                                cinemas.child("cinema" + currentCinema.getId()).child("showrooms").child("showroom" + currentShowroom.getId()).removeValue();
                                Snackbar.make(binding.getRoot(), "Кинозал и все показы в этом зале удалены.", Snackbar.LENGTH_LONG).show();
                            }
                            else Snackbar.make(binding.getRoot(), "Ошибка при удалении кинозала.", Snackbar.LENGTH_LONG).show();
                            updateCinemaLayout();
                        }
                    });
                    confirmDialog.showDialog("Подтвердите удаление", "Вы действительно хотите удалить кинозал " + currentShowroom.getName() + " и все связанные с ним сеансы?");
                }
            });
            binding.saveShowroomBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentShowroom.setName(binding.showroomNameEditText.getText().toString());
                    if (!binding.cinemaNameEditText.getText().toString().isEmpty())
                        currentCinema.setName(binding.cinemaNameEditText.getText().toString());
                    if (!binding.cinemaAddressEditText.getText().toString().isEmpty())
                        currentCinema.setAddress(binding.cinemaAddressEditText.getText().toString());
                    if (!binding.cinemaLatitudeEditText.getText().toString().isEmpty())
                        currentCinema.setLatitude(Float.parseFloat(binding.cinemaLatitudeEditText.getText().toString().replace(",", ".")));
                    if (!binding.cinemaLongitudeEditText.getText().toString().isEmpty())
                        currentCinema.setLongitude(Float.parseFloat(binding.cinemaLongitudeEditText.getText().toString().replace(",", ".")));

                    int index = -1;
                    for (Showroom showroom: currentCinema.getShowrooms()) {
                        if (showroom.getId() == currentShowroom.getId()) {
                            index = currentCinema.getShowrooms().indexOf(showroom);
                            break;
                        }
                    }
                    if (index != -1) currentCinema.getShowrooms().set(index, currentShowroom);
                    else Snackbar.make(binding.getRoot(), "Ошибка при изменении кинозала.", Snackbar.LENGTH_LONG).show();
                    updateCinemaLayout();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().invalidateOptionsMenu();
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
package com.example.cinematickets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Show {
    private String date;
    private int film_id;
    private int room_id;
    private int id;
    private List<Seat> seats;
    private String filmName;
    private String showroomName;

    public Show() {
        this.seats = new ArrayList<>();
    }

    public Show(int id, String date, int film_id, int room_id) {
        this.id = id;
        this.date = date;
        this.film_id = film_id;
        this.room_id = room_id;
        this.seats = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getFilm_id() {
        return film_id;
    }

    public void setFilm_id(int film_id) {
        this.film_id = film_id;
    }

    public int getRoom_id() {
        return room_id;
    }

    public void setRoom_id(int room_id) {
        this.room_id = room_id;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public void addSeat(Seat seat) {
        this.seats.add(seat);
    }

    public void removeSeat(int seatPosition) {
        this.seats.remove(seatPosition);
    }

    public void removeSeat(Seat seat) {
        this.seats.remove(seat);
    }

    public String getFilmName() {
        return filmName;
    }

    public void setFilmName(String filmName) {
        this.filmName = filmName;
    }

    public String getShowroomName() {
        return showroomName;
    }

    public void setShowroomName(String showroomName) {
        this.showroomName = showroomName;
    }

    public boolean hasAvailableSeats(String uid) {
        for (Seat seat: seats) {
            if (!(seat.sold && !Objects.equals(seat.getOwner(), uid))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        String text = "Фильм: " + filmName + "\nКинозал: " + showroomName + "\nДата: " + date;
        int seatsLeft = 0;
        int totalSeats = 0;
        for (Seat seat: seats) {
            if (!seat.sold) {
                seatsLeft++;
            }
            totalSeats++;
        }
        text += "\nОсталось мест: " + seatsLeft + " / " + totalSeats;
        return text;
    }

    public String getShowInfo(String uid) {
        String text = date;
        int seatsLeft = 0;
        int boughtSeats = 0;
        for (Seat seat: seats) {
            if (!seat.sold) {
                seatsLeft++;
            }
            else {
                if (seat.getOwner().equals(uid)) {
                    boughtSeats++;
                }
            }
        }
        text += " (мест куплено: " + boughtSeats + ", свободно: " + seatsLeft + ")";
        return text;
    }
}

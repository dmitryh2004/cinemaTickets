package com.example.cinematickets;

import java.util.ArrayList;
import java.util.List;

public class Cinema {
    private float longitude;
    private float latitude;
    private String name;
    private String address;
    private int id;
    private List<Showroom> showrooms;
    private List<Show> shows;

    public Cinema() {
        this.showrooms = new ArrayList<>();
        this.shows = new ArrayList<>();
    }

    public Cinema(float longitude, float latitude, String name, String address, int id) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.name = name;
        this.address = address;
        this.id = id;
        this.showrooms = new ArrayList<>();
        this.shows = new ArrayList<>();
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Showroom> getShowrooms() {
        return showrooms;
    }

    public void setShowrooms(List<Showroom> showrooms) {
        this.showrooms = showrooms;
    }

    public void addShowroom(Showroom showroom) {
        this.showrooms.add(showroom);
    }

    public void removeShowroom(Showroom showroom) {
        this.showrooms.remove(showroom);
    }

    public void removeShowroom(int index) {
        this.showrooms.remove(index);
    }

    public List<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> shows) {
        this.shows = shows;
    }

    public void addShow(Show show) {
        this.shows.add(show);
    }

    public void removeShow(Show show) {
        this.shows.remove(show);
    }

    public void removeShow(int index) {
        this.shows.remove(index);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        String text = name;
        if (address != null) {
            text += " (" + address + ")";
        }
        else {
            text += " (адрес не указан)";
        }
        return text;
    }
}

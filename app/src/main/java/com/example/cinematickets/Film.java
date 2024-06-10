package com.example.cinematickets;

import android.graphics.Bitmap;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Film {
    private String shortDesc;
    private int id = 0;
    private String name;
    private String descURL;
    private Bitmap poster; //binary string with image (100x100 or fewer)
    private int publish_year; //год выпуска фильма
    private String genre; //жанр
    private float rating; //оценка фильма
    private String desc;
    private LocalDateTime earliestDate;
    private int earliestCinemaID;
    private ArrayList<Integer> cinemaIDs; //ID кинотеатров, в которых будет показываться фильм

    Film() {

    }

    public Film(int id, String name, String shortDesc, String descURL, int publish_year,
                String genre, float rating, String desc) {
        this.id = id;
        this.name = name;
        this.shortDesc = shortDesc;
        this.descURL = descURL;
        this.cinemaIDs = new ArrayList<>();
        this.publish_year = publish_year;
        this.genre = genre;
        this.rating = rating;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescURL() {
        return descURL;
    }

    public void setDescURL(String descURL) {
        this.descURL = descURL;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public Bitmap getPoster() {
        return poster;
    }

    public void setPoster(Bitmap poster) {
        this.poster = poster;
    }

    public LocalDateTime getEarliestDate() {
        return earliestDate;
    }

    public void setEarliestDate(LocalDateTime earliestDate) {
        this.earliestDate = earliestDate;
    }

    public ArrayList<Integer> getCinemaIDs() {
        return cinemaIDs;
    }

    public void setCinemaIDs(ArrayList<Integer> cinemaID) {
        this.cinemaIDs = cinemaID;
    }

    public void addCinemaID(Integer newCinemaID) {
        this.cinemaIDs.add(newCinemaID);
    }
    public void removeCinemaID(Integer cinemaID) {
        this.cinemaIDs.remove(cinemaID);
    }

    public int getPublish_year() {
        return publish_year;
    }

    public void setPublish_year(int publish_year) {
        this.publish_year = publish_year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getEarliestCinemaID() {
        return earliestCinemaID;
    }

    public void setEarliestCinemaID(int earliestCinemaID) {
        this.earliestCinemaID = earliestCinemaID;
    }
}

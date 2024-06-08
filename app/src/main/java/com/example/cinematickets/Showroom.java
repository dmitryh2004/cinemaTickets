package com.example.cinematickets;

public class Showroom {
    private int cols;
    private int rows;
    private int id;
    private String name;

    public Showroom() {

    }

    public Showroom(int cols, int rows, int id, String name) {
        this.cols = cols;
        this.rows = rows;
        this.id = id;
        this.name = name;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
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
}

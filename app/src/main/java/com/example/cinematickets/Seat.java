package com.example.cinematickets;

public class Seat {
    int col;
    int row;
    int id;
    boolean sold;
    int price;
    String owner;

    public Seat() {

    }

    public Seat(int id, int col, int row, boolean sold, int price, String owner) {
        this.id = id;
        this.col = col;
        this.row = row;
        this.sold = sold;
        this.price = price;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}

package com.example.charles.twintracker;

/**
 * Created by clesoil on 23/11/2017.
 */

class iron {

    private String name;
    private String day;
    private int count;

    public iron(String name, String day, int count) {
        this.name = name;
        this.day = day;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

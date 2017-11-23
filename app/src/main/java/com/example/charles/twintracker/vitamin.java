package com.example.charles.twintracker;

/**
 * Created by clesoil on 23/11/2017.
 */

class vitamin {

    private String name;
    private String day;

    vitamin(String name, String day) {
        this.name = name;
        this.day = day;
    }

    public String getName() {
        return name;
    }

    public String getDay() {
        return day;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDay(String day) {
        this.day = day;
    }
}

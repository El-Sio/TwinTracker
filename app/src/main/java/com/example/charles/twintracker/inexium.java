package com.example.charles.twintracker;

/**
 * Created by clesoil on 20/01/2018.
 */

public class inexium {

    private String name;
    private String day;

    inexium(String name, String day) {
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

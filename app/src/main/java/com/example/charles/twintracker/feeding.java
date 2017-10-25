package com.example.charles.twintracker;

/**
 * Created by charl on 24/10/2017.
 */

public class feeding {

    private String name;
    private String start;
    private String duration;

    public feeding(String name, String start, String duration) {
        this.name = name;
        this.start = start;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public String getStart() {
        return start;
    }

    public String getDuration() {
        return duration;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}



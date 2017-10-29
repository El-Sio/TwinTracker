package com.example.charles.twintracker;

/**
 * Created by charl on 24/10/2017.
 */

//Custon class to store the Data structure of a feeding with a name (agathe or zoe), a start time (as hh:mm:ss string), and a duration (as (hh:mm:ss) string)
 class feeding {

    private String name;
    private String start;
    private String duration;

     feeding(String name, String start, String duration) {
        this.name = name;
        this.start = start;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

     String getStart() {
        return start;
    }

     String getDuration() {
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



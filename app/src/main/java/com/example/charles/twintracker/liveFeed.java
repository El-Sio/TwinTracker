package com.example.charles.twintracker;

/**
 * Created by clesoil on 26/11/2017.
 */

class liveFeed {

    private String name;
    private Boolean isOngoing;
    private long startTime;
    private String startDate;

    public liveFeed(String name, Boolean isOngoing, long startTime, String startDate) {
        this.name = name;
        this.isOngoing = isOngoing;
        this.startTime = startTime;
        this.startDate = startDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getOngoing() {
        return isOngoing;
    }

    public void setOngoing(Boolean ongoing) {
        isOngoing = ongoing;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}

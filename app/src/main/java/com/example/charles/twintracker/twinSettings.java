package com.example.charles.twintracker;

/**
 * Created by clesoil on 29/11/2017.
 */

class twinSettings {

    private String user;
    private Boolean shouldnotify;
    private Boolean autoStop;
    private String twin1name;
    private String twin2name;

    public twinSettings(String username, Boolean shouldnotify, Boolean autostpo, String twin1name, String twin2name) {
        this.user = username;
        this.shouldnotify = shouldnotify;
        this.twin1name = twin1name;
        this.twin2name = twin2name;
        this.autoStop = autostpo;
    }

    public Boolean getAutoStop() {
        return autoStop;
    }

    public void setAutoStop(Boolean autoStop) {
        this.autoStop = autoStop;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getShouldnotify() {
        return shouldnotify;
    }

    public void setShouldnotify(Boolean shouldnotify) {
        this.shouldnotify = shouldnotify;
    }

    public String getTwin1name() {
        return twin1name;
    }

    public void setTwin1name(String twin1name) {
        this.twin1name = twin1name;
    }

    public String getTwin2name() {
        return twin2name;
    }

    public void setTwin2name(String twin2name) {
        this.twin2name = twin2name;
    }
}

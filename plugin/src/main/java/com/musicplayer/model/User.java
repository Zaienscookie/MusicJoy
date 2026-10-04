package com.musicplayer.model;

public class User {
    private final String nickname;
    private final boolean vip;
    private final boolean loggedIn;

    public User(String nickname, boolean vip, boolean loggedIn) {
        this.nickname = nickname;
        this.vip = vip;
        this.loggedIn = loggedIn;
    }

    public String getNickname() { return nickname; }
    public boolean isVip() { return vip; }
    public boolean isLoggedIn() { return loggedIn; }
}
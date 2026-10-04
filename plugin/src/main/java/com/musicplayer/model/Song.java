package com.musicplayer.model;

public class Song {
    private final long id;
    private final String name;
    private final String artist;
    private final String album;
    private final long duration;

    public Song(long id, String name, String artist, String album, long duration) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getDuration() { return duration; }

    public String getDurationFormatted() {
        long seconds = duration / 1000;
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    @Override
    public String toString() {
        return name + " - " + artist + " [" + getDurationFormatted() + "]";
    }
}
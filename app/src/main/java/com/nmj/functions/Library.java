package com.nmj.functions;

public class Library {
    private String tmdbId, showId, playCount;

    public Library(String showId, String playCount, String tmdbId) {
        this.showId = showId;
        this.playCount = playCount;
        this.tmdbId = tmdbId;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public String getId() {
        return tmdbId;
    }

    public void setId(String tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getPlayCount() {
        return playCount;
    }

    public void setPlayCount(String playCount) {
        this.playCount = playCount;
    }

}
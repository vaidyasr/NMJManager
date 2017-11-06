/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nmj.apis.nmj;

import android.text.TextUtils;

import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.Actor;
import com.nmj.functions.NMJLib;
import com.nmj.functions.Video;
import com.nmj.functions.WebMovie;

import java.util.ArrayList;
import java.util.List;

public class Movie {

    private String showid = "", title = "", originalTitle = "", plot = "", thumbnail = "", poster = "", backdrop = "",
            rating = "0.0", tagline = "", releasedate = "", imdbId = "", certification = "", runtime = "0",
            trailer = "", genres = "", cast = "", crew = "", collectionTitle = "", collectionId = "",
            collectionImage = "", year = "", tmdbId = "", filepath = "", title_type;
    private List<Video> mVideo = new ArrayList<>();

    private List<Actor> mCast = new ArrayList<Actor>();
    private List<Actor> mCrew = new ArrayList<Actor>();
    private List<WebMovie> mSimilarMovies = new ArrayList<WebMovie>();
    private List<WebMovie> mRecommendedMovies = new ArrayList<WebMovie>();
    private String mFilepaths = filepath;
    private String TO_WATCH = "0", FAVOURITE = "0", HAS_WATCHED = "0";
    private String LIST_ID;

    public Movie() {
        // Unidentified by default
        setTmdbId(DbAdapterMovies.UNIDENTIFIED_ID);
    }

    public String getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(String id) {
        if (!id.startsWith("tmdb"))
            this.tmdbId = "tmdb" + id;
        else
            this.tmdbId = id;
    }

    public String getId() {
        if (TextUtils.isEmpty(tmdbId))
            return title;
        return tmdbId.replace("tmdb", "");
    }

    public String getShowId() {
        if (TextUtils.isEmpty(showid))
            return "0";
        return showid;
    }

    public void setShowId(String id) {
        this.showid = id;
    }

    public boolean isFavourite() {
        return FAVOURITE.equals("1");
    }

    public void setFavourite(String favourite) {
        this.FAVOURITE = favourite;
    }

    public boolean toWatch() {
        return (!TO_WATCH.equals("0"));
    }

    public boolean hasWatched() {
        return (!HAS_WATCHED.equals("0"));
    }

    public void setToWatch(String toWatch) {
        this.TO_WATCH = toWatch;
    }

    public void setHasWatched(String id) {
        this.HAS_WATCHED = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getThumbnail() {
        return thumbnail.replaceAll(" ", "%20");
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPoster() {
        return poster.replaceAll(" ", "%20");
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getTitleType() {
        return title_type;
    }

    public void setTitleType(String title_type) {
        this.title_type = title_type;
    }

    public String getListId() {
        return LIST_ID;
    }

    public void setListId(String list_id) {
        this.LIST_ID = list_id;
    }

    public String getBackdrop() {
        return backdrop.replaceAll(" ", "%20");
    }

    public void setBackdrop(String backdrop) {
        this.backdrop = backdrop;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getReleasedate() {
        return releasedate;
    }

    public void setReleasedate(String releasedate) {
        this.releasedate = releasedate;
    }

    public void setReleaseDateYMD(int year, int month, int day) {
        this.releasedate = year + "-" + NMJLib.addIndexZero(month) + "-" + NMJLib.addIndexZero(day);
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getCrewString() {
        return crew;
    }

    public void setCrewString(String crew) {
        this.crew = crew;
    }

    public String getCastString() {
        return cast;
    }

    public void setCastString(String cast) {
        this.cast = cast;
    }

    public String getCollectionTitle() {
        return collectionTitle;
    }

    public void setCollectionTitle(String collectionTitle) {
        this.collectionTitle = collectionTitle;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionImage() {
        return collectionImage;
    }

    public void setCollectionImage(String collectionImage) {
        this.collectionImage = collectionImage;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public List<Actor> getCast() {
        return mCast;
    }

    public void setCast(List<Actor> actors) {
        mCast = actors;
    }

    public List<Actor> getCrew() {
        return mCrew;
    }

    public void setCrew(List<Actor> actors) {
        mCrew = actors;
    }

    public List<WebMovie> getSimilarMovies() {
        return mSimilarMovies;
    }

    public void setSimilarMovies(List<WebMovie> movies) {
        mSimilarMovies = movies;
    }

    public List<WebMovie> getRecommendedMovies() {
        return mRecommendedMovies;
    }

    public String getAllFilepaths() {
        StringBuilder sb = new StringBuilder();
        String[] filepaths = mFilepaths.split(",");
        for (int i = 0; i < filepaths.length; i++)
            sb.append(filepaths[i]).append("\n");
        return sb.toString().trim();
    }

    public List<Video> getVideo() {
        return mVideo;
    }

    public void setVideo(List<Video> video) {
        mVideo = video;
    }


}
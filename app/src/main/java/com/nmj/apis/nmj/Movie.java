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
import com.nmj.functions.WebMovie;

import java.util.ArrayList;
import java.util.List;

public class Movie {

    private String showid = "", title = "", originalTitle = "", plot = "", thumbnail = "", poster = "", backdrop = "",
            rating = "0.0", tagline = "", releasedate = "", imdbId = "", certification = "", runtime = "0",
            trailer = "", genres = "", cast = "", crew = "", collectionTitle = "", collectionId = "",
            collectionImage = "", year = "", tmdbId = "";

    private List<Actor> mCast = new ArrayList<Actor>();
    private List<Actor> mCrew = new ArrayList<Actor>();
    private List<WebMovie> mSimilarMovies = new ArrayList<WebMovie>();

    public Movie() {
        // Unidentified by default
        setTmdbId(DbAdapterMovies.UNIDENTIFIED_ID);
    }

    public String getTmdbId() {
        if (TextUtils.isEmpty(tmdbId))
            return title;
        return tmdbId.replace("tmdb", "");
    }

    public void setTmdbId(String id) {
        this.tmdbId = id;
    }

    public String getShowId() {
        if (TextUtils.isEmpty(showid))
            return "0";
        return showid;
    }

    public void setShowId(String id) {
        this.showid = id;
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
}
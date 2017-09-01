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
import com.nmj.functions.Video;
import com.nmj.functions.WebMovie;

import java.util.ArrayList;
import java.util.List;

public class TvShow {

    public static final int TMDB = 1, THETVDB = 2;
    private String showid = "", title = "", originalTitle = "", plot = "", thumbnail = "", poster = "", backdrop = "",
            rating = "0.0", tagline = "", releasedate = "", imdbId = "", certification = "", runtime = "0",
            trailer = "", genres = "", cast = "", crew = "", collectionTitle = "", collectionId = "", mCoverUrl = "",
            collectionImage = "", year = "", tmdbId = "", filepath = "",mDescription = "", mActors = "", mBackdropUrl = "",
            mFirstAired = "", mImdbId= "";
    ;
    private boolean mFavorite;
    private List<Video> mVideo = new ArrayList<>();
    private ArrayList<Episode> mEpisodes = new ArrayList<>();
    private ArrayList<Season> mSeasons = new ArrayList<>();
    private List<Actor> mCast = new ArrayList<Actor>();
    private List<Actor> mCrew = new ArrayList<Actor>();
    private List<WebMovie> mSimilarShows = new ArrayList<WebMovie>();
    private String mFilepaths = filepath;

    public TvShow() {
        // Unidentified by default
        setId(DbAdapterMovies.UNIDENTIFIED_ID);
    }

    public String getBackdropUrl() {
        return mBackdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        mBackdropUrl = backdropUrl;
    }

    public String getId() {
        if (TextUtils.isEmpty(tmdbId))
            return title;
        return tmdbId.replace("tmdb", "").replace("tvdb", "");
    }

    public String getTmdbId(){
            return this.tmdbId;
    }

    public void setTmdbId(String id) {
        if (!id.startsWith("tmdb"))
            this.tmdbId = "tmdb" + id;
        else
            this.tmdbId = id;
    }

    public void setId(String id) {
        this.tmdbId = id;
    }

    public String getFirstAired() {
        return mFirstAired;
    }

    public void setFirstAired(String firstAired) {
        mFirstAired = firstAired;
    }

    public String getCoverUrl() {
        return mCoverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        mCoverUrl = coverUrl;
    }

    public void setIMDbId(String id) {
        mImdbId = id;
    }

    public int getIdType() {
        if (tmdbId.startsWith("tmdb"))
            return TMDB;
        return THETVDB;
    }

    public String getActors() {
        return mActors;
    }

    public void setActors(String actors) {
        mActors = actors;
    }

    public void addSeason(Season s) {
        mSeasons.add(s);
    }

    public ArrayList<Season> getSeasons() {
        return mSeasons;
    }

    public Integer getSeasonsCount() {
        return mSeasons.size();
    }

    public boolean hasSeason(int season) {
        for (Season s : mSeasons)
            if (s.getSeason() == season)
                return true;
        return false;
    }

    public Season getSeason(int season) {
        for (Season s : mSeasons)
            if (s.getSeason() == season)
                return s;
        return new Season();
    }
    public boolean isFavorite() {
        return mFavorite;
    }

    public String getFavorite() {
        return mFavorite ? "1" : "0";
    }

    public void setFavorite(boolean fav) {
        mFavorite = fav;
    }

    public String getIdWithoutHack() {
        return tmdbId.replace("tmdb_", "");
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

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
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

    public String getFirstAirdate() {
        return releasedate;
    }

    public void setFirstAirdate(String releasedate) {
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

    public void addEpisode(Episode ep) {
        mEpisodes.add(ep);
    }

    public ArrayList<Episode> getEpisodes() {
        return mEpisodes;
    }

    public Integer getEpisodeCount() {
        return mEpisodes.size();
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

    public List<WebMovie> getSimilarShows() {
        return mSimilarShows;
    }

    public void setSimilarShows(List<WebMovie> movies) {
        mSimilarShows = movies;
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
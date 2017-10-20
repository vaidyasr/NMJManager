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

package com.nmj.functions;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import com.nmj.nmjmanager.NMJManagerApplication;

public class NMJAdapter {
    // Database fields
    public static final String KEY_TMDB_ID = "tmdbid"; // Unidentified movies and .nfo files without TMDb ID use the filepath
    public int movieCount, showCount, musicCount;
    ArrayList<Library> mLibrary;

    public NMJAdapter() {
    }

    public int getMovieCount() {
        return movieCount;
    }

    public void setMovieCount(int count) {
        this.movieCount = count;
    }

    public int getShowCount() {
        return showCount;
    }

    public void setShowCount(int count) {
        this.showCount = count;
    }

    public int getMusicCount() {
        return musicCount;
    }

    public void setMusicCount(int count) {
        this.musicCount = count;
    }

    public ArrayList<Library> getLibrary() {
        return mLibrary;
    }

    public void setLibrary(ArrayList<Library> library) {
        this.mLibrary = library;
    }

    public boolean movieExistsbyId(String movieId) {
        if (mLibrary != null) {
            for (int i = 0; i < mLibrary.size(); i++) {
                if (mLibrary.get(i).getId().equals(movieId))
                    return true;
            }
        }
        return false;
    }

    public String getShowIdByTmdbId(String movieId){
        if (mLibrary != null) {
            for (int i = 0; i < mLibrary.size(); i++) {
                if (mLibrary.get(i).getId().equals("tmdb" + movieId))
                    return mLibrary.get(i).getShowId();
            }
        }
        return "0";
    }

    public boolean getWatchedByShowId(String movieId) {
        if (mLibrary != null) {
            for (int i = 0; i < mLibrary.size(); i++) {
                if (mLibrary.get(i).getShowId().equals(movieId)) {
                    if (!mLibrary.get(i).getPlayCount().equals("0"))
                        return true;
                }
            }
        }
        return false;
    }

    public void setWatchedByShowId(String movieId, boolean watched) {
        if (mLibrary != null) {
            for (int i = 0; i < mLibrary.size(); i++) {
                if (mLibrary.get(i).getShowId().equals(movieId)) {
                    if (watched)
                        mLibrary.get(i).setPlayCount("1");
                    else
                        mLibrary.get(i).setPlayCount("0");
                }
            }
        }
    }

    public boolean hasWatched(String movieId){
        if (mLibrary != null) {
            for (int i = 0; i < mLibrary.size(); i++) {
                if (mLibrary.get(i).getId().equals("tmdb" + movieId)) {
                    if (!mLibrary.get(i).getPlayCount().equals("0"))
                        return true;
                }
            }
        }
        return false;
    }

    public boolean movieExistsbyTvdbId(String movieId) {
        for (int i = 0; i < mLibrary.size(); i++) {
            if (mLibrary.get(i).getId().equals("tvdb" + movieId))
                return true;
        }
        return false;
    }

    public ArrayList<String> getCertifications() {
        ArrayList<String> certifications = new ArrayList<String>();

        return certifications;
    }

 /*   public Episode getTVDBEpisodes(Context context, String tvdbid, int seasonNbr, int episodeNbr, String language) {
        TheTVDBApi theTvdb = new TheTVDBApi(NMJLib.getTvdbApiKey(context));
        Episode episode = new Episode();
        try {
            episode = theTvdb.getEpisode(tvdbid, seasonNbr, episodeNbr, language);
        }catch (TvDbException ignored){

        }
        return episode;
    }

    public Series getTVDBSeasons(Context context, String tvdbid, String language) {
        TheTVDBApi theTvdb = new TheTVDBApi(NMJLib.getTvdbApiKey(context));
        Series series = new Series();
        try {
            series = theTvdb.getSeries(tvdbid, language);
        }catch (TvDbException ignored){

        }
        return series;
    }

    public List<com.omertron.thetvdbapi.model.Actor> getTVDBActors(Context context, String tvdbid, String language) {
        TheTVDBApi theTvdb = new TheTVDBApi(NMJLib.getTvdbApiKey(context), NMJManagerApplication.getHttpClient());
        List<com.omertron.thetvdbapi.model.Actor> actors = new ArrayList<Actor>();
        try {
            actors = theTvdb.getActors(tvdbid);
        }catch (TvDbException ignored){

        }
        for(int i=0;i<actors.size();i++)
            System.out.println("Actor: " + actors.get(i).getName());
        return actors;
    }*/
}
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

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodeResponse;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public boolean movieExistsbyTmdbId(String movieId) {
        for (int i = 0; i < mLibrary.size(); i++) {
            if (mLibrary.get(i).getId().equals("tmdb" + movieId))
                return true;
        }
        return false;
    }

    public String getShowIdByTmdbId(String movieId){
        for (int i = 0; i < mLibrary.size(); i++) {
            if (mLibrary.get(i).getId().equals("tmdb" + movieId))
                return mLibrary.get(i).getShowId();
        }
        return "0";
    }

    public boolean hasWatched(String movieId){
        for (int i = 0; i < mLibrary.size(); i++) {
            if (mLibrary.get(i).getId().equals("tmdb" + movieId)) {
                if(!mLibrary.get(i).getPlayCount().equals("0"))
                    return true;
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

    public void getEpisodes(Context context, Integer tvdbid) {
        TheTvdb theTvdb = new TheTvdb(NMJLib.getTvdbApiKey(context));
        try {
            retrofit2.Response<EpisodeResponse> response = theTvdb.episodes().get(tvdbid, "en").execute();

            if (response.isSuccessful()) {
                Episode results = response.body().data;
                System.out.println(results.toString() + " is awesome!");
            }
        } catch (IOException ignored) {
        }
    }

    public void getSeasons(Context context, Integer tvdbid) {
        TheTvdb theTvdb = new TheTvdb(NMJLib.getTvdbApiKey(context));
        try {
            retrofit2.Response<SeriesResponse> response = theTvdb.series().series(tvdbid, "en").execute();

            if (response.isSuccessful()) {
                Series results = response.body().data;
                System.out.println(results.toString() + " is awesome!");
            }
        } catch (IOException ignored) {
        }
    }
}
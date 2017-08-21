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
import android.text.TextUtils;

import com.nmj.abstractclasses.MediumBaseMovie;
import com.nmj.abstractclasses.NMJBaseMovie;
import com.nmj.nmjmanager.R;

import java.io.File;
import java.util.Locale;

public class NMJMovie extends NMJBaseMovie {

    private String PLOT, TAGLINE, IMDB_ID, TRAILER, TITLE;

    private String THUMBNAIL, SHOW_ID, LIST_ID, COLLECTION_ID;

    public NMJMovie(Context context, String title, String tmdbId, String rating, String releasedate,String genres,
                    String favourite, String listId, String collectionId, String toWatch, String hasWatched,
                    String date_added, String certification, String runtime, String showId, String thumbnail, boolean ignorePrefixes) {

        super(context, title, tmdbId, rating, releasedate, genres, favourite, listId,
                collectionId, toWatch, hasWatched, date_added, certification, runtime, showId, thumbnail, ignorePrefixes);

        // Set up movie fields based on constructor
        //PLOT = plot;
        //TAGLINE = tagline;
        mTmdbId = tmdbId;
        THUMBNAIL = thumbnail;
        SHOW_ID = showId;
        TITLE = title;
        COLLECTION_ID = collectionId;
        LIST_ID = listId;
        //IMDB_ID = imdbId;
        //TRAILER = trailer;

        // getPlot()
        /*if (TextUtils.isEmpty(PLOT)) {
            mGetPlot = mContext.getString(R.string.stringNoPlot);
        } else {
            mGetPlot = PLOT;
        }

        // getTagline()
        if (TextUtils.isEmpty(TAGLINE) || TAGLINE.equals("NOTAGLINE")) {
            mGetTagline = "";
        } else {
            mGetTagline = TAGLINE;
        }*/
    }

    //public String getPlot() {
     //   return mGetPlot;
    //}

    //public String getTagline() {
    //    return mGetTagline;
    //}

    public File getPoster() {
        return getThumbnail();
    }

    //public String getImdbId() {
    //    return IMDB_ID;
    //}

    public String getRating() {
        if (!TextUtils.isEmpty(RATING))
            return RATING;
        return "0.0";
    }

    //public String getTrailer() {
    //    return TRAILER;
    //}

    public String getNMJThumbnail() {
        return THUMBNAIL.replaceAll(" ", "%20");
    }

    public String getVideoType(){
        if (getShowId() == "0")
            return "tmdb";
        else
            return "nmj";
    }

    public String getShowId() {
        return SHOW_ID;
    }

    public String getCollectionId(){ return COLLECTION_ID;}

    public String getListId(){ return LIST_ID;}

    public String getFavourite() {
        return FAVOURITE;
    }

    public void setFavourite(boolean isFavourite) {
        if (isFavourite)
            FAVOURITE = "1";
        else
            FAVOURITE = "0";
    }

    public void setFavourite(String fav) {
        FAVOURITE = fav;
    }

    public String getHasWatched() {
        return HAS_WATCHED;
    }

    public void setHasWatched(boolean hasWatched) {
        if (hasWatched)
            HAS_WATCHED = "1";
        else
            HAS_WATCHED = "0";
    }

    public String getToWatch() {
        return TO_WATCH;
    }

    public void setToWatch(boolean toWatch) {
        if (toWatch)
            TO_WATCH = "1";
        else
            TO_WATCH = "0";
    }

    public boolean isSplitFile(String path) {
        return path.matches(".*(cd1|part1).*");
    }

    public boolean isPartOfCollection() {
        return !TextUtils.isEmpty(COLLECTION_ID);
    }

    public String getLocalTrailer(String path) {
        try {
            // Check if there's a custom cover art image
            String filename = path.substring(0, path.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
            File parentFolder = new File(path).getParentFile();

            if (parentFolder != null) {
                File[] list = parentFolder.listFiles();

                if (list != null) {
                    String name, absolutePath;
                    int count = list.length;
                    for (int i = 0; i < count; i++) {
                        name = list[i].getName();
                        absolutePath = list[i].getAbsolutePath();
                        if (absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + "-trailer.") ||
                                absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + "_trailer.") ||
                                absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + " trailer.") ||
                                name.toLowerCase(Locale.ENGLISH).startsWith("trailer.")) {
                            return absolutePath;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return "";
    }
}
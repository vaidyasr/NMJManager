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
import com.nmj.abstractclasses.NMJBaseMovie;
import java.io.File;

public class NMJTvShow extends NMJBaseMovie {

    private String TITLE, TITLE_TYPE, THUMBNAIL, SHOW_ID, LIST_ID, COLLECTION_ID;

    public NMJTvShow(Context context, String title, String tmdbId, String rating, String releasedate,String genres,
                    String favourite, String listId, String collectionId, String toWatch, String hasWatched,
                    String date_added, String certification, String runtime, String showId, String title_type,
                    String thumbnail, boolean ignorePrefixes) {

        super(context, title, tmdbId, rating, releasedate, genres, favourite, listId,
                collectionId, toWatch, hasWatched, date_added, certification, runtime, showId, thumbnail, ignorePrefixes);

        // Set up movie fields based on constructor
        mTmdbId = tmdbId;
        THUMBNAIL = thumbnail;
        SHOW_ID = showId;
        TITLE = title;
        COLLECTION_ID = collectionId;
        LIST_ID = listId;
        TITLE_TYPE = title_type;
        HAS_WATCHED = hasWatched;
    }

    public File getPoster() {
        return getThumbnail();
    }

    public String getRating() {
        if (!TextUtils.isEmpty(RATING))
            return RATING;
        return "0.0";
    }

    public String getNMJThumbnail() {
        return THUMBNAIL.replaceAll(" ", "%20");
    }

    public String getTitleType(){
        if (TITLE_TYPE == "0")
            return "tmdb";
        else
            return "nmj";
    }

    public String getShowId() {
        return SHOW_ID;
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

    public boolean hasWatched() {
        if (HAS_WATCHED.equals("0"))
            return false;
        else
            return true;
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
}
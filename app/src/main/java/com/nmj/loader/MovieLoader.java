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

package com.nmj.loader;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import android.text.TextUtils;
import android.widget.ListView;

import com.nmj.apis.nmj.Movie;

import com.nmj.functions.LibrarySectionAsyncTask;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJLib;
import com.nmj.functions.PreferenceKeys;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.fragments.MovieLibraryOverviewFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MovieLoader {

    // For MovieLibraryType
    public static final int ALL_MOVIES = 1,
            FAVORITES = 2,
            NEW_RELEASES = 3,
            WATCHLIST = 4,
            WATCHED = 5,
            UNWATCHED = 6,
            COLLECTIONS = 7,
            LISTS = 8,
            UPCOMING = 9,
            NOW_PLAYING = 10,
            POPULAR = 11,
            TOP_RATED = 12,
            LIST_MOVIES = 13,
            COLLECTION_MOVIES = 14;

    // For MovieSortType
    public static final int TITLE = 1,
            RELEASE = 2,
            DURATION = 3,
            RATING = 4,
            DATE_ADDED = 5,
            COLLECTION_TITLE = 7;

    // For saving the sorting type preference
    public static final String SORT_TITLE = "title",
            SORT_RELEASE = "release",
            SORT_RATING = "rating",
            SORT_DATE_ADDED = "date",
            SORT_DURATION = "duration";

    private final Context mContext;
    private final MovieLibraryType mLibraryType;
    private final OnLoadCompletedCallback mCallback;
    private final NMJAdapter mDatabase;
    protected ListView mDrawerList;
    private MovieSortType mSortType;
    private ArrayList<Movie> mResults = new ArrayList<>();
    private HashSet<MovieFilter> mFilters = new HashSet<>();
    private MovieLoaderAsyncTask mAsyncTask;
    private boolean mShowingSearchResults = false;
    private boolean mShowingFilterResults = false;
    private String mListId, mListTmdbId, mCollectionId, mCollectionTmdbId;
    private String mQuery, mQtype;

    public MovieLoader(Context context, MovieLibraryType libraryType, Intent intent, OnLoadCompletedCallback callback) {
        //System.out.println("Entering MovieLoader");
        mContext = context;
        mLibraryType = libraryType;

        if (libraryType == MovieLibraryType.LIST_MOVIES) {
            mListId = intent.getStringExtra("listId");
            mListTmdbId = intent.getStringExtra("listTmdbId");
        } else if (libraryType == MovieLibraryType.COLLECTION_MOVIES) {
            mCollectionId = intent.getStringExtra("collectionId");
            mCollectionTmdbId = intent.getStringExtra("collectionTmdbId");
        }
        mCallback = callback;
        mDatabase = NMJManagerApplication.getNMJAdapter();

        setupSortType();
    }

    /**
     * Get movie library type. Can be either <code>ALL_MOVIES</code>,
     * <code>FAVORITES</code>, <code>NEW_RELEASES</code>,
     * <code>WATCHED</code>, <code>UNWATCHED</code> , <code>COLLECTIONS</code>.
     * <code>NOW_PLAYING</code>, <code>POPULAR</code> , <code>TOP_RATED</code>.
     *
     * @return Movie library type
     */
    public MovieLibraryType getType() {
        return mLibraryType;
    }

    /**
     * Get the movie sort type. Can be either <code>TITLE</code>,
     * <code>RELEASE</code>, <code>DURATION</code>, <code>RATING</code>,
     * <code>WEIGHTED_RATING</code>, <code>DATE_ADDED</code> or <code>COLLECTION_TITLE</code>.
     *
     * @return Movie sort type
     */
    public MovieSortType getSortType() {
        return mSortType;
    }

    /**
     * Set the movie sort type.
     *
     * @param type
     */
    public void setSortType(MovieSortType type) {
        //System.out.println("INput type: " + type);
        //System.out.println("GetSortType " + getSortType());
        if (getSortType() == type) {
            getSortType().toggleSortOrder();
            System.out.println("Saved state: " + getSortType().getSortOrder());
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.putString(PreferenceKeys.SORT_TYPE, getSortType().getSortOrder());
            editor.apply();
        } else {
            mSortType = type;

            // If we're setting a sort type for the "All movies"
            // section, we want to save the sort type as the
            // default way of sorting that section.
            if (getType() == MovieLibraryType.ALL_MOVIES) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                switch (getSortType()) {
                    case TITLE:
                        editor.putString(PreferenceKeys.SORTING_MOVIES, SORT_TITLE);
                        break;
                    case RELEASE:
                        editor.putString(PreferenceKeys.SORTING_MOVIES, SORT_RELEASE);
                        break;
                    case RATING:
                        editor.putString(PreferenceKeys.SORTING_MOVIES, SORT_RATING);
                        break;
                    case DATE_ADDED:
                        editor.putString(PreferenceKeys.SORTING_MOVIES, SORT_DATE_ADDED);
                        break;
                    case DURATION:
                        editor.putString(PreferenceKeys.SORTING_MOVIES, SORT_DURATION);
                        break;
                }
                editor.apply();
            }
        }
    }

    /**
     * Used to know if the MovieLoader is currently
     * showing search results.
     *
     * @return True if showing search results, false otherwise.
     */
    public boolean isShowingSearchResults() {
        return mShowingSearchResults;
    }

    public boolean isShowingFilterResults() {
        return mShowingFilterResults;
    }


    /**
     * Starts loading movies using any active filters,
     * sorting types and settings, i.e. prefix ignoring.
     */
    public void load() {
        load("", "");
    }

    /**
     * Similar to <code>load()</code>, but filters the results
     * based on the search query.
     *
     * @param query
     */
    public void search(String query) {
        load(query, "search");
    }

    public void filter(String query) {
        load(query, "filter");
    }

    /**
     * Starts loading movies.
     *
     * @param query
     */
    private void load(String query, String qType) {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
        mQuery = query;
        mQtype = qType;

        if (qType.equals("search"))
            mShowingSearchResults = !TextUtils.isEmpty(query);
        else if (qType.equals("filter"))
            mShowingFilterResults = !TextUtils.isEmpty(query);

        //System.out.println("Executing MovieLoader");
        if (!NMJLib.getDbPath().equals("")) {
            if (mLibraryType == MovieLibraryType.TOP_RATED || mLibraryType == MovieLibraryType.POPULAR ||
                    mLibraryType == MovieLibraryType.NOW_PLAYING || mLibraryType == MovieLibraryType.UPCOMING)
                mAsyncTask = new MovieLoaderAsyncTask(mQuery, mQtype, 0, 1);
            else
                mAsyncTask = new MovieLoaderAsyncTask(mQuery, mQtype, 0, 25);
            mAsyncTask.execute();
        } else
            mResults.clear();
    }

    public void loadMore(int start, int count) {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = new MovieLoaderAsyncTask(mQuery, mQtype, start, count);
        mAsyncTask.execute();
    }

    public void clearAll() {
        mResults.clear();
    }

    /**
     * Get the results of the most recently loaded movies.
     *
     * @return List of movie objects.
     */
    public ArrayList<Movie> getResults() {
        return mResults;
    }

    /**
     * Sets the sort type depending on the movie
     * library type. The collections library will
     * always be sorted by collection title, the
     * "New releases" library will be sorted by
     * release date and the "All movies" library
     * will be sorted by the user's preference, if
     * such exists. If not, it'll sort by movie title
     * like the other library types do by default.
     */
    private void setupSortType() {
        if (getType() == MovieLibraryType.ALL_MOVIES) {

            // Load the saved sort type and set it
            String savedSortType = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PreferenceKeys.SORTING_MOVIES, SORT_TITLE);

            switch (savedSortType) {
                case SORT_TITLE:
                    setSortType(MovieSortType.TITLE);
                    break;
                case SORT_RELEASE:
                    setSortType(MovieSortType.RELEASE);
                    break;
                case SORT_RATING:
                    setSortType(MovieSortType.RATING);
                    break;
                case SORT_DATE_ADDED:
                    setSortType(MovieSortType.DATE_ADDED);
                    break;
                case SORT_DURATION:
                    setSortType(MovieSortType.DURATION);
                    break;
            }
        } else if (getType() == MovieLibraryType.COLLECTIONS) {
            setSortType(MovieSortType.COLLECTION_TITLE);
        } else if (getType() == MovieLibraryType.NEW_RELEASES) {
            setSortType(MovieSortType.RELEASE);
        } else {
            setSortType(MovieSortType.TITLE);
        }
    }

    /**
     * Handles everything related to loading, filtering, sorting
     * and delivering the callback when everything is finished.
     */
    private class MovieLoaderAsyncTask extends LibrarySectionAsyncTask<Void, Void, Void> {

        private final ArrayList<Movie> mMovieList;
        private final String mSearchQuery, mFilterType;
        private final int mStart, mCount;

        public MovieLoaderAsyncTask(String searchQuery, String filterType, int start, int count) {
            // Lowercase in order to search more efficiently
            if (filterType.equals("search"))
                mSearchQuery = searchQuery.toLowerCase(Locale.getDefault());
            else
                mSearchQuery = searchQuery;
            mFilterType = filterType;

            mStart = start;
            mCount = count;
            mMovieList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            switch (mLibraryType) {
                case ALL_MOVIES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "all movies", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case FAVORITES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "favorites", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case NEW_RELEASES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "new releases", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case WATCHLIST:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "watchlist", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case UNWATCHED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "unwatched", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case WATCHED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "watched", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case COLLECTIONS:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "collections", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case LISTS:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "lists", "", "", mFilterType, mStart, mCount));
                    break;
                case UPCOMING:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie",
                            "upcoming", "", "", "", mStart, mCount));
                    break;
                case NOW_PLAYING:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie",
                            "now_playing", "", "", "", mStart, mCount));
                    break;
                case POPULAR:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie",
                            "popular", "", "", "", mStart, mCount));
                    break;
                case TOP_RATED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie",
                            "top_rated", "", "", "", mStart, mCount));
                    break;
                case LIST_MOVIES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "list", mListId, "", "", mStart, mCount));
                    break;
                case COLLECTION_MOVIES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies",
                            "collection", mCollectionId, "", "", mStart, mCount));
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                ArrayList<Movie> tmpList = new ArrayList<>(mMovieList);
                mResults.addAll(tmpList);
                mCallback.onLoadCompleted();
            } else
                mMovieList.clear();
        }
    }

}
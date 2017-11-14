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

import android.app.Activity;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.text.TextUtils;
import android.widget.ListView;

import com.google.common.collect.Lists;
import com.nmj.apis.nmj.Movie;
import com.nmj.db.DbAdapterMovieMappings;

import com.nmj.functions.Filepath;
import com.nmj.functions.Library;
import com.nmj.functions.LibrarySectionAsyncTask;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJLib;
import com.nmj.functions.PreferenceKeys;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.fragments.MovieLibraryFragment;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

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

    /**
     * Add a movie filter. Filters are unique and only one
     * can be present at a time. It is, however, possible to
     * have multiple filters for different filter types, i.e.
     * two filters for genres.
     *
     * @param filter
     */
    public void addFilter(MovieFilter filter) {
        mFilters.remove(filter);
        mFilters.add(filter);
    }

    /**
     * Clears all filters.
     */
    public void clearFilters() {
        mFilters.clear();
    }

    /**
     * Get all movie filters.
     *
     * @return Set of all currently added movie filters.
     */
    public HashSet<MovieFilter> getFilters() {
        return mFilters;
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

        mShowingSearchResults = !TextUtils.isEmpty(query);
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

    public void clearAll(){
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
     * Show genres filter dialog.
     *
     * @param activity
     */
/*    public void showGenresFilterDialog(Activity activity) {
        final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
        String[] splitGenres;
        for (int i = 0; i < mResults.size(); i++) {
            if (!mResults.get(i).getGenres().isEmpty()) {
                splitGenres = mResults.get(i).getGenres().split(",");
                for (int j = 0; j < splitGenres.length; j++) {
                    if (map.containsKey(splitGenres[j].trim())) {
                        map.put(splitGenres[j].trim(), map.get(splitGenres[j].trim()) + 1);
                    } else {
                        map.put(splitGenres[j].trim(), 1);
                    }
                }
            }
        }

        createAndShowAlertDialog(activity, setupItemArray(map), R.string.selectGenre, MovieFilter.GENRE);
    }*/

    /**
     * Show certifications filter dialog.
     *
     * @param activity
     */
/*    public void showCertificationsFilterDialog(Activity activity) {
        final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
        for (int i = 0; i < mResults.size(); i++) {
            String certification = mResults.get(i).getCertification();
            if (!TextUtils.isEmpty(certification)) {
                if (map.containsKey(certification.trim())) {
                    map.put(certification.trim(), map.get(certification.trim()) + 1);
                } else {
                    map.put(certification.trim(), 1);
                }
            }
        }

        createAndShowAlertDialog(activity, setupItemArray(map), R.string.selectCertification, MovieFilter.CERTIFICATION);
    }*/

    /**
     * Show release year filter dialog.
     *
     * @param activity
     */
/*    public void showReleaseYearFilterDialog(Activity activity) {
        final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
        for (int i = 0; i < mResults.size(); i++) {
            String year = mResults.get(i).getReleaseYear().trim();
            if (!TextUtils.isEmpty(year)) {
                if (map.containsKey(year)) {
                    map.put(year, map.get(year) + 1);
                } else {
                    map.put(year, 1);
                }
            }
        }

        createAndShowAlertDialog(activity, setupItemArray(map), R.string.selectReleaseYear, MovieFilter.RELEASE_YEAR);
    }*/

    /**
     * Used to set up an array of items for the alert dialog.
     *
     * @param map
     * @return List of dialog options.
     */
    private CharSequence[] setupItemArray(TreeMap<String, Integer> map) {
        final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);
        for (int i = 0; i < tempArray.length; i++)
            tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) + ")";

        return tempArray;
    }

    /**
     * Shows an alert dialog and handles the user selection.
     *
     * @param activity
     * @param temp
     * @param title
     * @param type
     */
    private void createAndShowAlertDialog(Activity activity, final CharSequence[] temp, int title, final int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setItems(temp, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Let's get what the user selected and remove the parenthesis at the end
                        String selected = temp[which].toString();
                        selected = selected.substring(0, selected.lastIndexOf("(")).trim();

                        // Add filter
                        MovieFilter filter = new MovieFilter(type);
                        filter.setFilter(selected);
                        addFilter(filter);

                        // Re-load the library with the new filter
                        load();

                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        builder.show();
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
        private final NMJAdapter mTotalCount = new NMJAdapter();
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
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "all", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case FAVORITES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "favorites", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case NEW_RELEASES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "newReleases", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case WATCHLIST:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "watchlist", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case UNWATCHED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "unwatched", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case WATCHED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "watched", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case COLLECTIONS:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "collections", "", mSearchQuery, mFilterType, mStart, mCount));
                    break;
                case LISTS:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "lists", "", "", mFilterType, mStart, mCount));
                    break;
                case UPCOMING:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie", "upcoming", "", "", mFilterType, mStart, mCount));
                    break;
                case NOW_PLAYING:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie", "now_playing", "", "", mFilterType, mStart, mCount));
                    break;
                case POPULAR:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie", "popular", "", "", mFilterType, mStart, mCount));
                    break;
                case TOP_RATED:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "movie", "top_rated", "", "", mFilterType, mStart, mCount));
                    break;
                case LIST_MOVIES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "list", mListId, "", mFilterType, mStart, mCount));
                    break;
                case COLLECTION_MOVIES:
                    mMovieList.addAll(NMJLib.getMovieFromJSON(mContext, "Movies", "collection", mCollectionId, "", mFilterType, mStart, mCount));
                    break;
                default:
                    break;
            }

            /*int totalSize = mMovieList.size();

            for (MovieFilter filter : getFilters()) {
                for (int i = 0; i < totalSize; i++) {

                    if (isCancelled())
                        return null;

                    boolean condition = false;

                    switch (filter.getType()) {
                        case MovieFilter.INDEX:
                            break;
                        case MovieFilter.GENRE:

                            if (mMovieList.get(i).getGenres().contains(filter.getFilter())) {
                                String[] genres = mMovieList.get(i).getGenres().split(",");
                                for (String genre : genres) {
                                    if (genre.trim().equals(filter.getFilter())) {
                                        condition = true;
                                        break;
                                    }
                                }
                            }

                            break;

                        case MovieFilter.CERTIFICATION:

                            condition = mMovieList.get(i).getCertification().trim().equals(filter.getFilter());

                            break;

                        case MovieFilter.RELEASE_YEAR:

                            condition = mMovieList.get(i).getReleaseYear().trim().contains(filter.getFilter());

                            break;

                        case MovieFilter.USER_RATING:
                            break;

                        case MovieFilter.VIDEO_RESOLUTION:
                            break;

                        case MovieFilter.OTHERS:
                            break;
                    }

                    if (!condition && mMovieList.size() > i) {
                        mMovieList.remove(i);
                        i--;
                        totalSize--;
                    }
                }
            }*/

            // If we've got a search query, we should search based on it
            /*if (!TextUtils.isEmpty(mSearchQuery)) {

                ArrayList<NMJMovie> tempCollection = Lists.newArrayList();

                if (mSearchQuery.startsWith("actor:")) {
                    for (int i = 0; i < mMovieList.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (mMovieList.get(i).getCast().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
                            tempCollection.add(mMovieList.get(i));
                    }
                } else if (mSearchQuery.equalsIgnoreCase("missing_genres")) {
                    for (int i = 0; i < mMovieList.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (TextUtils.isEmpty(mMovieList.get(i).getGenres()))
                            tempCollection.add(mMovieList.get(i));
                    }
                } else if (mSearchQuery.equalsIgnoreCase("multiple_versions")) {
                    DbAdapterMovieMappings db = NMJManagerApplication.getMovieMappingAdapter();

                    for (int i = 0; i < mMovieList.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (db.hasMultipleFilepaths(mMovieList.get(i).getId()))
                            tempCollection.add(mMovieList.get(i));
                    }
                } else {
                    Pattern p = Pattern.compile(NMJLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

                    for (int i = 0; i < mMovieList.size(); i++) {
                        if (isCancelled())
                            return null;

                        String lowerCaseTitle = (getType() == MovieLibraryType.COLLECTIONS) ?
                                mMovieList.get(i).getCollectionTitle().toLowerCase(Locale.ENGLISH) :
                                mMovieList.get(i).getTitle().toLowerCase(Locale.ENGLISH);


                        String lowerCaseTitle = mMovieList.get(i).getTitle().toLowerCase(Locale.ENGLISH);
                        boolean foundInTitle = false;

                        if (lowerCaseTitle.contains(mSearchQuery) || p.matcher(lowerCaseTitle).replaceAll("").indexOf(mSearchQuery) != -1) {
                            tempCollection.add(mMovieList.get(i));
                            foundInTitle = true;
                        }

                        if (!foundInTitle) {
                            for (Filepath path : mMovieList.get(i).getFilepaths()) {
                                String filepath = path.getFilepath().toLowerCase(Locale.ENGLISH);
                                if (filepath.indexOf(mSearchQuery) != -1) {
                                    tempCollection.add(mMovieList.get(i));
                                    break; // Break the loop
                                }
                            }
                        }
                    }
                }

                // Clear the movie list
                //mMovieList.clear();

                // Add all the temporary ones after search completed
                mMovieList.addAll(tempCollection);

                // Clear the temporary list
                tempCollection.clear();
            }*/

            // Sort
            //Collections.sort(mMovieList, getSortType().getComparator());

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
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.ListView;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.nmj.db.DbAdapterMovieMappings;

import com.nmj.functions.Filepath;
import com.nmj.functions.LibrarySectionAsyncTask;
import com.nmj.functions.NMJAdapterMovies;
import com.nmj.functions.NMJCache;
import com.nmj.functions.NMJLib;
import com.nmj.functions.NMJMovie;
import com.nmj.functions.PreferenceKeys;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
            TOP_RATED = 12;

    // For MovieSortType
    public static final int TITLE = 1,
            RELEASE = 2,
            DURATION = 3,
            RATING = 4,
            DATE_ADDED = 5,
            COLLECTION_TITLE = 7;

    // For saving the sorting type preference
    public static final String SORT_TITLE = "sortTitle",
            SORT_RELEASE = "sortRelease",
            SORT_RATING = "sortRating",
            SORT_DATE_ADDED = "sortAdded",
            SORT_DURATION = "sortDuration";

    private final Context mContext;
    private final MovieLibraryType mLibraryType;
    private final OnLoadCompletedCallback mCallback;
    private final NMJAdapterMovies mDatabase;
    protected ListView mDrawerList;
    private MovieSortType mSortType;
    private ArrayList<NMJMovie> mResults = new ArrayList<>();
    private HashSet<MovieFilter> mFilters = new HashSet<>();
    private MovieLoaderAsyncTask mAsyncTask;
    private boolean mShowingSearchResults = false;

    public MovieLoader(Context context, MovieLibraryType libraryType, OnLoadCompletedCallback callback) {
        mContext = context;
        mLibraryType = libraryType;
        mCallback = callback;
        mDatabase = NMJManagerApplication.getNMJMovieAdapter();

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
        if (getSortType() == type) {
            getSortType().toggleSortOrder();
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
        load("");
    }

    /**
     * Similar to <code>load()</code>, but filters the results
     * based on the search query.
     *
     * @param query
     */
    public void search(String query) {
        load(query);
    }

    /**
     * Starts loading movies.
     *
     * @param query
     */
    private void load(String query) {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }

        mShowingSearchResults = !TextUtils.isEmpty(query);

        mAsyncTask = new MovieLoaderAsyncTask(query);
        mAsyncTask.execute();
    }

    /**
     * Creates movie objects from a URL and adds them to a list.
     *
     * @param loadType
     * @return List of movie objects from the supplied URL.
     */
    private ArrayList<NMJMovie> listFromTMDB(String loadType) {
        ArrayList<NMJMovie> list = new ArrayList<>();
        String url = "http://api.themoviedb.org/3/movie/" + loadType + "?api_key=b626260be86175272e48fa6347e58100&language=en";
        try {
            JSONObject jObject;
            LoadingCache<String, String> JSONCache = NMJCache.getLoadingCache();
            if (JSONCache.get(loadType) == "") {
                jObject = NMJLib.getJSONObject(mContext, url);
                JSONCache.put(loadType, jObject.toString());
                System.out.println("Putting Cache in " + loadType);
            } else {
                jObject = new JSONObject(JSONCache.get(loadType));
                System.out.println("Getting Cache from " + loadType);
            }
            JSONArray jArray = jObject.getJSONArray("results");

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject dObject = jArray.getJSONObject(i);
                list.add(new NMJMovie(mContext,
                        dObject.getString("title"),
                        dObject.getString("id"),
                        dObject.getString("vote_average"),
                        dObject.getString("release_date"),
                        "", //KEY_GENRES
                        "", //KEY_FAVOURITE
                        "", //KEY_COLLECTION_ID
                        "", //KEY_COLLECTION_ID
                        "", //KEY_TO_WATCH
                        "", //KEY_HAS_WATCHED
                        "", //KEY_DATE_ADDED
                        "", //CERTIFICATION
                        "", //RUNTIME
                        "0", //SHOW_ID
                        dObject.getString("poster_path"),
                        true));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * Creates movie objects from a URL and adds them to a list.
     *
     * @param loadType
     * @return List of movie objects from the supplied URL.
     */
    private ArrayList<NMJMovie> listFromJSON(String loadType) {
        ArrayList<NMJMovie> list = new ArrayList<>();
        String url = "http://www.pchportal.duckdns.org/NMJManagerTablet_web/gd.php?action=getVideos&drivepath=guerilla&dbpath=guerilla/nmj_database/media.db&orderby=asc&filterby=All&sortby=title&load=" + loadType + "&TYPE=Movies&VALUE=&searchtype=title";

        try {
            JSONObject jObject;
            LoadingCache<String, String> JSONCache = NMJCache.getLoadingCache();
            if (JSONCache.get(loadType) == "") {
                jObject = NMJLib.getJSONObject(mContext, url);
                JSONCache.put(loadType, jObject.toString());
                System.out.println("Putting Cache in " + loadType);
            } else {
                jObject = new JSONObject(JSONCache.get(loadType));
                System.out.println("Getting Cache from " + loadType);
            }
            JSONArray jArray = jObject.getJSONArray("data");

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject dObject = jArray.getJSONObject(i);
                list.add(new NMJMovie(mContext,
                        dObject.getString("TITLE"),
                        dObject.getString("CONTENT_TTID"),
                        dObject.getString("RATING"),
                        dObject.getString("RELEASE_DATE"),
                        dObject.getString("RATING"), //KEY_GENRES
                        dObject.getString("RATING"), //KEY_FAVOURITE
                        dObject.getString("SHOW_ID"), //KEY_COLLECTION_ID
                        dObject.getString("SHOW_ID"), //KEY_COLLECTION_ID
                        dObject.getString("SHOW_ID"), //KEY_TO_WATCH
                        dObject.getString("PLAY_COUNT"), //KEY_HAS_WATCHED
                        dObject.getString("SHOW_ID"), //KEY_DATE_ADDED
                        dObject.getString("PARENTAL_CONTROL"),
                        dObject.getString("RUNTIME"), //RUNTIME
                        dObject.getString("SHOW_ID"),
                        dObject.getString("THUMBNAIL"),
                        true));
            }
        } catch (Exception ignored) {
        }

        return list;
    }

    /**
     * Creates movie objects from a URL and adds them to a list.
     *
     * @return List of movie objects from the supplied URL.
     */
    private void setLibrary() {
        ArrayList<NMJMovie> list = new ArrayList<>();
        String url = "http://www.pchportal.duckdns.org/NMJManagerTablet_web/gd.php?action=getCount&drivepath=guerilla&dbpath=guerilla/nmj_database/media.db";

        try {
            JSONObject jObject = new JSONObject();
            LoadingCache<String, String> JSONCache = NMJCache.getLoadingCache();
            if (JSONCache.get("libCount") == "") {
                jObject = NMJLib.getJSONObject(mContext, url);
                JSONCache.put("libCount", jObject.toString());
                System.out.println("Putting Cache in libCount");
            } else {
                jObject = new JSONObject(JSONCache.get("libCount"));
                System.out.println("Getting Cache from libCount");
            }
            mDatabase.setMovieCount(Integer.parseInt(jObject.getJSONObject("data").getJSONObject("count").getString("movies")));
            mDatabase.setShowCount(Integer.parseInt(jObject.getJSONObject("data").getJSONObject("count").getString("shows")));
            //mDatabase.setLibrary(jObject.getJSONObject("data").getJSONArray("library"));
            //JSONArray jArray = jObject.getJSONArray("data");
            //com.nmj.nmjmanager.Main.

        } catch (Exception ignored) {
        }
        //return jObject;
    }


    /**
     * Get the results of the most recently loaded movies.
     *
     * @return List of movie objects.
     */
    public ArrayList<NMJMovie> getResults() {
        return mResults;
    }

    /**
     * Show genres filter dialog.
     *
     * @param activity
     */
    public void showGenresFilterDialog(Activity activity) {
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
    }

    /**
     * Show certifications filter dialog.
     *
     * @param activity
     */
    public void showCertificationsFilterDialog(Activity activity) {
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
    }

    /**
     * Show release year filter dialog.
     *
     * @param activity
     */
    public void showReleaseYearFilterDialog(Activity activity) {
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
    }

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

        private final ArrayList<NMJMovie> mMovieList;
        private final NMJAdapterMovies mTotalCount = new NMJAdapterMovies();
        private final String mSearchQuery;

        public MovieLoaderAsyncTask(String searchQuery) {
            // Lowercase in order to search more efficiently
            mSearchQuery = searchQuery.toLowerCase(Locale.getDefault());

            mMovieList = new ArrayList<NMJMovie>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            setLibrary();
            switch (mLibraryType) {
                case ALL_MOVIES:
                    mMovieList.addAll(listFromJSON("all"));
                    break;
                case FAVORITES:
                    mMovieList.addAll(listFromJSON("favorites"));
                    break;
                case NEW_RELEASES:
                    mMovieList.addAll(listFromJSON("newReleases"));
                    break;
                case WATCHLIST:
                    mMovieList.addAll(listFromJSON("watchlist"));
                    break;
                case UNWATCHED:
                    mMovieList.addAll(listFromJSON("unwatched"));
                    break;
                case WATCHED:
                    mMovieList.addAll(listFromJSON("watched"));
                    break;
                case COLLECTIONS:
                    mMovieList.addAll(listFromJSON("collections"));
                    break;
                case LISTS:
                    mMovieList.addAll(listFromJSON("lists"));
                    break;
                case UPCOMING:
                    mMovieList.addAll(listFromTMDB("upcoming"));
                    break;
                case NOW_PLAYING:
                    mMovieList.addAll(listFromTMDB("now_playing"));
                    break;
                case POPULAR:
                    mMovieList.addAll(listFromTMDB("popular"));
                    break;
                case TOP_RATED:
                    mMovieList.addAll(listFromTMDB("top_rated"));
                default:
                    break;
            }

            int totalSize = mMovieList.size();

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

                        /*case MovieFilter.FILE_SOURCE:

                            for (Filepath path : mMovieList.get(i).getFilepaths()) {
                                condition = path.getTypeAsString(mContext).equals(filter.getFilter());
                                if (condition)
                                    break;
                            }

                            break;*/

                        case MovieFilter.RELEASE_YEAR:

                            condition = mMovieList.get(i).getReleaseYear().trim().contains(filter.getFilter());

                            break;

                        case MovieFilter.USER_RATING:
                            break;

                        case MovieFilter.VIDEO_RESOLUTION:
                            break;

                        case MovieFilter.OTHERS:
                            break;

                        /*case MovieFilter.FOLDER:

                            for (Filepath path : mMovieList.get(i).getFilepaths()) {
                                condition = path.getFilepath().trim().startsWith(filter.getFilter());
                                if (condition)
                                    break;
                            }

                            break;

                        case MovieFilter.OFFLINE_FILES:

                            for (Filepath path : mMovieList.get(i).getFilepaths()) {
                                condition = mMovieList.get(i).hasOfflineCopy(path);
                                if (condition)
                                    break;
                            }

                            break;

                        case MovieFilter.AVAILABLE_FILES:

                            ArrayList<FileSource> filesources = NMJLib.getFileSources(NMJLib.TYPE_MOVIE, true);

                            if (isCancelled())
                                return null;

                            for (Filepath path : mMovieList.get(i).getFilepaths()) {
                                if (path.isNetworkFile())
                                    if (mMovieList.get(i).hasOfflineCopy(path)) {
                                        condition = true;
                                        break; // break inner loop to continue to the next movie
                                    } else {
                                        if (path.getType() == FileSource.SMB) {
                                            if (NMJLib.isWifiConnected(mContext)) {
                                                FileSource source = null;

                                                for (int j = 0; j < filesources.size(); j++)
                                                    if (path.getFilepath().contains(filesources.get(j).getFilepath())) {
                                                        source = filesources.get(j);
                                                        break;
                                                    }

                                                if (source == null)
                                                    continue;

                                                try {
                                                    final SmbFile file = new SmbFile(
                                                            NMJLib.createSmbLoginString(
                                                                    source.getDomain(),
                                                                    source.getUser(),
                                                                    source.getPassword(),
                                                                    path.getFilepath(),
                                                                    false
                                                            ));
                                                    if (file.exists()) {
                                                        condition = true;
                                                        break; // break inner loop to continue to the next movie
                                                    }
                                                } catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
                                            }
                                        } else if (path.getType() == FileSource.UPNP) {
                                            if (NMJLib.exists(path.getFilepath())) {
                                                condition = true;
                                                break; // break inner loop to continue to the next movie
                                            }
                                        }
                                    } else {
                                    if (new File(path.getFilepath()).exists()) {
                                        condition = true;
                                        break; // break inner loop to continue to the next movie
                                    }
                                }
                            }

                            break;
                            */
                    }

                    if (!condition && mMovieList.size() > i) {
                        mMovieList.remove(i);
                        i--;
                        totalSize--;
                    }
                }
            }

            // If we've got a search query, we should search based on it
            if (!TextUtils.isEmpty(mSearchQuery)) {

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

                        if (db.hasMultipleFilepaths(mMovieList.get(i).getTmdbId()))
                            tempCollection.add(mMovieList.get(i));
                    }
                } else {
                    Pattern p = Pattern.compile(NMJLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

                    for (int i = 0; i < mMovieList.size(); i++) {
                        if (isCancelled())
                            return null;

                        String lowerCaseTitle = (getType() == MovieLibraryType.COLLECTIONS) ?
                                mMovieList.get(i).getCollection().toLowerCase(Locale.ENGLISH) :
                                mMovieList.get(i).getTitle().toLowerCase(Locale.ENGLISH);

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
                mMovieList.clear();

                // Add all the temporary ones after search completed
                mMovieList.addAll(tempCollection);

                // Clear the temporary list
                tempCollection.clear();
            }

            // Sort
            Collections.sort(mMovieList, getSortType().getComparator());

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mResults = new ArrayList<>(mMovieList);
                mCallback.onLoadCompleted();
            } else
                mMovieList.clear();
        }
    }

}
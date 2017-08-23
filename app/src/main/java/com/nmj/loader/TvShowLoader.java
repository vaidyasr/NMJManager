package com.nmj.loader;/*
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.common.collect.Lists;
import com.iainconnor.objectcache.CacheManager;
import com.nmj.db.DbAdapterTvShowEpisodes;
import com.nmj.db.DbAdapterTvShows;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.functions.FileSource;
import com.nmj.functions.Filepath;
import com.nmj.functions.LibrarySectionAsyncTask;
import com.nmj.functions.NMJLib;
import com.nmj.functions.NMJMovie;
import com.nmj.functions.PreferenceKeys;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.TvShow;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jcifs.smb.SmbFile;

public class TvShowLoader {

    // For TvShowLibraryType
    public static final int ALL_SHOWS = 1,
            FAVORITES = 2,
            RECENTLY_AIRED = 3,
            WATCHED = 4,
            UNWATCHED = 5,
            POPULAR = 6,
            TOP_RATED = 7,
            ON_TV = 8,
            AIRING_TODAY = 9;

    // For TvShowSortType
    public static final int TITLE = 1,
            FIRST_AIR_DATE = 2,
            NEWEST_EPISODE = 3,
            DURATION = 4,
            RATING = 5,
            WEIGHTED_RATING = 6;

    // For saving the sorting type preference
    public static final String SORT_TITLE = "sortTitle",
            SORT_RELEASE = "sortRelease",
            SORT_RATING = "sortRating",
            SORT_NEWEST_EPISODE = "sortNewestEpisode",
            SORT_DURATION = "sortDuration";

    private final Context mContext;
    private final TvShowLibraryType mLibraryType;
    private final OnLoadCompletedCallback mCallback;
    private final DbAdapterTvShows mTvShowDatabase;
    private final DbAdapterTvShowEpisodes mTvShowEpisodeDatabase;

    private TvShowSortType mSortType;
    private ArrayList<NMJMovie> mResults = new ArrayList<>();
    private HashSet<TvShowFilter> mFilters = new HashSet<>();
    private TvShowLoaderAsyncTask mAsyncTask;
    private boolean mIgnorePrefixes = false,
            mShowingSearchResults = false;

    public TvShowLoader(Context context, TvShowLibraryType libraryType, OnLoadCompletedCallback callback) {
        mContext = context;
        mLibraryType = libraryType;
        mCallback = callback;
        mTvShowDatabase = NMJManagerApplication.getTvDbAdapter();
        mTvShowEpisodeDatabase = NMJManagerApplication.getTvEpisodeDbAdapter();

        setupSortType();
    }

    /**
     * Get TV show library type. Can be either <code>ALL_SHOWS</code>,
     * <code>FAVORITES</code>, <code>NEWLY_AIRED</code>, <code>WATCHED</code>
     * or <code>UNWATCHED</code>.
     * @return TV show library type
     */
    public TvShowLibraryType getType() {
        return mLibraryType;
    }

    /**
     * Determine whether the TvShowLoader should ignore title prefixes.
     * @param ignore
     */
    public void setIgnorePrefixes(boolean ignore) {
        mIgnorePrefixes = ignore;
    }

    /**
     * Add a TV show filter. Filters are unique and only one
     * can be present at a time. It is, however, possible to
     * have multiple filters for different filter types, i.e.
     * two filters for genres.
     * @param filter
     */
    public void addFilter(TvShowFilter filter) {
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
     * Get all TV show filters.
     * @return Set of all currently added TV show filters.
     */
    public HashSet<TvShowFilter> getFilters() {
        return mFilters;
    }

    /**
     * Get the TV show sort type. Can be either <code>TITLE</code>,
     * <code>FIRST_AIR_DATE</code>, <code>NEWEST_EPISODE</code>, <code>DURATION</code>,
     * <code>RATING</code>, or <code>WEIGHTED_RATING</code>.
     * @return TV show sort type
     */
    public TvShowSortType getSortType() {
        return mSortType;
    }

    /**
     * Set the TV show sort type.
     * @param type
     */
    public void setSortType(TvShowSortType type) {
        if (getSortType() == type) {
            getSortType().toggleSortOrder();
        } else {
            mSortType = type;

            // If we're setting a sort type for the "All shows"
            // section, we want to save the sort type as the
            // default way of sorting that section.
            if (getType() == TvShowLibraryType.ALL_SHOWS) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                switch (getSortType()) {
                    case TITLE:
                        editor.putString(PreferenceKeys.SORTING_TVSHOWS, SORT_TITLE);
                        break;
                    case FIRST_AIR_DATE:
                        editor.putString(PreferenceKeys.SORTING_TVSHOWS, SORT_RELEASE);
                        break;
                    case NEWEST_EPISODE:
                        editor.putString(PreferenceKeys.SORTING_TVSHOWS, SORT_NEWEST_EPISODE);
                        break;
                    case DURATION:
                        editor.putString(PreferenceKeys.SORTING_TVSHOWS, SORT_DURATION);
                        break;
                    case RATING:
                        editor.putString(PreferenceKeys.SORTING_TVSHOWS, SORT_RATING);
                        break;
                }
                editor.apply();
            }
        }
    }

    /**
     * Starts loading TV shows using any active filters,
     * sorting types and settings, i.e. prefix ignoring.
     */
    public void load() {
        load("");
    }

    /**
     * Similar to <code>load()</code>, but filters the results
     * based on the search query.
     * @param query
     */
    public void search(String query) {
        load(query);
    }

    /**
     * Starts loading TV shows.
     * @param query
     */
    private void load(String query) {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }

        mShowingSearchResults = !TextUtils.isEmpty(query);

        mAsyncTask = new TvShowLoaderAsyncTask(query);
        mAsyncTask.execute();
    }

    /**
     * Used to know if the TvShowLoader is currently
     * showing search results.
     * @return True if showing search results, false otherwise.
     */
    public boolean isShowingSearchResults() {
        return mShowingSearchResults;
    }

    /**
     * Get the results of the most recently loaded TV shows.
     * @return List of TV show objects.
     */
    public ArrayList<NMJMovie> getResults() {
        return mResults;
    }

    /**
     * Show genres filter dialog.
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

        createAndShowAlertDialog(activity, setupItemArray(map, false), R.string.selectGenre, TvShowFilter.GENRE);
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

        createAndShowAlertDialog(activity, setupItemArray(map, false), R.string.selectCertification, TvShowFilter.CERTIFICATION);
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

        createAndShowAlertDialog(activity, setupItemArray(map, false), R.string.selectReleaseYear, TvShowFilter.RELEASE_YEAR);
    }

    /**
     * Used to set up an array of items for the alert dialog.
     *
     * @param map
     * @return List of dialog options.
     */
    private CharSequence[] setupItemArray(TreeMap<String, Integer> map, boolean addFilesPostfix) {
        final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);
        for (int i = 0; i < tempArray.length; i++)
            tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) +
                    (addFilesPostfix ? " " + mContext.getResources().getQuantityString(R.plurals.files, map.get(tempArray[i])) + ")" : ")");

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
                        TvShowFilter filter = new TvShowFilter(type);
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

    private void setupSortType() {
        if (getType() == TvShowLibraryType.ALL_SHOWS) {

            // Load the saved sort type and set it
            String savedSortType = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PreferenceKeys.SORTING_TVSHOWS, SORT_TITLE);

            switch (savedSortType) {
                case SORT_TITLE:
                    setSortType(TvShowSortType.TITLE);
                    break;
                case SORT_RELEASE:
                    setSortType(TvShowSortType.FIRST_AIR_DATE);
                    break;
                case SORT_RATING:
                    setSortType(TvShowSortType.RATING);
                    break;
                case SORT_NEWEST_EPISODE:
                    setSortType(TvShowSortType.NEWEST_EPISODE);
                    break;
                case SORT_DURATION:
                    setSortType(TvShowSortType.DURATION);
                    break;
            }
        } else if (getType() == TvShowLibraryType.RECENTLY_AIRED) {
            setSortType(TvShowSortType.NEWEST_EPISODE);
        } else {
            setSortType(TvShowSortType.TITLE);
        }
    }

    /**
     * Handles everything related to loading, filtering, sorting
     * and delivering the callback when everything is finished.
     */
    private class TvShowLoaderAsyncTask extends LibrarySectionAsyncTask<Void, Void, Void> {

        private final ArrayList<NMJMovie> mTvShowList;
        private final String mSearchQuery;

        public TvShowLoaderAsyncTask(String searchQuery) {
            // Lowercase in order to search more efficiently
            mSearchQuery = searchQuery.toLowerCase(Locale.getDefault());

            mTvShowList = new ArrayList<NMJMovie>();
        }

        @Override
        protected Void doInBackground(Void... params) {

            switch (mLibraryType) {
                case ALL_SHOWS:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "TV Shows", "all", ""));
                    break;
                case FAVORITES:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "TV Shows", "favorites", ""));
                    break;
                case RECENTLY_AIRED:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "TV Shows", "newReleases", ""));
                    break;
                case UNWATCHED:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "TV Shows", "unwatched", ""));
                    break;
                case WATCHED:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "TV Shows", "watched", ""));
                    break;
                case POPULAR:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "tv", "popular", ""));
                    break;
                case TOP_RATED:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "tv", "top_rated", ""));
                    break;
                case ON_TV:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "tv", "on_the_air", ""));
                    break;
                case AIRING_TODAY:
                    mTvShowList.addAll(NMJLib.getVideoFromJSON(mContext, "tv", "airing_today", ""));
                    break;
                default:
                    break;
            }

            int totalSize = mTvShowList.size();

            for (TvShowFilter filter : getFilters()) {
                for (int i = 0; i < totalSize; i++) {

                    if (isCancelled())
                        return null;

/*                    ArrayList<Filepath> paths = new ArrayList<>();
                    for (String s : NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter()
                            .getFilepathsForShow(mTvShowList.get(i).getId())) {
                        paths.add(new Filepath(s));
                    }*/

                    boolean condition = false;

                    switch (filter.getType()) {
                        case TvShowFilter.GENRE:

                            if (mTvShowList.get(i).getGenres().contains(filter.getFilter())) {
                                String[] genres = mTvShowList.get(i).getGenres().split(",");
                                for (String genre : genres) {
                                    if (genre.trim().equals(filter.getFilter())) {
                                        condition = true;
                                        break;
                                    }
                                }
                            }

                            break;

                        case TvShowFilter.CERTIFICATION:

                            condition = mTvShowList.get(i).getCertification().trim().equals(filter.getFilter());

                            break;

                        /*case TvShowFilter.FILE_SOURCE:

                            for (Filepath path : paths) {
                                condition = path.getTypeAsString(mContext).equals(filter.getFilter());
                                if (condition)
                                    break;
                            }

                            break;*/

                        case TvShowFilter.RELEASE_YEAR:

                            condition = mTvShowList.get(i).getReleaseYear().trim().contains(filter.getFilter());

                            break;

                        /*case TvShowFilter.FOLDER:

                            for (Filepath path : paths) {
                                condition = path.getFilepath().trim().startsWith(filter.getFilter());
                                if (condition)
                                    break;
                            }

                            break;

                        case TvShowFilter.OFFLINE_FILES:

                            for (Filepath path : paths) {
                                condition = mTvShowList.get(i).hasOfflineCopy(path);
                                if (condition)
                                    break;
                            }

                            break;

                        case TvShowFilter.AVAILABLE_FILES:

                            ArrayList<FileSource> filesources = NMJLib.getFileSources(NMJLib.TYPE_SHOWS, true);

                            if (isCancelled())
                                return null;

                            for (Filepath path : paths) {
                                if (path.isNetworkFile())
                                    if (mTvShowList.get(i).hasOfflineCopy(path)) {
                                        condition = true;
                                        break; // break inner loop to continue to the next TV show
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
                                                        break; // break inner loop to continue to the next TV show
                                                    }
                                                } catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
                                            }
                                        } else if (path.getType() == FileSource.UPNP) {
                                            if (NMJLib.exists(path.getFilepath())) {
                                                condition = true;
                                                break; // break inner loop to continue to the next TV show
                                            }
                                        }
                                    } else {
                                    if (new File(path.getFilepath()).exists()) {
                                        condition = true;
                                        break; // break inner loop to continue to the next TV show
                                    }
                                }
                            }

                            break;*/
                    }

                    if (!condition && mTvShowList.size() > i) {
                        mTvShowList.remove(i);
                        i--;
                        totalSize--;
                    }
                }
            }

            // If we've got a search query, we should search based on it
            if (!TextUtils.isEmpty(mSearchQuery)) {

                ArrayList<NMJMovie> tempCollection = Lists.newArrayList();

/*
                if (mSearchQuery.startsWith("actor:")) {
                    for (int i = 0; i < mTvShowList.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (mTvShowList.get(i).getActors().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
                            tempCollection.add(mTvShowList.get(i));
                    }
                } else if (mSearchQuery.equalsIgnoreCase("missing_genres")) {
                    for (int i = 0; i < mTvShowList.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (TextUtils.isEmpty(mTvShowList.get(i).getGenres()))
                            tempCollection.add(mTvShowList.get(i));
                    }
                } else {
                    Pattern p = Pattern.compile(NMJLib.CHARACTER_REGEX);

                    for (int i = 0; i < mTvShowList.size(); i++) {
                        if (isCancelled())
                            return null;

                        String lowerCaseTitle = mTvShowList.get(i).getTitle().toLowerCase(Locale.ENGLISH);

                        if (lowerCaseTitle.contains(mSearchQuery) || p.matcher(lowerCaseTitle).replaceAll("").indexOf(mSearchQuery) != -1) {
                            tempCollection.add(mTvShowList.get(i));
                        }
                    }
                }
*/

                // Clear the TV show list
                mTvShowList.clear();

                // Add all the temporary ones after search completed
                mTvShowList.addAll(tempCollection);

                // Clear the temporary list
                tempCollection.clear();
            }

            // Sort
            //Collections.sort(mTvShowList, getSortType().getComparator());

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mResults = new ArrayList<>(mTvShowList);
                mCallback.onLoadCompleted();
            } else
                mTvShowList.clear();
        }
    }
}

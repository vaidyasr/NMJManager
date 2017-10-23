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

package com.nmj.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.nmj.nmjmanager.NMJManagerApplication;

public class LocalBroadcastUtils {

	public static final String UPDATE_MOVIE_LIBRARY = "NMJManager-movies-update";
	public static final String UPDATE_MOVIE_DETAIL = "NMJManager-movie-detail-update";
	public static final String LOAD_MOVIE_LIBRARY = "NMJManager-movies-load";
	public static final String LOAD_TV_SHOW_LIBRARY = "NMJManager-tvshows-load";
	public static final String UPDATE_TV_SHOW_LIBRARY = "NMJManager-tvshows-update";
	public static final String UPDATE_LIBRARY_COUNT = "NMJManager-update-library-count";
	public static final String RELOAD_MOVIE_FRAGMENT = "NMJManager-reload-movie-fragment";
	public static final String RELOAD_SHOW_FRAGMENT = "NMJManager-reload-show-fragment";
	public static final String CLEAR_IMAGE_CACHE = "clear-image-cache";
    public static final String UPDATE_TV_SHOW_SEASONS_OVERVIEW = "NMJManager-tvshows-seasons-update";
    public static final String UPDATE_TV_SHOW_EPISODES_OVERVIEW = "NMJManager-tvshows-episodes-update";
    public static final String UPDATE_TV_SHOW_EPISODE_DETAILS_OVERVIEW = "NMJManager-tvshows-episode-details-update";
	public static final String UPDATE_MOVIE_TABS = "NMJManager-movie-tabs";
	
	private LocalBroadcastUtils() {} // No instantiation
	
	/**
	 * Force the movie library to clear the cache and reload everything.
	 * @param context
	 */
	public static void updateMovieLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_MOVIE_LIBRARY));
	}

	public static void updateMovieDetail(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_MOVIE_DETAIL));
	}

	public static void loadMovieLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(LOAD_MOVIE_LIBRARY));
	}

	public static void updateLibraryCount(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_LIBRARY_COUNT));
	}

	public static void reloadMovieFragment(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(RELOAD_MOVIE_FRAGMENT));
	}

	public static void reloadTVShowFragment(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(RELOAD_SHOW_FRAGMENT));
	}
	/**
	 * Force the TV show library to clear the cache and reload everything.
	 * @param context
	 */
	public static void updateTvShowLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_LIBRARY));
	}

	public static void loadTvShowLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(LOAD_TV_SHOW_LIBRARY));
	}
	/**
	 * Clear the image cache.
	 * @param context
	 */
	public static void clearImageCache(Context context) {
		NMJManagerApplication.clearLruCache(context);
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(CLEAR_IMAGE_CACHE));
	}

    /**
     * Force the TV show seasons overview to reload.
     * @param context
     */
    public static void updateTvShowSeasonsOverview(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_SEASONS_OVERVIEW));
    }

    /**
     * Force the TV show episodes overview to reload.
     * @param context
     */
    public static void updateTvShowEpisodesOverview(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_EPISODES_OVERVIEW));
    }

    /**
     * Force the TV show details view to reload.
     * @param context
     */
    public static void updateTvShowEpisodeDetailsView(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_EPISODE_DETAILS_OVERVIEW));
    }
}
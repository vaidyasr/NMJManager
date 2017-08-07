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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.MediumMovie;
import com.nmj.functions.Movie;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;

import java.util.ArrayList;
import java.util.List;

import static com.nmj.functions.PreferenceKeys.REMOVE_MOVIES_FROM_WATCHLIST;

public class MovieDatabaseUtils {

	private MovieDatabaseUtils() {} // No instantiation

	/**
	 * Delete all movies in the various database tables and remove all related image files.
	 * @param context
	 */
	public static void deleteAllMovies(Context context) {
		// Delete all movies
		NMJManagerApplication.getMovieAdapter().deleteAllMovies();
		
		// Delete all movie filepath mappings
		NMJManagerApplication.getMovieMappingAdapter().deleteAllMovies();
		
		// Delete all movie collections
		NMJManagerApplication.getCollectionsAdapter().deleteAllCollections();

		// Delete all downloaded image files from the device
		FileUtils.deleteRecursive(NMJManagerApplication.getMovieThumbFolder(context), false);
		FileUtils.deleteRecursive(NMJManagerApplication.getMovieBackdropFolder(context), false);
	}

	public static void removeAllUnidentifiedFiles() {
		NMJManagerApplication.getMovieMappingAdapter().deleteAllUnidentifiedFilepaths();
	}

    public static void deleteMovie(Context context, String tmdbId) {
        if (TextUtils.isEmpty(tmdbId))
            return;

        DbAdapterMovies db = NMJManagerApplication.getMovieAdapter();

        // Delete the movie details
        db.deleteMovie(tmdbId);

        // When deleting a movie, check if there's other movies
        // linked to the collection - if not, delete the collection entry
        String collectionId = db.getCollectionId(tmdbId);
        if (NMJManagerApplication.getCollectionsAdapter().getMovieCount(collectionId) == 1)
            NMJManagerApplication.getCollectionsAdapter().deleteCollection(collectionId);

        // Finally, delete all filepath mappings to this movie ID
        NMJManagerApplication.getMovieMappingAdapter().deleteMovie(tmdbId);
    }

    public static void ignoreMovie(String tmdbId) {
        if (TextUtils.isEmpty(tmdbId))
            return;

        DbAdapterMovies db = NMJManagerApplication.getMovieAdapter();

        // We delete the movie...
        db.deleteMovie(tmdbId);

        // ... check if there's other movies linked to the collection
        // - if not, we delete the collection entry as well
        String collectionId = db.getCollectionId(tmdbId);
        if (NMJManagerApplication.getCollectionsAdapter().getMovieCount(collectionId) == 1)
            NMJManagerApplication.getCollectionsAdapter().deleteCollection(collectionId);

        // And finally we make sure that NMJManager ignores the filepath in the future
        NMJManagerApplication.getMovieMappingAdapter().ignoreMovie(tmdbId);
    }

    public static void setMoviesFavourite(final Context context,
                                        final List<MediumMovie> movies,
                                        boolean favourite) {
        boolean success = true;

        for (MediumMovie movie : movies)
            success = success && NMJManagerApplication.getMovieAdapter().updateMovieSingleItem(movie.getTmdbId(),
                    DbAdapterMovies.KEY_FAVOURITE, favourite ? "1" : "0");

        if (success)
            if (favourite)
                Toast.makeText(context, context.getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                Trakt.moviesFavorite(movies, context);
            }
        }.start();
    }

    public static void setMoviesWatched(final Context context,
                                        final List<MediumMovie> movies,
                                        boolean watched) {
        boolean success = true;

        for (MediumMovie movie : movies)
            success = success && NMJManagerApplication.getMovieAdapter().updateMovieSingleItem(movie.getTmdbId(),
                    DbAdapterMovies.KEY_HAS_WATCHED, watched ? "1" : "0");

        if (success)
            if (watched) {
                Toast.makeText(context, context.getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();

                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(REMOVE_MOVIES_FROM_WATCHLIST, true))
                    setMoviesWatchlist(context, movies, false); // False to remove from watchlist
            } else
                Toast.makeText(context, context.getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                Trakt.markMoviesAsWatched(movies, context);
            }
        }.start();
    }

    public static void setMoviesWatchlist(final Context context,
                                        final List<MediumMovie> movies,
                                        boolean toWatch) {
        boolean success = true;

        for (MediumMovie movie : movies)
            success = success && NMJManagerApplication.getMovieAdapter().updateMovieSingleItem(movie.getTmdbId(),
                    DbAdapterMovies.KEY_TO_WATCH, toWatch ? "1" : "0");

        if (success)
            if (toWatch)
                Toast.makeText(context, context.getString(R.string.addedToWatchList), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.removedFromWatchList), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                Trakt.moviesWatchlist(movies, context);
            }
        }.start();
    }
}
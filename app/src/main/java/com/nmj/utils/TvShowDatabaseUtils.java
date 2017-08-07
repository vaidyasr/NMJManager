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
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterTvShowEpisodeMappings;
import com.nmj.db.DbAdapterTvShowEpisodes;
import com.nmj.db.DbAdapterTvShows;
import com.nmj.functions.GridEpisode;
import com.nmj.functions.NMJLib;
import com.nmj.functions.PreferenceKeys;
import com.nmj.functions.TvShowEpisode;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.TvShow;

import java.util.ArrayList;
import java.util.List;

public class TvShowDatabaseUtils {

	private TvShowDatabaseUtils() {} // No instantiation

    /**
     * Delete all TV shows in the various database tables and remove all related image files.
     * @param context
     */
    public static void deleteAllTvShows(Context context) {
        // Delete all movies
        NMJManagerApplication.getTvDbAdapter().deleteAllShowsInDatabase();

        // Delete all episodes
        NMJManagerApplication.getTvEpisodeDbAdapter().deleteAllEpisodes();

        // Delete all episode filepath mappings
        NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().deleteAllFilepaths();

        // Delete all downloaded image files from the device
        FileUtils.deleteRecursive(NMJManagerApplication.getTvShowThumbFolder(context), false);
        FileUtils.deleteRecursive(NMJManagerApplication.getTvShowBackdropFolder(context), false);
        FileUtils.deleteRecursive(NMJManagerApplication.getTvShowSeasonFolder(context), false);
        FileUtils.deleteRecursive(NMJManagerApplication.getTvShowEpisodeFolder(context), false);
    }

	/**
	 * Remove all database entries and related images for a given TV show season.
	 * This also checks if the TV show has any other seasons - if not, it'll remove
	 * the TV show database entry as well.
	 */
	public static void deleteSeason(Context context, String showId, int season) {
		// Should filepaths be removed completely or ignored in future library updates?
		boolean ignoreFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.IGNORED_FILES_ENABLED, false);

		// Database adapters
		DbAdapterTvShows showAdapter = NMJManagerApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = NMJManagerApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter();

		// Get a List of episodes in the season
		List<GridEpisode> episodesInSeason = episodeAdapter.getEpisodesInSeason(context, showId, season);

		// Remove all episodes from the season
		episodeAdapter.deleteSeason(showId, season);

		// Remove / ignore all filepath mappings to the season
		if (ignoreFiles)
			episodeMappingsAdapter.ignoreSeason(showId, season);
		else
			episodeMappingsAdapter.removeSeason(showId, season);

		// Remove all episode images
		for (GridEpisode episode : episodesInSeason) {
			episode.getCover().delete();
		}

		// Remove season image
		FileUtils.getTvShowSeason(context, showId, season).delete();

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {
			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}
	}

	public static void deleteEpisode(Context context, String showId, String filepath) {
		// Should filepaths be removed completely or ignored in future library updates?
		boolean ignoreFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.IGNORED_FILES_ENABLED, false);

		// Database adapters
		DbAdapterTvShows showAdapter = NMJManagerApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = NMJManagerApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter();

		// Go through each filepath and remove the database entries
		Cursor cursor = episodeMappingsAdapter.getAllFilepathInfo(filepath);
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					String season = cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_SEASON));
					String episode = cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_EPISODE));

					// Remove / ignore the filepath mapping
					if (ignoreFiles)
						episodeMappingsAdapter.ignoreFilepath(filepath);
					else
						episodeMappingsAdapter.deleteFilepath(filepath);

					// Check if there are any more filepaths mapped to the season / episode
					if (!episodeMappingsAdapter.hasMultipleFilepaths(showId, season, episode)) {

						episodeAdapter.deleteEpisode(showId, NMJLib.getInteger(season), NMJLib.getInteger(episode));

						// Delete the episode photo
						FileUtils.getTvShowEpisode(context, showId, season, episode).delete();

						// Check if the season contains any more mapped filepaths
						if (episodeAdapter.getEpisodesInSeason(context, showId, NMJLib.getInteger(season)).size() == 0) {

							// Remove season image
							FileUtils.getTvShowSeason(context, showId, season).delete();
						}
					}
				}
			} catch (Exception ignored) {} finally {
				cursor.close();
			}
		}

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {

			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}	
	}

	public static void deleteEpisode(Context context, String showId, int season, int episode) {
		// Should filepaths be removed completely or ignored in future library updates?
		boolean ignoreFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.IGNORED_FILES_ENABLED, false);

		// Database adapters
		DbAdapterTvShows showAdapter = NMJManagerApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = NMJManagerApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter();

		ArrayList<String> filepaths = episodeMappingsAdapter.getFilepathsForEpisode(showId, NMJLib.addIndexZero(season), NMJLib.addIndexZero(episode));

		for (int i = 0; i < filepaths.size(); i++) {
			String filepath = filepaths.get(i);
			// Remove / ignore the filepath mapping
			if (ignoreFiles)
				episodeMappingsAdapter.ignoreFilepath(filepath);
			else
				episodeMappingsAdapter.deleteFilepath(filepath);
		}

        // Delete the episode
        episodeAdapter.deleteEpisode(showId, season, episode);

        // Delete the episode photo
        FileUtils.getTvShowEpisode(context, showId, season, episode).delete();

        // Check if the season contains any more mapped filepaths
        if (episodeAdapter.getEpisodesInSeason(context, showId, season).size() == 0) {

            // Remove season image
            FileUtils.getTvShowSeason(context, showId, season).delete();
        }

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {

			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}	
	}
	
	public static void deleteAllUnidentifiedFiles() {
		NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().deleteAllUnidentifiedFilepaths();
	}

    public static void setTvShowsFavourite(final Context context,
                                          final List<TvShow> shows,
                                          boolean favourite) {
        boolean success = true;

        for (TvShow show : shows)
            success = success && NMJManagerApplication.getTvDbAdapter().updateShowSingleItem(show.getId(),
                DbAdapterTvShows.KEY_SHOW_FAVOURITE, favourite ? "1" : "0");

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
                Trakt.tvShowFavorite(shows, context);
            }
        }.start();
    }

    public static void setTvShowsWatched(final Context context,
                                           final List<TvShow> shows,
                                           final boolean watched) {
        boolean success = true;

        for (TvShow show : shows)
            success = success && NMJManagerApplication.getTvEpisodeDbAdapter().setShowWatchStatus(show.getId(), watched);

        if (success)
            if (watched)
                Toast.makeText(context, context.getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                for (TvShow show : shows) {
                    Cursor cursor = NMJManagerApplication.getTvEpisodeDbAdapter().getEpisodes(show.getId());
                    if (cursor != null) {
                        try {
                            List<TvShowEpisode> episodes = new ArrayList<>();
                            while (cursor.moveToNext()) {
                                episodes.add(new TvShowEpisode(show.getId(),
                                        NMJLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))),
                                        NMJLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)))
                                ));
                            }
                            Trakt.markEpisodeAsWatched(show.getId(), episodes, context, watched);
                        } catch (Exception e) {

                        } finally {
                            cursor.close();
                        }
                    }
                }
            }
        }.start();
    }
}
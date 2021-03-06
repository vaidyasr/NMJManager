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
import android.net.Uri;
import android.text.TextUtils;

import com.nmj.functions.Actor;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.Library;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJLib;
import com.nmj.functions.WebMovie;
import com.nmj.nmjmanager.ActorBrowser;
import com.nmj.nmjmanager.ActorDetails;
import com.nmj.nmjmanager.ActorPhotos;
import com.nmj.nmjmanager.ActorTaggedPhotos;
import com.nmj.nmjmanager.ActorVideos;
import com.nmj.nmjmanager.ImageViewer;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.NMJMovieDetails;
import com.nmj.nmjmanager.NMJTvShowDetails;
import com.nmj.nmjmanager.SimilarMovies;
import com.nmj.nmjmanager.TMDbMovieDetails;
import com.nmj.nmjmanager.TvShowDetails;
import com.nmj.nmjmanager.TvShowEpisodes;
import com.nmj.nmjmanager.TvShowSeasons;

import java.util.ArrayList;
import java.util.List;

public class IntentUtils {

	private IntentUtils() {} // No instantiation
	
	/**
	 * Intent for actor details.
	 * @param context
	 * @param name
	 * @param id
	 * @param thumbnail
	 * @return
	 */
	public static Intent getActorIntent(Context context, String name, String id, String personType, String thumbnail, int toolbarColor) {
		Intent actorIntent = new Intent(context, ActorDetails.class);
		actorIntent.putExtra("actorName", name);
		actorIntent.putExtra("actorID", id);
        actorIntent.putExtra("personType", personType);
		actorIntent.putExtra("thumb", thumbnail);
		actorIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);

		return actorIntent;
	}
	
	/**
	 * Intent for actor details. Uses the supplied {@link Actor} object to get details.
	 * @param context
	 * @param actor
	 * @return
	 */
	public static Intent getActorIntent(Context context, Actor actor, int toolbarColor) {
		return getActorIntent(context, actor.getName(), actor.getId(), actor.getPersonType(), actor.getUrl(), toolbarColor);
	}
	
	/**
	 * Intent for the movie cast browser.
	 * @param context
	 * @param title
	 * @param showId
	 * @return
	 */
    public static Intent getActorBrowserMovies(Context context, String title, String showId, int toolbarColor, String loadType) {
        Intent actorIntent = new Intent(context, ActorBrowser.class);
		actorIntent.putExtra("title", title);
		actorIntent.putExtra("movieId", showId);
        actorIntent.putExtra("loadType", loadType);
		actorIntent.putExtra("videoType", "movie");
        actorIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorIntent;
	}

	/**
	 * Intent for the TV show cast browser.
	 * @param context
	 * @param title
	 * @param showId
	 * @return
	 */
    public static Intent getActorBrowserTvShows(Context context, String title, String showId, int toolbarColor, String loadType) {
        Intent actorIntent = new Intent(context, ActorBrowser.class);
		actorIntent.putExtra("title", title);
		actorIntent.putExtra("movieId", showId);
        actorIntent.putExtra("loadType", loadType);
		actorIntent.putExtra("videoType", "tv");
        actorIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorIntent;
	}

	/**
	 * Intent for the similar movies browser.
	 * @param context
	 * @param title
	 * @param movieId
	 * @return
	 */
	public static Intent getSimilarMovies(Context context, String title, String movieId, int toolbarColor) {
		Intent similarMoviesIntent = new Intent(context, SimilarMovies.class);
		similarMoviesIntent.putExtra("title", title);
		similarMoviesIntent.putExtra("movieId", movieId);
		similarMoviesIntent.putExtra("loadType", "similar");
		similarMoviesIntent.putExtra("videoType", "movie");

		similarMoviesIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return similarMoviesIntent;
	}


	public static Intent getRecommendedMovies(Context context, String title, String movieId, int toolbarColor) {
		Intent recommendedMoviesIntent = new Intent(context, SimilarMovies.class);
		recommendedMoviesIntent.putExtra("title", title);
		recommendedMoviesIntent.putExtra("movieId", movieId);
		recommendedMoviesIntent.putExtra("loadType", "recommended");
		recommendedMoviesIntent.putExtra("videoType", "movie");

		recommendedMoviesIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return recommendedMoviesIntent;
	}

	public static Intent getSimilarShows(Context context, String title, String movieId, int toolbarColor) {
		Intent similarShowsIntent = new Intent(context, SimilarMovies.class);
		similarShowsIntent.putExtra("title", title);
		similarShowsIntent.putExtra("movieId", movieId);
		similarShowsIntent.putExtra("loadType", "similar");
		similarShowsIntent.putExtra("videoType", "tv");

		similarShowsIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return similarShowsIntent;
	}

	public static Intent getRecommendedShows(Context context, String title, String movieId, int toolbarColor) {
		Intent recommendedShowsIntent = new Intent(context, SimilarMovies.class);
		recommendedShowsIntent.putExtra("title", title);
		recommendedShowsIntent.putExtra("movieId", movieId);
		recommendedShowsIntent.putExtra("loadType", "recommended");
		recommendedShowsIntent.putExtra("videoType", "tv");

		recommendedShowsIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return recommendedShowsIntent;
	}
	
	public static Intent getTmdbMovieDetails(Context context, WebMovie movie, int toolbarColor) {
		NMJAdapter mDatabase = NMJManagerApplication.getNMJAdapter();
		ArrayList<Library> list = mDatabase.getLibrary();
		System.out.println("Movie is in library? " + movie.isInLibrary());
		Intent movieDetailsIntent = new Intent(context, movie.isInLibrary() ? NMJMovieDetails.class : TMDbMovieDetails.class);
		movieDetailsIntent.putExtra("tmdbId", "tmdb" + movie.getId());
		System.out.println("tmdbId: " + "tmdb" + movie.getId());
		if (movie.isInLibrary())
			movieDetailsIntent.putExtra("showId", mDatabase.getShowIdByTmdbId(movie.getId()));
		System.out.println("showId: " + mDatabase.getShowIdByTmdbId(movie.getId()));
		movieDetailsIntent.putExtra("title", movie.getTitle());
		movieDetailsIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);

		return movieDetailsIntent;
	}

	public static Intent getTmdbShowDetails(Context context, WebMovie show, int toolbarColor) {
		NMJAdapter mDatabase = NMJManagerApplication.getNMJAdapter();
		ArrayList<Library> list = mDatabase.getLibrary();
		System.out.println("Movie is in library? " + show.isInLibrary());
		Intent movieDetailsIntent = new Intent(context, NMJTvShowDetails.class);
		movieDetailsIntent.putExtra("showId", "0");
		movieDetailsIntent.putExtra("movieId", "tmdb" + show.getId());
		if (show.isInLibrary())
			movieDetailsIntent.putExtra("showId", mDatabase.getShowIdByTmdbId(show.getId()));
		System.out.println("showId: " + mDatabase.getShowIdByTmdbId(show.getId()));
		movieDetailsIntent.putExtra("title", show.getTitle());
		movieDetailsIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);

		return movieDetailsIntent;
	}
	
	public static Intent getActorPhotoIntent(Context context, List<String> photos, int selectedIndex, int toolbarColor) {
		String[] array = new String[photos.size()];
		for (int i = 0; i < photos.size(); i++)
			array[i] = photos.get(i).replace(NMJLib.getActorUrlSize(context), "original");
		
		Intent intent = new Intent(context, ImageViewer.class);
		intent.putExtra("photos", array);
		intent.putExtra("selectedIndex", selectedIndex);
		intent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);

		return intent;
	}
	
	public static Intent getActorTaggedPhotoIntent(Context context, List<String> photos, int selectedIndex, int toolbarColor) {
		String[] array = new String[photos.size()];
		for (int i = 0; i < photos.size(); i++)
			array[i] = photos.get(i).replace(NMJLib.getBackdropThumbUrlSize(context), "original");
		
		Intent intent = new Intent(context, ImageViewer.class);
		intent.putExtra("photos", array);
		intent.putExtra("portraitPhotos", false);
		intent.putExtra("selectedIndex", selectedIndex);
		intent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);

		return intent;
	}
	
	public static Intent getActorMoviesIntent(Context context, String actorName, String actorId, String personType, int toolbarColor) {
		Intent actorMoviesIntent = new Intent(context, ActorVideos.class);
		actorMoviesIntent.putExtra("actorName", actorName);
		actorMoviesIntent.putExtra("actorId", actorId);
		actorMoviesIntent.putExtra("personType", personType);
		actorMoviesIntent.putExtra("videoType", "movie");

		actorMoviesIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorMoviesIntent;
	}
	
	public static Intent getActorTvShowsIntent(Context context, String actorName, String actorId, String personType, int toolbarColor) {
		Intent actorTvShowIntent = new Intent(context, ActorVideos.class);
		actorTvShowIntent.putExtra("actorName", actorName);
		actorTvShowIntent.putExtra("actorId", actorId);
		actorTvShowIntent.putExtra("personType", personType);
		actorTvShowIntent.putExtra("videoType", "tv");

		actorTvShowIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorTvShowIntent;
	}
	
	public static Intent getActorPhotosIntent(Context context, String actorName, String actorId, int toolbarColor) {
		Intent actorPhotosIntent = new Intent(context, ActorPhotos.class);
		actorPhotosIntent.putExtra("actorName", actorName);
		actorPhotosIntent.putExtra("actorId", actorId);
        actorPhotosIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorPhotosIntent;
	}
	
	public static Intent getActorTaggedPhotosIntent(Context context, String actorName, String actorId, int toolbarColor) {
		Intent actorTaggedPhotosIntent = new Intent(context, ActorTaggedPhotos.class);
		actorTaggedPhotosIntent.putExtra("actorName", actorName);
		actorTaggedPhotosIntent.putExtra("actorId", actorId);
        actorTaggedPhotosIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
		return actorTaggedPhotosIntent;
	}

    public static Intent getTvShowSeasonsIntent(Context context, String title, String showId, int toolbarColor) {
        Intent seasonsIntent = new Intent(context, TvShowSeasons.class);
        seasonsIntent.putExtra("showTitle", title);
        seasonsIntent.putExtra("showId", showId);
        seasonsIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
        return seasonsIntent;
    }

    public static Intent getTvShowSeasonIntent(Context context, String showId, int season, int episodeCount, int toolbarColor) {
        Intent seasonIntent = new Intent(context, TvShowEpisodes.class);
        seasonIntent.putExtra("showId", showId);
        seasonIntent.putExtra("season", season);
        seasonIntent.putExtra("episodeCount", episodeCount);
        seasonIntent.putExtra(IntentKeys.TOOLBAR_COLOR, toolbarColor);
        return seasonIntent;
    }
}
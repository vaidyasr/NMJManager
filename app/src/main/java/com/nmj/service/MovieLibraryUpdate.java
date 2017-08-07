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

package com.nmj.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.nmj.abstractclasses.MovieFileSource;
import com.nmj.apis.trakt.Trakt;
import com.nmj.db.DbAdapterSources;
import com.nmj.filesources.FileMovie;
import com.nmj.filesources.SmbMovie;
import com.nmj.filesources.UpnpMovie;
import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;
import com.nmj.functions.MovieLibraryUpdateCallback;
import com.nmj.identification.MovieIdentification;
import com.nmj.identification.MovieStructure;
import com.nmj.nmjmanager.CancelLibraryUpdate;
import com.nmj.nmjmanager.Main;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.utils.LocalBroadcastUtils;
import com.nmj.utils.MovieDatabaseUtils;
import com.nmj.utils.WidgetUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.nmj.functions.PreferenceKeys.CLEAR_LIBRARY_MOVIES;
import static com.nmj.functions.PreferenceKeys.DISABLE_ETHERNET_WIFI_CHECK;
import static com.nmj.functions.PreferenceKeys.ENABLE_SUBFOLDER_SEARCH;
import static com.nmj.functions.PreferenceKeys.IGNORED_FILES_ENABLED;
import static com.nmj.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.nmj.functions.PreferenceKeys.REMOVE_UNAVAILABLE_FILES_MOVIES;
import static com.nmj.functions.PreferenceKeys.SYNC_WITH_TRAKT;

public class MovieLibraryUpdate extends IntentService implements MovieLibraryUpdateCallback {

	public static final String STOP_MOVIE_LIBRARY_UPDATE = "NMJManager-stop-movie-library-update";
	private boolean isDebugging = true;
	private ArrayList<FileSource> mFileSources;
	private ArrayList<MovieFileSource<?>> mMovieFileSources;
	private HashMap<String, InputStream> mNfoFiles = new HashMap<String, InputStream>();
	private ArrayList<MovieStructure> mMovieQueue = new ArrayList<MovieStructure>();
	private boolean mIgnoreRemovedFiles, mClearLibrary, mSearchSubfolders, mClearUnavailable, mIgnoreNfoFiles, mDisableEthernetWiFiCheck, mSyncLibraries, mStopUpdate;
	private int mTotalFiles, mCount;
	private SharedPreferences mSettings;
	private Editor mEditor;
	private final int NOTIFICATION_ID = 200, POST_UPDATE_NOTIFICATION = 213;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;
	private MovieIdentification mMovieIdentification;

	public MovieLibraryUpdate() {
		super("MovieLibraryUpdate");
	}

	public MovieLibraryUpdate(String name) {
		super(name);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		log("onDestroy()");

		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);

		reloadLibrary();
		
		showPostUpdateNotification();

		WidgetUtils.updateMovieWidgets(this);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

		NMJLib.scheduleMovieUpdate(this);

		if (Trakt.hasTraktAccount(this) && mSyncLibraries && mCount > 0) {
			getApplicationContext().startService(new Intent(getApplicationContext(), TraktMoviesSyncService.class));
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		log("clear()");

		// Clear and set up all variables
		clear();

		log("setup()");

		// Set up Notification, variables, etc.
		setup();

		log("loadFileSources()");

		// Load all file sources from the database
		loadFileSources();

		log("setupMovieFileSources()");

		// Add the different file sources to the MovieFileSource ArrayList
		setupMovieFileSources(mIgnoreRemovedFiles, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck);

		if (mStopUpdate)
			return;

		log("removeUnidentifiedFiles()");

		// Remove unavailable movies, so we can try to identify them again
		if (!mClearLibrary)
			removeUnidentifiedFiles();

		if (mStopUpdate)
			return;

		// Check if the library should be cleared
		if (mClearLibrary) {

			// Reset the preference, so it isn't checked the next
			// time the user wants to update the library
			mEditor = mSettings.edit();
			mEditor.putBoolean(CLEAR_LIBRARY_MOVIES, false);
			mEditor.apply();

			log("removeMoviesFromDatabase()");

			// Remove all entries from the database
			removeMoviesFromDatabase();
		}

		if (mStopUpdate)
			return;

		// Check if we should remove all unavailable files.
		// Note that this only makes sense if we haven't already cleared the library.
		if (!mClearLibrary && mClearUnavailable) {

			log("removeUnavailableFiles()");

			// Remove all unavailable files from the database
			removeUnavailableFiles();
		}

		log("searchFolders()");

		if (mStopUpdate)
			return;
		
		reloadLibrary();

		// Search all folders
		searchFolders();

		if (mStopUpdate)
			return;
		log("mTotalFiles > 0 check");

		// Check if we've found any files to identify
		if (mTotalFiles > 0) {
			log("updateMovies()");

			// Start the actual movie update / identification task
			updateMovies();
		}
	}
	
	private void reloadLibrary() {
		log("reloadLibrary()");
		
		LocalBroadcastUtils.updateMovieLibrary(getApplicationContext());
	}

	private void loadFileSources() {
		mFileSources = new ArrayList<FileSource>();
		DbAdapterSources dbHelperSources = NMJManagerApplication.getSourcesAdapter();
		Cursor c = dbHelperSources.fetchAllMovieSources();
		try {
			while (c.moveToNext()) {
				mFileSources.add(new FileSource(
						c.getLong(c.getColumnIndex(DbAdapterSources.KEY_ROWID)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
						c.getInt(c.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_USER)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_TYPE))
						));
			}
		} catch (Exception e) {
		} finally {
			c.close();
		}
	}

	private void setupMovieFileSources(boolean mIgnoreRemovedFiles, boolean mSearchSubfolders, boolean mClearLibrary, boolean mDisableEthernetWiFiCheck) {
		for (FileSource fileSource : mFileSources) {
			if (mStopUpdate)
				return;
			switch (fileSource.getFileSourceType()) {
			case FileSource.FILE:
				mMovieFileSources.add(new FileMovie(getApplicationContext(), fileSource, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			case FileSource.SMB:
				mMovieFileSources.add(new SmbMovie(getApplicationContext(), fileSource, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			case FileSource.UPNP:
				mMovieFileSources.add(new UpnpMovie(getApplicationContext(), fileSource, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			}
		}
	}

	private void removeUnidentifiedFiles() {
		for (MovieFileSource<?> movieFileSource : mMovieFileSources) {
			movieFileSource.removeUnidentifiedFiles();
		}
	}

	private void removeMoviesFromDatabase() {
		MovieDatabaseUtils.deleteAllMovies(this);
	}

	private void removeUnavailableFiles() {
		for (MovieFileSource<?> movieFileSource : mMovieFileSources) {
			movieFileSource.removeUnavailableFiles();
		}
	}

	private void searchFolders() {
		// Temporary collections
		List<String> tempList = null;
		HashMap<String, InputStream> tempNfoMap = null;

		for (int j = 0; j < mMovieFileSources.size(); j++) {
			updateMovieScaningNotification(mMovieFileSources.get(j).toString());
			tempList = mMovieFileSources.get(j).searchFolder();
			for (int i = 0; i < tempList.size(); i++) {
				mMovieQueue.add(new MovieStructure(tempList.get(i)));
			}

			if (!mIgnoreNfoFiles) {
				tempNfoMap = mMovieFileSources.get(j).getNfoFiles();
				mNfoFiles.putAll(tempNfoMap);
			}
		}

		// Clean up...
		if (tempList != null)
			tempList.clear();

		if (tempNfoMap != null)
			tempNfoMap.clear();

		mTotalFiles = mMovieQueue.size();
	}

	private void setup() {
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter(STOP_MOVIE_LIBRARY_UPDATE));

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelLibraryUpdate.class);
		notificationIntent.putExtra("isMovie", true);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
		mBuilder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		mBuilder.setTicker(getString(R.string.updatingMovies));
		mBuilder.setContentTitle(getString(R.string.updatingMovies));
		mBuilder.setContentText(getString(R.string.gettingReady));
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.addAction(R.drawable.ic_close_white_24dp, getString(android.R.string.cancel), contentIntent);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);

		mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mClearLibrary = mSettings.getBoolean(CLEAR_LIBRARY_MOVIES, false);
		mSearchSubfolders = mSettings.getBoolean(ENABLE_SUBFOLDER_SEARCH, true);
		mClearUnavailable = mSettings.getBoolean(REMOVE_UNAVAILABLE_FILES_MOVIES, false);
		mIgnoreNfoFiles = mSettings.getBoolean(IGNORED_NFO_FILES, true);
		mDisableEthernetWiFiCheck = mSettings.getBoolean(DISABLE_ETHERNET_WIFI_CHECK, false);
		mIgnoreRemovedFiles = mSettings.getBoolean(IGNORED_FILES_ENABLED, false);
		mSyncLibraries = mSettings.getBoolean(SYNC_WITH_TRAKT, true);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStopUpdate = true;
			
			if (mMovieIdentification != null)
				mMovieIdentification.cancel();
		}
	};

	private void updateMovies() {
		mMovieIdentification = new MovieIdentification(getApplicationContext(), this, mMovieQueue, mNfoFiles);
		mMovieIdentification.start();
	}

	private void clear() {
		// Lists
		mFileSources = new ArrayList<FileSource>();
		mMovieFileSources = new ArrayList<MovieFileSource<?>>();
		mMovieQueue = new ArrayList<MovieStructure>();
		mNfoFiles = new HashMap<String, InputStream>();

		// Booleans
		mIgnoreRemovedFiles = false;
		mClearLibrary = false;
		mSearchSubfolders = true;
		mClearUnavailable = false;
		mIgnoreNfoFiles = true;
		mDisableEthernetWiFiCheck = false;
		mSyncLibraries = true;
		mStopUpdate = false;

		// Other variables
		mEditor = null;
		mSettings = null;
		mTotalFiles = 0;
		mNotificationManager = null;
		mBuilder = null;
	}

	private void log(String msg) {
		if (isDebugging)
			Log.d("MovieLibraryUpdate", msg);
	}

	@Override
	public void onMovieAdded(String title, Bitmap cover, Bitmap backdrop, int count) {
		mCount = count;
		updateMovieAddedNotification(title, cover, backdrop);
	}

	private void updateMovieAddedNotification(String title, Bitmap cover, Bitmap backdrop) {
		mBuilder.setLargeIcon(cover);
		mBuilder.setContentTitle(getString(R.string.updatingMovies) + " (" + (int) ((100.0 / (double) mTotalFiles) * (double) mCount) + "%)");
		mBuilder.setContentText(getString(R.string.stringJustAdded) + ": " + title);
		mBuilder.setStyle(
				new NotificationCompat.BigPictureStyle()
				.setSummaryText(getString(R.string.stringJustAdded) + ": " + title)
				.bigPicture(backdrop)
				);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void updateMovieScaningNotification(String filesource) {
		mBuilder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		mBuilder.setContentTitle(getString(R.string.updatingMovies));
		mBuilder.setContentText(getString(R.string.scanning) + ": " + filesource);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void showPostUpdateNotification() {
		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, Main.class);
		notificationIntent.putExtra("fromUpdate", true);
		notificationIntent.putExtra("startup", "1");
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		if (!mStopUpdate) {
			mBuilder.setSmallIcon(R.drawable.ic_done_white_24dp);
			mBuilder.setTicker(getString(R.string.finishedMovieLibraryUpdate));
			mBuilder.setContentTitle(getString(R.string.finishedMovieLibraryUpdate));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + mCount + " " + getResources().getQuantityString(R.plurals.moviesInLibrary, mCount));
		} else {
			mBuilder.setSmallIcon(R.drawable.ic_cancel_white_24dp);
			mBuilder.setTicker(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentTitle(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + mCount + " " + getResources().getQuantityString(R.plurals.moviesInLibrary, mCount, mCount));
		}
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if (mCount > 0)
			mNotificationManager.notify(POST_UPDATE_NOTIFICATION, updateNotification);
	}
}
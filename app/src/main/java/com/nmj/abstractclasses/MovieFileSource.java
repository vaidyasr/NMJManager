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

package com.nmj.abstractclasses;

import android.content.Context;
import android.database.Cursor;

import com.nmj.db.DbAdapterMovieMappings;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.functions.DbMovie;
import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.NMJManagerApplication;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class MovieFileSource<T> extends AbstractFileSource<T> {

	protected HashMap<String, InputStream> mNfoFiles = new HashMap<String, InputStream>();
	protected List<DbMovie> mDbMovies = new ArrayList<DbMovie>();

	public MovieFileSource(Context context, FileSource fileSource, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		mContext = context;
		mFileSource = fileSource;
		mSubFolderSearch = subFolderSearch;
		mClearLibrary = clearLibrary;

		mFileSizeLimit = NMJLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbMovies() {
		mDbMovies.clear();
		
		// Fetch all the movies from the database
		DbAdapterMovies db = NMJManagerApplication.getMovieAdapter();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor tempCursor = db.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
		try {
			while (tempCursor.moveToNext()) {
				mDbMovies.add(new DbMovie(getContext(),
						NMJManagerApplication.getMovieMappingAdapter().getFirstFilepathForMovie(tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovieMappings.KEY_TMDB_ID))),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_TMDB_ID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_RUNTIME)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_RELEASEDATE)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_GENRES)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_TITLE))));
			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
			cache.clear();
		}
	}
	
	public List<DbMovie> getDbMovies() {
		if (mDbMovies.size() == 0)
			setupDbMovies();
		return mDbMovies;
	}

	public void addNfoFile(String filepath, InputStream is) {
		mNfoFiles.put(filepath, is);
	}

	public HashMap<String, InputStream> getNfoFiles() {
		return mNfoFiles;
	}

	/**
	 * Determine if this file source supports loading of .NFO files. Should be overridden if the file source doesn't support .NFO files.
	 * @return The value indicates if the file source supports loading of .NFO files or not.
	 */
	public boolean supportsNfo() {
		return true;
	}
}
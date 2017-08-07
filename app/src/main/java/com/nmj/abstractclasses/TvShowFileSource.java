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

import com.nmj.db.DbAdapterTvShowEpisodes;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.functions.DbEpisode;
import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.NMJManagerApplication;

import java.util.ArrayList;
import java.util.List;

public abstract class TvShowFileSource<T> extends AbstractFileSource<T> {

	protected List<DbEpisode> mDbEpisode = new ArrayList<DbEpisode>();

	public TvShowFileSource(Context context, FileSource fileSource, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		mContext = context;
		mFileSource = fileSource;
		mSubFolderSearch = subFolderSearch;
		mClearLibrary = clearLibrary;

		mFileSizeLimit = NMJLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbEpisodes() {
		mDbEpisode.clear();

		DbAdapterTvShowEpisodes db = NMJManagerApplication.getTvEpisodeDbAdapter();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor tempCursor = db.getAllEpisodes();
		try {
			while (tempCursor.moveToNext()) {
				mDbEpisode.add(new DbEpisode(getContext(),
						NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().getFirstFilepath(tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SHOW_ID)), tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SEASON)), tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_EPISODE))),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SEASON)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_EPISODE))
						)
				);

			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
			cache.clear();
		}
	}

	public List<DbEpisode> getDbEpisodes() {
		if (mDbEpisode.size() == 0)
			setupDbEpisodes();
		return mDbEpisode;
	}
}
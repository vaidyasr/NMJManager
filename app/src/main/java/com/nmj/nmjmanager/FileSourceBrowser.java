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

package com.nmj.nmjmanager;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.widget.Toast;

import com.nmj.base.NMJActivity;
import com.nmj.functions.FileSource;
import com.nmj.nmjmanager.fragments.FileSourceBrowserFragment;

import static com.nmj.functions.NMJLib.DOMAIN;
import static com.nmj.functions.NMJLib.FILESOURCE;
import static com.nmj.functions.NMJLib.MOVIE;
import static com.nmj.functions.NMJLib.PASSWORD;
import static com.nmj.functions.NMJLib.SERIAL_NUMBER;
import static com.nmj.functions.NMJLib.SERVER;
import static com.nmj.functions.NMJLib.TYPE;
import static com.nmj.functions.NMJLib.USER;
import static com.nmj.functions.PreferenceKeys.HAS_SHOWN_FILEBROWSER_MESSAGE;

public class FileSourceBrowser extends NMJActivity {

	private static String TAG = "";
	private boolean isMovie = true;
	private int mFilesource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		isMovie = extras.getString(TYPE).equals(MOVIE);
		mFilesource = extras.getInt(FILESOURCE, FileSource.FILE);
		
		TAG = "source" + mFilesource;

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
			switch (mFilesource) {
			case FileSource.FILE:
				ft.replace(R.id.content, FileSourceBrowserFragment.newInstanceFile(isMovie), TAG);
				break;
			case FileSource.SMB:
				ft.replace(R.id.content, FileSourceBrowserFragment.newInstanceSmbFile(extras.getString(SERVER), extras.getString(USER), extras.getString(PASSWORD), extras.getString(DOMAIN), isMovie), TAG);
				break;
			case FileSource.UPNP:
				ft.replace(R.id.content, FileSourceBrowserFragment.newInstanceUpnp(extras.getString(SERVER), extras.getString(SERIAL_NUMBER), isMovie), TAG);
				break;
			}
			ft.commit();
		}
		
		boolean hasShown = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HAS_SHOWN_FILEBROWSER_MESSAGE, false);
		if (!hasShown) {
			Toast.makeText(this, R.string.browser_help_message, Toast.LENGTH_LONG).show();
			
			Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			editor.putBoolean(HAS_SHOWN_FILEBROWSER_MESSAGE, true);
			editor.apply();
		}
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onBackPressed() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("onBackPressed"));
	}
	
	@Override
	protected int getLayoutResource() {
		return R.layout.empty_layout_with_toolbar;
	}
}
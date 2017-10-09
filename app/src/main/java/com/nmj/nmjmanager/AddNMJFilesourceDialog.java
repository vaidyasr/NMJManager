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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.nmj.db.DbAdapterSources;
import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import static com.nmj.functions.NMJLib.DOMAIN;
import static com.nmj.functions.NMJLib.FILESOURCE;
import static com.nmj.functions.NMJLib.MOVIE;
import static com.nmj.functions.NMJLib.PASSWORD;
import static com.nmj.functions.NMJLib.SERVER;
import static com.nmj.functions.NMJLib.TV_SHOW;
import static com.nmj.functions.NMJLib.TYPE;
import static com.nmj.functions.NMJLib.USER;

public class AddNMJFilesourceDialog extends Activity {

	private EditText server, display_name, port;
	private String mDomain, mUser, mPass, mServer;
	private boolean isMovie = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.nmj_input);

		server = (EditText) findViewById(R.id.ip_address);
		display_name = (EditText) findViewById(R.id.display_name);

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("NMJManager-network-search"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String ip = intent.getExtras().getString("ip");
			server.setText(ip);
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@SuppressLint("UseSparseArrays")
	public void search(View v) {
		final ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = NMJManagerApplication.getSourcesAdapter();
		TreeSet<String> uniqueSources = new TreeSet<String>();

		int count = sources.size();
		for (int i = 0; i < count; i++) {
			String temp = sources.get(i).getFilepath().replace("smb://", "");
			temp = temp.substring(0, temp.indexOf("/"));
			uniqueSources.add(temp);
		}

		final CharSequence[] items = new CharSequence[uniqueSources.size() + 1];

		count = 0;
		Iterator<String> it = uniqueSources.iterator();
		while (it.hasNext()) {
			items[count] = it.next();
			count++;
		}

		items[items.length - 1] = getString(R.string.scanForSources);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.browseSources));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == (items.length - 1)) {
					Intent intent = new Intent();
					intent.setClass(getApplicationContext(), SearchForNetworkShares.class);
					startActivity(intent);
				} else {
					showUserDialog(items, which);
				}
			}});
		builder.show();
	}

	private void showUserDialog(final CharSequence[] items, final int which) {

		final ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = NMJManagerApplication.getSourcesAdapter();

		// Fetch all movie sources and add them to the array
		Cursor cursor = dbHelper.fetchAllSources();
		while (cursor.moveToNext()) {
			sources.add(new FileSource(
					cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
					cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
					));
		}

		cursor.close();

		HashMap<String, String> userPass = new HashMap<String, String>();

		int count = sources.size();
		for (int i = 0; i < count; i++) {
			String temp = sources.get(i).getFilepath().replace("smb://", "");
			temp = temp.substring(0, temp.indexOf("/"));

			if (temp.equals(items[which])) {
				userPass.put((sources.get(i).getUser().isEmpty() ? getString(R.string.anonymous) : sources.get(i).getUser()), sources.get(i).getPassword());
			}
		}

		if (userPass.size() == 1) {	
			String userTemp = userPass.keySet().iterator().next();
			userPass.get(userTemp);

			server.setText(items[which]);
		} else {

			final CharSequence[] usernames = new CharSequence[userPass.size()];
			final CharSequence[] passwords = new CharSequence[userPass.size()];
			int i = 0;
			Iterator<String> it = userPass.keySet().iterator();
			while (it.hasNext()) {
				String s = it.next();
				usernames[i] = s;
				passwords[i] = userPass.get(s);
				i++;
			}

			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle(getString(R.string.selectLogin));
			builder2.setItems(usernames, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int choice) {
					server.setText(items[which]);
				}
			});
			builder2.show();	
		}
	}

	public void cancel(View v) {
		finish();
	}

	public void ok(View v) {	
		if (server.getText().toString().isEmpty()) {
			Toast.makeText(AddNMJFilesourceDialog.this, getString(R.string.enterNetworkAddress), Toast.LENGTH_LONG).show();
			return;
		}

		if (NMJLib.isWifiConnected(this)) {
			mDomain = display_name.getText().toString().trim();
			mServer = server.getText().toString().trim();
			attemptLogin();
		} else
			Toast.makeText(AddNMJFilesourceDialog.this, getString(R.string.noConnection), Toast.LENGTH_LONG).show();
	}

	private void attemptLogin() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), FileSourceBrowser.class);
		intent.putExtra(USER, mUser);
		intent.putExtra(PASSWORD, mPass);
		intent.putExtra(DOMAIN, mDomain);
		intent.putExtra(SERVER, mServer);
		intent.putExtra(TYPE, isMovie ? MOVIE : TV_SHOW);
		intent.putExtra(FILESOURCE, FileSource.SMB);
		startActivity(intent);
		finish();
	}
}
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nmj.functions.AsyncTask;
import com.nmj.functions.MenuItem;
import com.nmj.functions.NMJDb;
import com.nmj.functions.NMJLib;
import com.nmj.utils.LocalBroadcastUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class AddNMJDbDialog extends Activity {

	private EditText server, domain, username, password;
	private CheckBox anonymous, guest;
	private String mDomain, mUser, mPass, mServer;
	private boolean isMovie = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.nmjdb_select);

		RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.segmented_layout);
		ListView listView = (ListView) findViewById(R.id.nmjlist);
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.segmented_group);

		if (NMJLib.getMachineType().equals(""))
			relativeLayout.setVisibility(View.GONE);

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
}
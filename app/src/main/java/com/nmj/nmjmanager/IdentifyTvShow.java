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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.nmjmanager.fragments.IdentifyTvShowFragment;
import com.nmj.utils.ViewUtils;

public class IdentifyTvShow extends NMJActivity {

	private static String TAG = "IdentifyTvShowFragment";
    private int mToolbarColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String showId = getIntent().getExtras().getString("showId");
		String showTitle = getIntent().getExtras().getString("showTitle");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, IdentifyTvShowFragment.newInstance(showTitle, showId), TAG);
			ft.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
	}
	
	@Override
	protected int getLayoutResource() {
		return 0;
	}
}
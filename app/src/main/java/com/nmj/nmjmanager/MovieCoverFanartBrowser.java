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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.fragments.CollectionCoverSearchFragment;
import com.nmj.nmjmanager.fragments.CoverSearchFragment;
import com.nmj.nmjmanager.fragments.FanartSearchFragment;
import com.nmj.utils.ViewUtils;

import org.json.JSONObject;

public class MovieCoverFanartBrowser extends NMJActivity  {

	private String mTmdbId, mShowId, mCollectionId, mBaseUrl = "", mJson = "", mCollection = "", mTmdbApiKey, mTmdbApiURL;
	private ViewPager mViewPager;
	private ProgressBar mProgressBar;
    private PagerSlidingTabStrip mTabs;
    private int mToolbarColor;

	@Override
	protected int getLayoutResource() {
		return R.layout.viewpager_with_toolbar_and_tabs;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		NMJManagerApplication.setupTheme(this);

        if (NMJLib.hasLollipop())
            getSupportActionBar().setElevation(0);

        mTmdbId = getIntent().getExtras().getString("tmdbId");
		mShowId = getIntent().getExtras().getString("showId");
		mCollectionId = getIntent().getExtras().getString("collectionId");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);
		mTmdbApiKey = NMJLib.getTmdbApiKey(this);
		mTmdbApiURL = NMJLib.getTmdbApiURL(this);

        mProgressBar = findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        mViewPager = findViewById(R.id.awesomepager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPageMargin(NMJLib.convertDpToPixels(this, 16));

        mTabs = findViewById(R.id.tabs);
        mTabs.setVisibility(View.GONE);

		if (savedInstanceState != null) {
            mJson = savedInstanceState.getString("json", "");
            mBaseUrl = savedInstanceState.getString("baseUrl");
            mCollection = savedInstanceState.getString("collection");
			setupActionBarStuff();

            mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new MovieLoader(getApplicationContext()).execute(mTmdbId, mCollectionId);
		}
	}

    @Override
    public void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
        mTabs.setBackgroundColor(mToolbarColor);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
		outState.putString("json", mJson);
		outState.putString("baseUrl", mBaseUrl);
		outState.putString("collection", mCollection);
	}

	private void setupActionBarStuff() {
		if (!NMJLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(0);

		mProgressBar.setVisibility(View.GONE);
		mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
		mTabs.setViewPager(mViewPager);
		mTabs.setVisibility(View.VISIBLE);
	}

	private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.coverart), getString(R.string.backdrop), getString(R.string.collectionart)};

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

		@Override
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return CoverSearchFragment.newInstance(mTmdbId, mShowId, mJson, mBaseUrl);
			case 1:
				return FanartSearchFragment.newInstance(mTmdbId, mShowId, mJson, mBaseUrl);
			default:
				return CollectionCoverSearchFragment.newInstance(mCollectionId, mCollection, mBaseUrl);
			}
		}

		@Override
		public int getCount() {
			if (!NMJLib.isValidTmdbId(mCollectionId))
				return 2;
			return 3;
		}
	}

	private class MovieLoader extends AsyncTask<Object, Object, String> {

		private Context mContext;

		public MovieLoader(Context context) {
			mContext = context;
		}

		@Override
		protected String doInBackground(Object... params) {
			String URL;
            String tmdbid = params[0].toString().replace("tmdb", "");
            String CacheId = "movie_" + tmdbid;
            try {
                URL = NMJLib.getTmdbApiURL(mContext) + "movie/" + tmdbid + "?api_key=" +
                        NMJLib.getTmdbApiKey(mContext) + "&language=en";
                URL += "&append_to_response=recommendations,releases,trailers,credits,images,similar&include_image_language=en,null";

                if (NMJLib.getTMDbCache(CacheId).equals("")) {
                    System.out.println("Putting Cache in " + CacheId);
                    NMJLib.setTMDbCache(CacheId, NMJLib.getJSONObject(mContext, URL).toString());
                }
                System.out.println("Getting Cache from " + CacheId);
                mJson = new JSONObject(NMJLib.getTMDbCache(CacheId)).getJSONObject("images").toString();
                System.out.println("JSON: " + mJson);

				//JSONArray jArray = jObject.getJSONObject("images").getJSONArray("cast");

				//mBaseUrl = NMJLib.getTmdbImageBaseUrl(mContext);

                //mJson = NMJLib.getJSONObject(mContext, mTmdbApiURL + "movie/" + params[0] + "/images?api_key=" + mTmdbApiKey).toString();

/*				if (NMJLib.isValidTmdbId(mCollectionId)) {
                    mCollection = NMJLib.getJSONObject(mContext, mTmdbApiURL + "collection/" + params[1] + "/images?api_key=" + mTmdbApiKey).toString();
				}*/

				return mJson;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				setupActionBarStuff();
			} else {
				Toast.makeText(getApplicationContext(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
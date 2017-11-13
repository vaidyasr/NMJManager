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
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.fragments.CoverSearchFragmentTv;
import com.nmj.nmjmanager.fragments.FanartSearchFragmentTv;
import com.nmj.utils.ViewUtils;

public class ShowCoverFanartBrowser extends NMJActivity  {

	private String tvdbId;
    private ViewPager mViewPager;
    private ProgressBar mProgressBar;
    private TabLayout mTabs;
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		tvdbId = getIntent().getExtras().getString("id");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

        mProgressBar = findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        mViewPager = findViewById(R.id.awesomepager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPageMargin(NMJLib.convertDpToPixels(this, 16));

        mTabs = findViewById(R.id.tabs);
        mTabs.setVisibility(View.GONE);

        setupActionBarStuff();

		if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
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
    }

    private void setupActionBarStuff() {
        if (!NMJLib.isPortrait(getApplicationContext()))
            findViewById(R.id.layout).setBackgroundResource(0);

        mProgressBar.setVisibility(View.GONE);
        mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        mTabs.setupWithViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);
    }

	private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.coverart), getString(R.string.backdrop)};

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
				return CoverSearchFragmentTv.newInstance(tvdbId);
			default:
				return FanartSearchFragmentTv.newInstance(tvdbId);
			}
		}  

		@Override  
		public int getCount() {  
			return 2;  
		}
	}
}
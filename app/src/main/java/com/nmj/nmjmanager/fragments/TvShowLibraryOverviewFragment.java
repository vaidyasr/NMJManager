package com.nmj.nmjmanager.fragments;/*
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

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.nmj.functions.NMJLib;
import com.nmj.loader.TvShowLoader;
import com.nmj.nmjmanager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nmj.functions.PreferenceKeys.MOVIES_TABS_SELECTED;
import static com.nmj.functions.PreferenceKeys.SHOWS_TAB_SELECTED;

public class TvShowLibraryOverviewFragment extends Fragment {

    List<String> TITLES = new ArrayList<>();
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

    public TvShowLibraryOverviewFragment() {} // Empty constructor

    public static TvShowLibraryOverviewFragment newInstance() {
        return new TvShowLibraryOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);
        PagerAdapter mAdapter;

        final String[] TITLES1 = {getString(R.string.choiceAllShows), getString(R.string.choiceFavorites), getString(R.string.recently_aired),
                getString(R.string.watched_tv_shows), getString(R.string.unwatched_tv_shows), getString(R.string.popular_shows),
                getString(R.string.toprated_shows), getString(R.string.ontv), getString(R.string.airing_today)};
        for (int i = 0; i < TITLES1.length; i++)
            TITLES.add(i, TITLES1[i]);

        if (NMJLib.hasLollipop())
            ((ActionBarActivity) getActivity()).getSupportActionBar().setElevation(0);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setPageMargin(NMJLib.convertDpToPixels(getActivity(), 16));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);

        mViewPager.setAdapter(mAdapter = new PagerAdapter(getChildFragmentManager()));

        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);

        // Work-around a bug that sometimes happens with the tabs
        mViewPager.setCurrentItem(0);

        mAdapter.addTab(TITLES.get(0));

        Set<String> defValues = new HashSet<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8"));
        Set<String> values = PreferenceManager.getDefaultSharedPreferences(getActivity()).getStringSet(SHOWS_TAB_SELECTED, defValues);
        List<String> list = new ArrayList<String>(values);
        Collections.sort(list, new Comparator<String>()
        {
            @Override
            public int compare(String s1, String s2)
            {
                Integer val1 = Integer.parseInt(s1);
                Integer val2 = Integer.parseInt(s2);
                return val1.compareTo(val2);
            }
        });

        for (int i=0; i<list.size();i++){
            mAdapter.addTab(TITLES.get(Integer.parseInt(list.get(i))));
        }
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());

        if (NMJLib.hasLollipop())
            mTabs.setElevation(1f);

        return v;
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private ArrayList<String> tabs = new ArrayList<>();
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addTab(String tab) {
            tabs.add(tab);
            notifyDataSetChanged();
            mTabs.notifyDataSetChanged();
        }

        public void removeTab(int position) {
            tabs.remove(position);
            notifyDataSetChanged();
            mTabs.notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs.get(position);
        }

        @Override
        public Fragment getItem(int index) {
            switch (TITLES.indexOf(getPageTitle(index))) {
                case 0:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.ALL_SHOWS);
                case 1:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.FAVORITES);
                case 2:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.RECENTLY_AIRED);
                case 3:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.WATCHED);
                case 4:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.UNWATCHED);
                case 5:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.POPULAR);
                case 6:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.TOP_RATED);
                case 7:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.ON_TV);
                case 8:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.AIRING_TODAY);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return tabs.size();
        }
    }
}

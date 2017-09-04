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

import android.content.SharedPreferences;
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
import com.nmj.loader.MovieLoader;
import com.nmj.nmjmanager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_COLLECTIONS;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_FAVOURITES;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_LIST;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_NEW_RELEASES;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_NOW_RUNNING;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_POPULAR;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_TOP_RATED;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_UNWATCHED;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_UPCOMING;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_WATCHED;
import static com.nmj.functions.PreferenceKeys.SHOW_MOVIE_WATCHLIST;

public class MovieLibraryOverviewFragment extends Fragment {

    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;
    List<String> TITLES = new ArrayList<>();

    public MovieLibraryOverviewFragment() {} // Empty constructor

    public static MovieLibraryOverviewFragment newInstance() {
        return new MovieLibraryOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);
        PagerAdapter mAdapter;
        SharedPreferences mSharedPreferences;
        final String[] TITLES1 = {getString(R.string.choiceAllMovies), getString(R.string.choiceFavorites), getString(R.string.choiceNewReleases),
                getString(R.string.chooserWatchList), getString(R.string.choiceWatchedMovies), getString(R.string.choiceUnwatchedMovies), getString(R.string.choiceCollections),
                getString(R.string.choiceLists), getString(R.string.choiceUpcoming), getString(R.string.choicePopular), getString(R.string.choiceNowPlaying),
                getString(R.string.choiceTopRated)};
        for(int i=0;i<TITLES1.length;i++)
        TITLES.add(i, TITLES1[i]);

        boolean mShowFavourites, mShowNewReleases, mShowWatchlist, mShowWatched, mShowUnwatched,
                mShowCollections, mShowList, mShowUpcoming, mShowPopular, mShowNowRunning, mShowTopRated;


        if (NMJLib.hasLollipop())
            ((ActionBarActivity) getActivity()).getSupportActionBar().setElevation(0);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setPageMargin(NMJLib.convertDpToPixels(getActivity(), 16));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);

        mViewPager.setAdapter(mAdapter = new PagerAdapter(getChildFragmentManager()));
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());

        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);
        System.out.println("Debug: No Tabs: " + mViewPager.getAdapter().getCount());

        // Work-around a bug that sometimes happens with the tabs
        mViewPager.setCurrentItem(0);
        mAdapter.addTab(TITLES.get(0));
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mShowFavourites = mSharedPreferences.getBoolean(SHOW_MOVIE_FAVOURITES, true);
        mShowNewReleases = mSharedPreferences.getBoolean(SHOW_MOVIE_NEW_RELEASES, true);
        mShowWatchlist =  mSharedPreferences.getBoolean(SHOW_MOVIE_WATCHLIST, true);
        mShowWatched = mSharedPreferences.getBoolean(SHOW_MOVIE_WATCHED, true);
        mShowUnwatched = mSharedPreferences.getBoolean(SHOW_MOVIE_UNWATCHED, true);
        mShowCollections = mSharedPreferences.getBoolean(SHOW_MOVIE_COLLECTIONS, true);
        mShowList = mSharedPreferences.getBoolean(SHOW_MOVIE_LIST, true);
        mShowUpcoming =  mSharedPreferences.getBoolean(SHOW_MOVIE_UPCOMING, true);
        mShowPopular = mSharedPreferences.getBoolean(SHOW_MOVIE_POPULAR, true);
        mShowNowRunning = mSharedPreferences.getBoolean(SHOW_MOVIE_NOW_RUNNING, true);
        mShowTopRated = mSharedPreferences.getBoolean(SHOW_MOVIE_TOP_RATED, true);
        System.out.println("Favourites enabled: " + mSharedPreferences.getBoolean(SHOW_MOVIE_FAVOURITES, false));

        if(mShowFavourites)
            mAdapter.addTab(TITLES.get(1));
        if(mShowNewReleases)
            mAdapter.addTab(TITLES.get(2));
        if(mShowWatchlist)
            mAdapter.addTab(TITLES.get(3));
        if(mShowWatched)
            mAdapter.addTab(TITLES.get(4));
        if(mShowUnwatched)
            mAdapter.addTab(TITLES.get(5));
        if(mShowCollections)
            mAdapter.addTab(TITLES.get(6));
        if(mShowList)
            mAdapter.addTab(TITLES.get(7));
        if(mShowUpcoming)
            mAdapter.addTab(TITLES.get(8));
        if(mShowPopular)
            mAdapter.addTab(TITLES.get(9));
        if(mShowNowRunning)
            mAdapter.addTab(TITLES.get(10));
        if(mShowTopRated)
            mAdapter.addTab(TITLES.get(11));

        if (NMJLib.hasLollipop())
            mTabs.setElevation(1f);

        return v;
    }

    public class PagerAdapter extends FragmentPagerAdapter {

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
                    return MovieLibraryFragment.newInstance(MovieLoader.ALL_MOVIES);
                case 1:
                    return MovieLibraryFragment.newInstance(MovieLoader.FAVORITES);
                case 2:
                    return MovieLibraryFragment.newInstance(MovieLoader.NEW_RELEASES);
                case 3:
                    return MovieLibraryFragment.newInstance(MovieLoader.WATCHLIST);
                case 4:
                    return MovieLibraryFragment.newInstance(MovieLoader.WATCHED);
                case 5:
                    return MovieLibraryFragment.newInstance(MovieLoader.UNWATCHED);
                case 6:
                    return MovieLibraryFragment.newInstance(MovieLoader.COLLECTIONS);
                case 7:
                    return MovieLibraryFragment.newInstance(MovieLoader.LISTS);
                case 8:
                    return MovieLibraryFragment.newInstance(MovieLoader.UPCOMING);
                case 9:
                    return MovieLibraryFragment.newInstance(MovieLoader.NOW_PLAYING);
                case 10:
                    return MovieLibraryFragment.newInstance(MovieLoader.POPULAR);
                case 11:
                    return MovieLibraryFragment.newInstance(MovieLoader.TOP_RATED);
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

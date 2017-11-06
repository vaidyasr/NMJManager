package com.nmj.nmjmanager.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.nmj.functions.NMJLib;
import com.nmj.loader.MovieLoader;
import com.nmj.nmjmanager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nmj.functions.PreferenceKeys.MOVIES_TABS_SELECTED;

public class MovieLibraryOverviewFragment extends Fragment {

    private List<String> TITLES = new ArrayList<>();
    private PagerSlidingTabStrip mTabs;

    public MovieLibraryOverviewFragment() {
    } // Empty constructor

    public static MovieLibraryOverviewFragment newInstance() {
        return new MovieLibraryOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);
        PagerAdapter mAdapter;
        ViewPager mViewPager;

        final String[] TITLES1 = {getString(R.string.choiceAllMovies), getString(R.string.choiceFavorites), getString(R.string.choiceNewReleases),
                getString(R.string.chooserWatchList), getString(R.string.choiceWatchedMovies), getString(R.string.choiceUnwatchedMovies), getString(R.string.choiceCollections),
                getString(R.string.choiceLists), getString(R.string.choiceUpcoming), getString(R.string.choicePopular), getString(R.string.choiceNowPlaying),
                getString(R.string.choiceTopRated)};
        for (int i = 0; i < TITLES1.length; i++)
            TITLES.add(i, TITLES1[i]);

        if (NMJLib.hasLollipop())
            ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0);

        mViewPager = v.findViewById(R.id.awesomepager);
        mViewPager.setPageMargin(NMJLib.convertDpToPixels(getActivity(), 16));

        mTabs = v.findViewById(R.id.tabs);

        mViewPager.setAdapter(mAdapter = new PagerAdapter(getChildFragmentManager()));

        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);

        // Work-around a bug that sometimes happens with the tabs
        mViewPager.setCurrentItem(0);
        mAdapter.addTab(TITLES.get(0));
        Set<String> defValues = new HashSet<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));
        Set<String> values = PreferenceManager.getDefaultSharedPreferences(getActivity()).getStringSet(MOVIES_TABS_SELECTED, defValues);
        List<String> list = new ArrayList<>(values);
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                Integer val1 = Integer.parseInt(s1);
                Integer val2 = Integer.parseInt(s2);
                return val1.compareTo(val2);
            }
        });

        for (int i = 0; i < list.size(); i++) {
            mAdapter.addTab(TITLES.get(Integer.parseInt(list.get(i))));
        }
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());

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

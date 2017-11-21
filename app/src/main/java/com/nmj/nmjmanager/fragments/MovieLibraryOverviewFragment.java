package com.nmj.nmjmanager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public static TabLayout mTabs;
    public static int mPosition;
    private static ViewPagerAdapter mAdapter;
    private final List<String> TITLES = new ArrayList<>();

    public MovieLibraryOverviewFragment() {
    } // Empty constructor

    public static MovieLibraryOverviewFragment newInstance() {
        return new MovieLibraryOverviewFragment();
    }

    private static void removeTab(int position) {
        if (mTabs.getTabCount() >= 1 && position < mTabs.getTabCount()) {
            mTabs.removeTabAt(position);
            mAdapter.removeTabPage(position);
        }
    }

    public static String getCurrentTab() {
        return mAdapter.getPageTitle(mPosition).toString();
    }

    public static void initializeTabs(Set<String> values, boolean remove) {
        if (remove) {
            for (int i = 11; i >= 1; i--)
                if (mTabs.getTabAt(i) != null)
                    removeTab(i);
        }

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
            mAdapter.addTabPage(Integer.parseInt(list.get(i)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);
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

        mViewPager.setAdapter(mAdapter = new ViewPagerAdapter(getChildFragmentManager()));

        mTabs.setupWithViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);

        // Work-around a bug that sometimes happens with the tabs
        mViewPager.setCurrentItem(0);
        mAdapter.addTabPage(0);
        Set<String> defValues = new HashSet<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));
        Set<String> values = PreferenceManager.getDefaultSharedPreferences(getActivity()).getStringSet(MOVIES_TABS_SELECTED, defValues);

        initializeTabs(values, false);
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());

        if (NMJLib.hasLollipop())
            mTabs.setElevation(1f);

        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return v;
    }

    public class ViewPagerAdapter extends CustomFragmentPagerAdapter {
        private final List<String> mFragmentTitleList = new ArrayList<>();

        private ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private void addTabPage(int index) {
            mFragmentTitleList.add(TITLES.get(index));
            notifyDataSetChanged();
        }

        private void removeTabPage(int index) {
            mFragmentTitleList.remove(index);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public Fragment getItem(int index) {
            int idx = TITLES.indexOf(mFragmentTitleList.get(index));
            switch (idx) {
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
            return mFragmentTitleList.size();
        }
    }
}

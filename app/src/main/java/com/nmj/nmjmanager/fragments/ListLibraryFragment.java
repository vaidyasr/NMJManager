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

package com.nmj.nmjmanager.fragments;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.nmj.apis.nmj.Movie;
import com.nmj.functions.CoverItem;
import com.nmj.functions.NMJLib;
import com.nmj.loader.MovieLoader;
import com.nmj.loader.MovieLibraryType;
import com.nmj.loader.MovieSortType;
import com.nmj.loader.OnLoadCompletedCallback;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.NMJMovieDetails;
import com.nmj.nmjmanager.R;
import com.nmj.utils.LocalBroadcastUtils;
import com.nmj.utils.TypefaceUtils;
import com.nmj.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.nmj.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.nmj.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.nmj.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class ListLibraryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private String baseUrl, imageSizeUrl;
    private SharedPreferences mSharedPreferences;
    private int mImageThumbSize;
    private LoaderAdapter mAdapter;
    private ObservableGridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles, mIgnorePrefixes, mLoading = true;
    private Picasso mPicasso;
    private Config mConfig;
    private MovieLoader mMovieLoader;
    private View mEmptyLibraryLayout;
    private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;
    private String mListId, mListTmdbId;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMovieLoader != null) {
                if (intent.filterEquals(new Intent("NMJManager-movie-actor-search"))) {
                    mMovieLoader.search("actor: " + intent.getStringExtra("intent_extra_data_key"));
                } else {
                    mMovieLoader.load();
                }
                showProgressBar();
            }
        }
    };
    private OnLoadCompletedCallback mCallback = new OnLoadCompletedCallback() {
        @Override
        public void onLoadCompleted() {
            mAdapter.notifyDataSetChanged();
        }
    };

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ListLibraryFragment() {
    }

    public static ListLibraryFragment newInstance(String listId, String listTitle, String listTmdbId) {
        ListLibraryFragment frag = new ListLibraryFragment();
        Bundle b = new Bundle();
        b.putString("listId", listId);
        b.putString("listTitle", listTitle);
        b.putString("listTmdbId", listTmdbId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int mImageThumbSpacing;
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        mListId = getArguments().getString("listId", "");
        mListTmdbId = getArguments().getString("listTmdbId", "");
        mContext = getActivity().getApplicationContext();

        baseUrl = NMJLib.getTmdbImageBaseUrl(mContext);
        imageSizeUrl = NMJLib.getImageUrlSize(mContext);

        // Set OnSharedPreferenceChange listener
        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);

        // Initialize the PreferenceManager variable and preference variable(s)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
        mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);

        mImageThumbSize = ViewUtils.getGridViewThumbSize(mContext);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mPicasso = NMJManagerApplication.getPicasso(mContext);
        mConfig = NMJManagerApplication.getBitmapConfig();

        mAdapter = new LoaderAdapter(mContext);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("NMJManager-movie-actor-search"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = v.findViewById(R.id.progress);

        mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
        mEmptyLibraryTitle = v.findViewById(R.id.empty_library_title);
        mEmptyLibraryTitle.setTypeface(TypefaceUtils.getRobotoCondensedRegular(mContext));
        mEmptyLibraryDescription = v.findViewById(R.id.empty_library_description);
        mEmptyLibraryDescription.setTypeface(TypefaceUtils.getRobotoLight(mContext));

        mAdapter = new LoaderAdapter(mContext);

        mGridView = v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setColumnWidth(mImageThumbSize);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                viewMovieDetails(arg2, arg1);
            }
        });

        // We only want to display the contextual menu if we're showing movies, not collections
        System.out.println("Type " + getArguments().getInt("type"));
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mAdapter.setItemChecked(position, checked);

                mode.setTitle(String.format(getString(R.string.selected),
                        mAdapter.getCheckedItemCount()));
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.movie_library_cab, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                int id = item.getItemId();

                switch (id) {
                    case R.id.movie_add_fav:
                        NMJLib.setMoviesFavourite(mContext, mAdapter.getCheckedItems(), true);
                        break;
                    case R.id.movie_remove_fav:
                        NMJLib.setMoviesFavourite(mContext, mAdapter.getCheckedItems(), false);
                        break;
                    case R.id.movie_watched:
                        NMJLib.setMoviesWatched(mContext, mAdapter.getCheckedItems(), true);
                        break;
                    case R.id.movie_unwatched:
                        NMJLib.setMoviesWatched(mContext, mAdapter.getCheckedItems(), false);
                        break;
                    case R.id.add_to_watchlist:
                        NMJLib.setMoviesWatchlist(mContext, mAdapter.getCheckedItems(), true);
                        break;
                    case R.id.remove_from_watchlist:
                        NMJLib.setMoviesWatchlist(mContext, mAdapter.getCheckedItems(), false);
                        break;
                    case R.id.add_to_list:
                        NMJLib.setMoviesWatchlist(mContext, mAdapter.getCheckedItems(), true);
                        break;
                    case R.id.remove_from_list:
                        NMJLib.setMoviesWatchlist(mContext, mAdapter.getCheckedItems(), false);
                        break;
                }

                if (!(id == R.id.watched_menu ||
                        id == R.id.watchlist_menu ||
                        id == R.id.favorite_menu ||
                        id == R.id.add_list_menu ||
                        id == R.id.edit_menu)) {
                    mode.finish();

                    LocalBroadcastUtils.updateMovieLibrary(mContext);
                }

                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.clearCheckedItems();
            }
        });

        Intent intent = new Intent();
        intent.putExtra("listId", mListId);
        intent.putExtra("listTmdbId", mListTmdbId);
        mMovieLoader = new MovieLoader(mContext, MovieLibraryType.LIST_MOVIES, intent, mCallback);
        //mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
        mMovieLoader.load();
        showProgressBar();

        return v;
    }

    private void viewMovieDetails(int position, View view) {
        Intent intent = new Intent();
        intent.putExtra("tmdbId", mAdapter.getItem(position).getTmdbId());
        intent.putExtra("showId", mAdapter.getItem(position).getShowId());
        intent.setClass(mContext, NMJMovieDetails.class);
        if (view != null) {
            Pair<View, String> pair = new Pair<>(view.findViewById(R.id.cover), "cover");
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair);
            ActivityCompat.startActivityForResult(getActivity(), intent, 0, options.toBundle());
        } else {
            startActivityForResult(intent, 0);
        }
    }

    private void onSearchViewCollapsed() {
        mMovieLoader.load();
        showProgressBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        SearchView mSearchView;
        inflater.inflate(R.menu.menu, menu);

        menu.findItem(R.id.random).setVisible(mMovieLoader.getType() != MovieLibraryType.LISTS);

        menu.removeItem(R.id.update);
        menu.removeItem(R.id.filters);
        menu.removeItem(R.id.unidentifiedFiles);

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.search_textbox), new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                onSearchViewCollapsed();
                return true;
            }
        });

        mSearchView = (SearchView) menu.findItem(R.id.search_textbox).getActionView();
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 0) {
                    mMovieLoader.search(newText);
                    showProgressBar();
                } else {
                    onSearchViewCollapsed();
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menuSortAdded:
                mMovieLoader.setSortType(MovieSortType.DATE_ADDED);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRating:
                mMovieLoader.setSortType(MovieSortType.RATING);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRelease:
                mMovieLoader.setSortType(MovieSortType.RELEASE);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortTitle:
                mMovieLoader.setSortType(MovieSortType.TITLE);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortDuration:
                mMovieLoader.setSortType(MovieSortType.DURATION);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.random:
                if (mAdapter.getCount() > 0) {
                    int random = new Random().nextInt(mAdapter.getCount());
                    viewMovieDetails(random, null);
                }
                break;
        }

        return true;
    }

    private void hideProgressBar() {
        mGridView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mLoading = false;
    }

    private void showProgressBar() {
        mGridView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mLoading = true;
    }

    private void showEmptyView() {
        mGridView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mEmptyLibraryLayout.setVisibility(View.VISIBLE);

        if (mMovieLoader.isShowingSearchResults()) {
            mEmptyLibraryTitle.setText(R.string.no_search_results);
            mEmptyLibraryDescription.setText(R.string.no_search_results_description);
        } else {
            switch (mMovieLoader.getType()) {
                case ALL_MOVIES:
                    mEmptyLibraryTitle.setText(R.string.no_movies);
                    mEmptyLibraryDescription.setText(NMJLib.isTablet(mContext) ?
                            R.string.no_movies_description_tablet : R.string.no_movies_description);
                    break;
                case FAVORITES:
                    mEmptyLibraryTitle.setText(R.string.no_favorites);
                    mEmptyLibraryDescription.setText(R.string.no_favorites_description);
                    break;
                case NEW_RELEASES:
                    mEmptyLibraryTitle.setText(R.string.no_new_releases);
                    mEmptyLibraryDescription.setText(R.string.no_new_releases_description);
                    break;
                case WATCHLIST:
                    mEmptyLibraryTitle.setText(R.string.empty_watchlist);
                    mEmptyLibraryDescription.setText(R.string.empty_watchlist_description);
                    break;
                case WATCHED:
                    mEmptyLibraryTitle.setText(R.string.no_watched_movies);
                    mEmptyLibraryDescription.setText(R.string.no_watched_movies_description);
                    break;
                case UNWATCHED:
                    mEmptyLibraryTitle.setText(R.string.no_unwatched_movies);
                    mEmptyLibraryDescription.setText(R.string.no_unwatched_movies_description);
                    break;
                case COLLECTIONS:
                    mEmptyLibraryTitle.setText(R.string.no_movie_collections);
                    mEmptyLibraryDescription.setText(R.string.no_movie_collections_description);
                    break;
                case LISTS:
                    mEmptyLibraryTitle.setText(R.string.no_movie_lists);
                    mEmptyLibraryDescription.setText(R.string.no_movie_lists_description);
                    break;
            }
        }
    }

    private void hideEmptyView() {
        mEmptyLibraryLayout.setVisibility(View.GONE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case IGNORED_TITLE_PREFIXES:
                mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);

                if (mMovieLoader != null) {
                    //mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
                    mMovieLoader.load();
                }
                break;
            case GRID_ITEM_SIZE:
                mImageThumbSize = ViewUtils.getGridViewThumbSize(mContext);

                if (mGridView != null)
                    mGridView.setColumnWidth(mImageThumbSize);

                mAdapter.notifyDataSetChanged();
                break;
            case SHOW_TITLES_IN_GRID:
                mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    private class LoaderAdapter extends BaseAdapter {

        private final Context mContext;
        private Set<Integer> mChecked = new HashSet<>();
        private LayoutInflater mInflater;
        private Typeface mTypeface;

        public LoaderAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTypeface = TypefaceUtils.getRobotoMedium(mContext);
        }

        public void setItemChecked(int index, boolean checked) {
            if (checked)
                mChecked.add(index);
            else
                mChecked.remove(index);

            notifyDataSetChanged();
        }

        public void clearCheckedItems() {
            mChecked.clear();
            notifyDataSetChanged();
        }

        public int getCheckedItemCount() {
            return mChecked.size();
        }

        public List<String> getCheckedMovies() {
            List<String> movies = new ArrayList<>(mChecked.size());
            for (Integer i : mChecked)
                movies.add(getItem(i).getShowId());
            return movies;
        }

        public List<Movie> getCheckedItems() {
            List<Movie> movies = new ArrayList<>(mChecked.size());
            for (Integer i : mChecked)
                movies.add(getItem(i));
            return movies;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0 && !mLoading;
        }

        @Override
        public int getCount() {
            if (mMovieLoader != null)
                return mMovieLoader.getResults().size();
            return 0;
        }

        @Override
        public Movie getItem(int position) {
            return mMovieLoader.getResults().get(position);
        }

        public boolean hasWatched(int position) {
            return getItem(position).hasWatched();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            final Movie movie = getItem(position);
            String mURL;

            CoverItem holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.grid_cover, container, false);
                holder = new CoverItem();

                holder.cardview = convertView.findViewById(R.id.card);
                holder.cover = convertView.findViewById(R.id.cover);
                holder.hasWatched = convertView.findViewById(R.id.hasWatched);
                holder.inLibrary = convertView.findViewById(R.id.inLibrary);
                holder.text = convertView.findViewById(R.id.text);
                holder.text.setTypeface(mTypeface);

                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            if (!mShowTitles) {
                holder.text.setVisibility(View.GONE);
            } else {
                holder.text.setVisibility(View.VISIBLE);
/*                holder.text.setText(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
                        movie.getCollection() : movie.getTitle());*/
                holder.text.setText(movie.getTitle());
            }

            holder.cover.setImageResource(R.color.card_background_dark);

            //mPicasso.load(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
            //       movie.getCollectionPoster() : movie.getThumbnail()).placeholder(R.drawable.bg).config(mConfig).into(holder);

            //System.out.println("baseUrl: " + baseUrl);
            //System.out.println("Thumbnail: " + movie.getNMJThumbnail());
            //System.out.println("Video Type:" + movie.getVideoType());
            if (movie.getTitleType().equals("tmdb"))
                mURL = baseUrl + imageSizeUrl;
            else
                mURL = NMJLib.getNMJImageURL();
            mPicasso.load(mURL + movie.getThumbnail()).placeholder(R.drawable.bg).config(mConfig).into(holder);
            if (mChecked.contains(position)) {
                holder.cardview.setForeground(getResources().getDrawable(R.drawable.checked_foreground_drawable));
            } else {
                holder.cardview.setForeground(null);
            }

            if (hasWatched(position))
                holder.hasWatched.setVisibility(View.VISIBLE);
            else
                holder.hasWatched.setVisibility(View.GONE);

            holder.inLibrary.setVisibility(View.GONE);
            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();

            // Hide the progress bar once the data set has been changed
            hideProgressBar();

            if (isEmpty()) {
                showEmptyView();
            } else {
                hideEmptyView();
            }
        }
    }
}
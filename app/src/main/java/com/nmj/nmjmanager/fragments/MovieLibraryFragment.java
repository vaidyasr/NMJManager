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
import android.support.annotation.NonNull;
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
import android.widget.AbsListView.OnScrollListener;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.nmj.apis.nmj.Movie;
import com.nmj.functions.CoverItem;
import com.nmj.functions.NMJLib;
import com.nmj.loader.MovieLoader;
import com.nmj.loader.MovieLibraryType;
import com.nmj.loader.MovieSortType;
import com.nmj.loader.OnLoadCompletedCallback;
import com.nmj.nmjmanager.MovieCollection;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.MovieList;
import com.nmj.nmjmanager.NMJMovieDetails;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.UnidentifiedMovies;
import com.nmj.nmjmanager.Update;
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
import static com.nmj.loader.MovieLibraryType.NOW_PLAYING;
import static com.nmj.loader.MovieLibraryType.POPULAR;
import static com.nmj.loader.MovieLibraryType.TOP_RATED;
import static com.nmj.loader.MovieLibraryType.UPCOMING;
import static com.nmj.loader.MovieLoader.LISTS;
import static com.nmj.loader.MovieLoader.SORT_DATE_ADDED;
import static com.nmj.loader.MovieLoader.SORT_DURATION;
import static com.nmj.loader.MovieLoader.SORT_RATING;
import static com.nmj.loader.MovieLoader.SORT_RELEASE;
import static com.nmj.loader.MovieLoader.SORT_TITLE;
import static com.nmj.nmjmanager.Main.disableFilterDrawerMenu;
import static com.nmj.nmjmanager.Main.enableFilterDrawerMenu;
import static com.nmj.nmjmanager.Main.togglefilterDrawerMenu;

public class MovieLibraryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context mContext;
    private String baseUrl, imageSizeUrl;
    private SharedPreferences mSharedPreferences;
    private int mImageThumbSize, mImageThumbSpacing;
    private LoaderAdapter mAdapter;
    private ObservableGridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles, mIgnorePrefixes, mLoading = true;
    private Picasso mPicasso;
    private Config mConfig;
    private MovieLoader mMovieLoader;
    private SearchView mSearchView;
    private View mEmptyLibraryLayout;
    private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;
    private MenuItem sortMenu;
    private View mMovieView;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMovieLoader != null) {
                if (intent.filterEquals(new Intent("NMJManager-movie-actor-search"))) {
                    mMovieLoader.search("actor: " + intent.getStringExtra("intent_extra_data_key"));
                    showProgressBar();
                } else if (intent.filterEquals(new Intent("NMJManager-movies-filter"))) {
                    hideEmptyView();
                    mMovieLoader.clearAll();
                    mMovieLoader.filter(intent.getExtras().getString(("filterURL")));
                    showProgressBar();
                } else if (intent.filterEquals(new Intent("NMJManager-movies-load"))) {
                    hideEmptyView();
                    mMovieLoader.clearAll();
                    mMovieLoader.load();
                    showProgressBar();
                } else if (intent.filterEquals(new Intent("NMJManager-movies-update"))) {
                    //System.out.println("MovieLoaderType: " + mMovieLoader.getType());
                    if (mMovieLoader.getType() == MovieLibraryType.WATCHED ||
                            mMovieLoader.getType() == MovieLibraryType.UNWATCHED ||
                            mMovieLoader.getType() == MovieLibraryType.FAVORITES ||
                            mMovieLoader.getType() == MovieLibraryType.WATCHLIST) {
                        hideEmptyView();
                        mMovieLoader.clearAll();
                        mMovieLoader.load();
                        showProgressBar();
                    } else
                        mAdapter.notifyDataSetChanged();
                } else {
                    hideEmptyView();
                    mMovieLoader.load();
                    showProgressBar();
                }
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
    public MovieLibraryFragment() {
    }

    public static MovieLibraryFragment newInstance(int type) {
        MovieLibraryFragment frag = new MovieLibraryFragment();
        Bundle b = new Bundle();
        b.putInt("type", type);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

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

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("NMJManager-movie-actor-search"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.FILTER_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.LOAD_MOVIE_LIBRARY));

        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
        mMovieView = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mMovieView = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = mMovieView.findViewById(R.id.progress);

        mEmptyLibraryLayout = mMovieView.findViewById(R.id.empty_library_layout);
        mEmptyLibraryTitle = mMovieView.findViewById(R.id.empty_library_title);
        mEmptyLibraryTitle.setTypeface(TypefaceUtils.getRobotoCondensedRegular(mContext));
        mEmptyLibraryDescription = mMovieView.findViewById(R.id.empty_library_description);
        mEmptyLibraryDescription.setTypeface(TypefaceUtils.getRobotoLight(mContext));

        mAdapter = new LoaderAdapter(mContext);

        mGridView = mMovieView.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setColumnWidth(mImageThumbSize);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                viewMovieDetails(arg2, arg1);
            }
        });

        mGridView.setOnScrollListener(new EndlessScrollListener());

        // We only want to display the contextual menu if we're showing movies, not collections
        if (getArguments().getInt("type") != MovieLoader.COLLECTIONS &&
                getArguments().getInt("type") != MovieLoader.LISTS &&
                getArguments().getInt("type") != MovieLoader.TOP_RATED &&
                getArguments().getInt("type") != MovieLoader.POPULAR &&
                getArguments().getInt("type") != MovieLoader.NOW_PLAYING &&
                getArguments().getInt("type") != MovieLoader.UPCOMING &&
                !NMJLib.isNMJDbWritable()) {
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
                    if (getActivity() != null)
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
                        case R.id.delete_button_2:
                            new MaterialDialog.Builder(getActivity())
                                    .iconRes(R.drawable.ic_movie_white_24dp)
                                    .limitIconToDefaultSize()
                                    .title("Do you want to delete the Movie?")
                                    .positiveText("Yes")
                                    .negativeText("No")
                                    .onAny(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            Toast.makeText(mContext, "Prompt checked? " + dialog.isPromptCheckBoxChecked(), Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .checkBoxPrompt("Delete media files", true, null)
                                    .show();
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
        }
        Intent intent = new Intent();
        mMovieLoader = new MovieLoader(mContext, MovieLibraryType.fromInt(getArguments().getInt("type")), intent, mCallback);

        if (NMJManagerApplication.getNMJAdapter().getLibrary() == null) {
            showEmptyView();
        } else {
            mMovieLoader.clearAll();
            //mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
            mMovieLoader.load();
            showProgressBar();
        }
        return mMovieView;
    }

    private void showDeleteDialog() {
        new MaterialDialog.Builder(getContext())
                .iconRes(R.drawable.ic_movie_white_24dp)
                .limitIconToDefaultSize()
                .title("Do you want to delete the Movie?")
                .positiveText("Yes")
                .negativeText("No")
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Toast.makeText(mContext, "Prompt checked? " + dialog.isPromptCheckBoxChecked(), Toast.LENGTH_LONG).show();
                    }
                })
                .checkBoxPrompt("Delete media files", true, null)
                .show();
    }

    private void viewMovieDetails(int position, View view) {
        Intent intent = new Intent();
        String tmdbId = mAdapter.getItem(position).getTmdbId();
        if (mMovieLoader.getType() == MovieLibraryType.COLLECTIONS) { // Collection
            intent.putExtra("type", MovieLibraryType.COLLECTIONS.toString());
            intent.putExtra("collectionId", mAdapter.getItem(position).getCollectionId());
            intent.putExtra("collectionTitle", mAdapter.getItem(position).getTitle());
            intent.putExtra("collectionTmdbId", tmdbId);
            intent.setClass(mContext, MovieCollection.class);
            //startActivity(intent);
        } else if (mMovieLoader.getType() == MovieLibraryType.LISTS) { // Collection
            intent.putExtra("type", MovieLibraryType.LISTS.toString());
            intent.putExtra("listId", mAdapter.getItem(position).getListId());
            intent.putExtra("listTitle", mAdapter.getItem(position).getTitle());
            intent.putExtra("listTmdbId", tmdbId);
            intent.setClass(mContext, MovieList.class);
            //startActivity(intent);
        } else if (mMovieLoader.getType() == MovieLibraryType.UPCOMING ||
                mMovieLoader.getType() == MovieLibraryType.TOP_RATED ||
                mMovieLoader.getType() == MovieLibraryType.POPULAR ||
                mMovieLoader.getType() == MovieLibraryType.NOW_PLAYING) { // Collection
            intent.putExtra("tmdbId", tmdbId);
            intent.putExtra("showId", NMJManagerApplication.getNMJAdapter().getShowIdByTmdbId(tmdbId));
            intent.setClass(mContext, NMJMovieDetails.class);
        } else {
            intent.putExtra("tmdbId", tmdbId);
            intent.putExtra("showId", mAdapter.getItem(position).getShowId());
            intent.setClass(mContext, NMJMovieDetails.class);
        }
        if (view != null) {
            Pair<View, String> pair = new Pair<>(view.findViewById(R.id.cover), "cover");
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair);
            ActivityCompat.startActivityForResult(getActivity(), intent, 0, options.toBundle());
        } else {
            startActivityForResult(intent, 0);
        }
    }

    private void onSearchViewCollapsed() {
        mMovieLoader.clearAll();
        mMovieLoader.load();
        showProgressBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        //System.out.println("Sort Order: " + NMJLib.getSortOrder());

        if (NMJLib.getSortOrder().equals("asc"))
            menu.findItem(R.id.sort).setIcon(R.drawable.ic_sort_asc_white_24dp);
        else
            menu.findItem(R.id.sort).setIcon(R.drawable.ic_sort_desc_white_24dp);

        switch (NMJLib.getSortType()) {
            case SORT_TITLE:
                menu.findItem(R.id.menuSortTitle).setChecked(true);
                break;
            case SORT_RELEASE:
                menu.findItem(R.id.menuSortRelease).setChecked(true);
                break;
            case SORT_RATING:
                menu.findItem(R.id.menuSortRating).setChecked(true);
                break;
            case SORT_DATE_ADDED:
                menu.findItem(R.id.menuSortAdded).setChecked(true);
                break;
            case SORT_DURATION:
                menu.findItem(R.id.menuSortDuration).setChecked(true);
                break;
        }

        menu.findItem(R.id.random).setVisible(mMovieLoader.getType() != MovieLibraryType.COLLECTIONS &&
                mMovieLoader.getType() != MovieLibraryType.LISTS);

        enableFilterDrawerMenu();

        if (mMovieLoader.getType() == MovieLibraryType.COLLECTIONS || mMovieLoader.getType() == MovieLibraryType.LISTS) {
            menu.findItem(R.id.sort).setVisible(false);
            menu.findItem(R.id.filters).setVisible(false);
            disableFilterDrawerMenu();
        }

        if (mMovieLoader.getType() == MovieLibraryType.POPULAR ||
                mMovieLoader.getType() == MovieLibraryType.TOP_RATED ||
                mMovieLoader.getType() == MovieLibraryType.NOW_PLAYING ||
                mMovieLoader.getType() == MovieLibraryType.UPCOMING) {
            menu.findItem(R.id.search_textbox).setVisible(false);
            menu.findItem(R.id.sort).setVisible(false);
            menu.findItem(R.id.filters).setVisible(false);
            menu.findItem(R.id.random).setVisible(false);
            disableFilterDrawerMenu();
        }

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
                    mMovieLoader.clearAll();
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
            case R.id.update:
                Intent intent = new Intent();
                intent.setClass(mContext, Update.class);
                intent.putExtra("isMovie", true);
                startActivityForResult(intent, 0);
                break;
            case R.id.menuSortAdded:
                mMovieLoader.setSortType(MovieSortType.DATE_ADDED);
                mMovieLoader.clearAll();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRating:
                mMovieLoader.setSortType(MovieSortType.RATING);
                mMovieLoader.clearAll();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRelease:
                mMovieLoader.setSortType(MovieSortType.RELEASE);
                mMovieLoader.clearAll();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortTitle:
                mMovieLoader.setSortType(MovieSortType.TITLE);
                mMovieLoader.clearAll();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortDuration:
                mMovieLoader.setSortType(MovieSortType.DURATION);
                mMovieLoader.clearAll();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.filters:
                togglefilterDrawerMenu();
                break;
            case R.id.random:
                if (mAdapter.getCount() > 0) {
                    int random = new Random().nextInt(mAdapter.getCount());
                    viewMovieDetails(random, null);
                }
                break;
            case R.id.unidentifiedFiles:
                startActivity(new Intent(mContext, UnidentifiedMovies.class));
                break;
        }
        if (item.getTitle().equals("Sort"))
            sortMenu = item;
        else
            item.setChecked(true);
        if (sortMenu != null) {
            if (NMJLib.getSortOrder().equals("asc"))
                sortMenu.setIcon(R.drawable.ic_sort_asc_white_24dp);
            else
                sortMenu.setIcon(R.drawable.ic_sort_desc_white_24dp);
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
        } else if (mMovieLoader.isShowingFilterResults() && (mMovieLoader.getType() != MovieLibraryType.LISTS &&
                mMovieLoader.getType() != MovieLibraryType.COLLECTIONS)) {
            mEmptyLibraryTitle.setText(R.string.no_filter_results);
            mEmptyLibraryDescription.setText(R.string.no_filter_results_description);
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
        if (key.equals(IGNORED_TITLE_PREFIXES)) {
            mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);

            if (mMovieLoader != null) {
                //mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
                mMovieLoader.load();
            }

        } else if (key.equals(GRID_ITEM_SIZE)) {
            mImageThumbSize = ViewUtils.getGridViewThumbSize(mContext);

            if (mGridView != null)
                mGridView.setColumnWidth(mImageThumbSize);

            mAdapter.notifyDataSetChanged();
        } else if (key.equals(SHOW_TITLES_IN_GRID)) {
            mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
            mAdapter.notifyDataSetChanged();
        }
    }

    public class EndlessScrollListener implements OnScrollListener {
        // The minimum number of items to have below your current scroll position
        // before loading more.
        private int visibleThreshold = 5;
        // The current offset index of data you have loaded
        private int currentPage = 0;
        // The total number of items in the dataset after the last load
        private int previousTotalItemCount = 0;
        // True if we are still waiting for the last set of data to load.
        private boolean loading = true;
        // Sets the starting page index
        private int startingPageIndex = 0;

        public EndlessScrollListener() {
        }

        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        public EndlessScrollListener(int visibleThreshold, int startPage) {
            this.visibleThreshold = visibleThreshold;
            this.startingPageIndex = startPage;
            this.currentPage = startPage;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            // If the total item count is zero and the previous isn't, assume the
            // list is invalidated and should be reset back to initial state
            if (totalItemCount < previousTotalItemCount) {
                this.currentPage = this.startingPageIndex;
                this.previousTotalItemCount = totalItemCount;
                if (totalItemCount == 0) {
                    this.loading = true;
                }
            }

            // If it's still loading, we check to see if the dataset count has
            // changed, if so we conclude it has finished loading and update the current page
            // number and total item count.
            if (loading && (totalItemCount > previousTotalItemCount)) {
                loading = false;
                previousTotalItemCount = totalItemCount;
                currentPage++;
            }

            // If it isn't currently loading, we check to see if we have breached
            // the visibleThreshold and need to reload more data.
            // If we do need to reload some more data, we execute onLoadMore to fetch the data.
            if (!loading && (firstVisibleItem + visibleItemCount + visibleThreshold) >= totalItemCount) {
                if (MovieLibraryType.fromInt(getArguments().getInt("type")) == UPCOMING ||
                        MovieLibraryType.fromInt(getArguments().getInt("type")) == POPULAR ||
                        MovieLibraryType.fromInt(getArguments().getInt("type")) == NOW_PLAYING ||
                        MovieLibraryType.fromInt(getArguments().getInt("type")) == TOP_RATED)
                    mMovieLoader.loadMore(totalItemCount, currentPage + 1);
                else
                    mMovieLoader.loadMore(totalItemCount, 25);
                mAdapter.notifyDataSetChanged();
                System.out.println("Loading...");
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    public class LoaderAdapter extends BaseAdapter {
        private final Context mContext;
        private Set<Integer> mChecked = new HashSet<>();
        private LayoutInflater mInflater;
        private Typeface mTypeface;

        private LoaderAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTypeface = TypefaceUtils.getRobotoMedium(mContext);
        }

        private void setItemChecked(int index, boolean checked) {
            if (checked)
                mChecked.add(index);
            else
                mChecked.remove(index);

            notifyDataSetChanged();
        }

        private void clearCheckedItems() {
            mChecked.clear();
            notifyDataSetChanged();
        }

        private int getCheckedItemCount() {
            return mChecked.size();
        }

        private List<Movie> getCheckedItems() {
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
            //holder.hasWatched.setImageResource(R.color.card_background_dark);
            //mPicasso.load(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
            //       movie.getCollectionPoster() : movie.getThumbnail()).placeholder(R.drawable.bg).config(mConfig).into(holder);

            //System.out.println("baseUrl: " + baseUrl);
            //System.out.println("Thumbnail: " + movie.getPoster());
            //System.out.println("Video Type:" + movie.getVideoType());
            if (movie.getShowId().equals("0"))
                mURL = baseUrl + imageSizeUrl;
            else
                mURL = NMJLib.getNMJImageURL();
            mPicasso.load(mURL + movie.getPoster()).placeholder(R.drawable.bg).config(mConfig).into(holder);
            if (mChecked.contains(position)) {
                holder.cardview.setForeground(getResources().getDrawable(R.drawable.checked_foreground_drawable));
            } else {
                holder.cardview.setForeground(null);
            }
            holder.hasWatched.setVisibility(View.GONE);
            holder.inLibrary.setVisibility(View.GONE);

            if (NMJManagerApplication.getNMJAdapter().getWatchedByShowId(movie.getShowId()))
                holder.hasWatched.setVisibility(View.VISIBLE);


            if (movie.getTitleType() == "tmdb" && NMJManagerApplication.getNMJAdapter().movieExistsbyId("tmdb" + movie.getTmdbId())) {
                //System.out.println("Movie Exists:");
                holder.inLibrary.setVisibility(View.VISIBLE);
                if (NMJManagerApplication.getNMJAdapter().hasWatched(movie.getTmdbId()))
                    holder.hasWatched.setVisibility(View.VISIBLE);
            }
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
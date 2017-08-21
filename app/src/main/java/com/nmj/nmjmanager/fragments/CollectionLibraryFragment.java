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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.cache.LoadingCache;
import com.nmj.db.DatabaseHelper;
import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.AsyncTask;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.functions.CoverItem;
import com.nmj.functions.LibrarySectionAsyncTask;
import com.nmj.functions.MediumMovie;
import com.nmj.functions.NMJCache;
import com.nmj.functions.NMJLib;
import com.nmj.functions.NMJMovie;
import com.nmj.functions.SQLiteCursorLoader;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.MovieDetails;
import com.nmj.nmjmanager.NMJMovieDetails;
import com.nmj.nmjmanager.R;
import com.nmj.utils.LocalBroadcastUtils;
import com.nmj.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static com.nmj.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.nmj.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.nmj.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class CollectionLibraryFragment extends Fragment implements OnSharedPreferenceChangeListener {

    private SharedPreferences mSharedPreferences;
    private  Context mContext;
    private int mImageThumbSize, mImageThumbSpacing, mResizedWidth, mResizedHeight, mCurrentSort;
    private LoaderAdapter mAdapter;
    private ArrayList<NMJMovie> mMovies = new ArrayList<NMJMovie>();
    private ArrayList<Integer> mMovieKeys = new ArrayList<Integer>();
    private GridView mGridView = null;
    private ProgressBar mProgressBar;
    private boolean mIgnorePrefixes, mLoading, mShowTitles;
    private Picasso mPicasso;
    private Config mConfig;
    private MovieSectionLoader mMovieSectionLoader;
    private String mCollectionId, mCollectionTmdbId;
    LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            mLoading = true;
            return new SQLiteCursorLoader(getActivity(), DatabaseHelper.getHelper(getActivity()).getWritableDatabase(), DbAdapterMovies.DATABASE_TABLE,
                    DbAdapterMovies.SELECT_ALL, DbAdapterMovies.KEY_COLLECTION_ID + " = '" + mCollectionId + "'", null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> arg0, final Cursor cursor) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    mMovies.clear();
                    mMovieKeys.clear();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    String url = "http://pchportal.duckdns.org/NMJManagerTablet_web/gd.php?action=getCollections&drivepath=My_Book&sourceurl=undefined&dbpath=My_Book/nmj_database/media.db&id=" + mCollectionId + "&sortby=title&orderby=asc";
                    try {
                        JSONObject jObject;
                        LoadingCache<String, String> JSONCache = NMJCache.getLoadingCache();
                        String CacheId = "collection_" + mCollectionId;
                        if (JSONCache.get(mCollectionId) == "") {
                            jObject = NMJLib.getJSONObject(mContext, url);
                            JSONCache.put(CacheId, jObject.toString());
                            System.out.println("Putting Cache in " + CacheId);
                        } else {
                            jObject = new JSONObject(JSONCache.get(CacheId));
                            System.out.println("Getting Cache from " + CacheId);
                        }
                        JSONArray jArray = jObject.getJSONArray("data");

                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject dObject = jArray.getJSONObject(i);
                            System.out.println("Output: " + dObject.toString());
                            mMovies.add(new NMJMovie(getActivity(),
                                    dObject.getString("TITLE"),
                                    dObject.getString("tmdbid"),
                                    dObject.getString("RATING"),
                                    dObject.getString("RELEASE_DATE"),
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    dObject.getString("PLAY_COUNT"),
                                    "",
                                    "",
                                    "",
                                    dObject.getString("SHOW_ID"),
                                    dObject.getString("POSTER"),
                                    true));
                        }
                    } catch (Exception ignored) {

                    }

                    for (int i = 0; i < mMovies.size(); i++) {
                        mMovieKeys.add(i);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    showMovieSection(0);

                    mLoading = false;
                }
            }.execute();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {
            mMovies.clear();
            mMovieKeys.clear();
            notifyDataSetChanged();
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mContext = context;
            clearCaches();
            forceLoaderLoad();
        }
    };

    /**
     * Empty constructor as per the Fragment documentation
     */
    public CollectionLibraryFragment() {
    }

    public static CollectionLibraryFragment newInstance(String collectionId, String collectionTitle, String collectionTmdbId) {
        CollectionLibraryFragment frag = new CollectionLibraryFragment();
        Bundle b = new Bundle();
        b.putString("collectionId", collectionId);
        b.putString("collectionTitle", collectionTitle);
        b.putString("collectionTmdbId", collectionTmdbId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCollectionId = getArguments().getString("collectionId", "");
        mCollectionTmdbId = getArguments().getString("collectionTmdbId", "");
        if (TextUtils.isEmpty(mCollectionId)) {
            getActivity().finish();
            return;
        }

        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Set OnSharedPreferenceChange listener
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

        // Initialize the PreferenceManager variable and preference variable(s)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
        mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);

        String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
        if (thumbnailSize.equals(getString(R.string.large)))
            mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
        else if (thumbnailSize.equals(getString(R.string.normal)))
            mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1;
        else
            mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mPicasso = NMJManagerApplication.getPicasso(getActivity());
        mConfig = NMJManagerApplication.getBitmapConfig();

        mAdapter = new LoaderAdapter(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
    }

    private void clearCaches() {
        if (isAdded())
            NMJManagerApplication.clearLruCache(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_grid_fragment, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        if (mMovieKeys.size() > 0)
            mProgressBar.setVisibility(View.GONE);

        mAdapter = new LoaderAdapter(getActivity());

        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setColumnWidth(mImageThumbSize);

        // Calculate the total column width to set item heights by factor 1.5
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                mAdapter.setNumColumns(numColumns);
                                mResizedWidth = (int) (((mGridView.getWidth() - (numColumns * mImageThumbSpacing))
                                        / numColumns) * 1.1); // * 1.1 is a hack to make images look slightly less blurry
                                mResizedHeight = (int) (mResizedWidth * 1.5);
                            }

                            NMJLib.removeViewTreeObserver(mGridView.getViewTreeObserver(), this);
                        }
                    }
                });
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showDetails(arg2);
            }
        });
    }

    private void showDetails(int arg2) {
        Intent intent = new Intent();
        intent.putExtra("showId", mMovies.get(mMovieKeys.get(arg2)).getShowId());

        intent.setClass(getActivity(), NMJMovieDetails.class);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMovies.size() == 0)
            forceLoaderLoad();

        notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    private void notifyDataSetChanged() {
        if (mAdapter != null)
            mAdapter.setItems(mMovieKeys, mMovies);
    }

    private void showMovieSection(int position) {
        if (mMovieSectionLoader != null)
            mMovieSectionLoader.cancel(true);
        mMovieSectionLoader = new MovieSectionLoader(position);
        mMovieSectionLoader.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu, menu);

        menu.removeItem(R.id.update);
        menu.removeItem(R.id.search_textbox);
        menu.removeItem(R.id.filters);
        menu.removeItem(R.id.sort);
        menu.removeItem(R.id.unidentifiedFiles);

        menu.findItem(R.id.view_collection_online).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.random:
                if (mMovieKeys.size() > 0) {
                    int random = new Random().nextInt(mMovieKeys.size());
                    showDetails(random);
                }
                break;
            case R.id.view_collection_online:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.themoviedb.org/collection/" + mCollectionTmdbId.replace("tmdb","")));
                startActivity(i);
                break;
        }

        return true;
    }

    private void setProgressBarVisible(boolean visible) {
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        mGridView.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 1) { // Update
            forceLoaderLoad();
        } else if (resultCode == 3) {
            notifyDataSetChanged();
        }
    }

    /**
     * Creates movie objects from a URL and adds them to a list.
     *
     * @param collectionId
     * @return List of movie objects from the supplied URL.
     */
    private ArrayList<NMJMovie> listFromJSON(String collectionId) {
        ArrayList<NMJMovie> list = new ArrayList<>();


        return list;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(IGNORED_TITLE_PREFIXES)) {
            mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
            forceLoaderLoad();
        } else if (key.equals(GRID_ITEM_SIZE)) {
            String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
            if (thumbnailSize.equals(getString(R.string.large)))
                mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
            else if (thumbnailSize.equals(getString(R.string.normal)))
                mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1;
            else
                mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);

            mGridView.setColumnWidth(mImageThumbSize);

            final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
            if (numColumns > 0) {
                mAdapter.setNumColumns(numColumns);
            }

            notifyDataSetChanged();
        } else if (key.equals(SHOW_TITLES_IN_GRID)) {
            mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
            notifyDataSetChanged();
        }
    }

    private void forceLoaderLoad() {
        if (isAdded())
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, loaderCallbacks);
            } else {
                getLoaderManager().restartLoader(0, null, loaderCallbacks);
            }
    }

    private class LoaderAdapter extends BaseAdapter {

        private final Context mContext;
        private LayoutInflater mInflater;
        private int mNumColumns = 0;
        private ArrayList<Integer> mMovieKeys = new ArrayList<Integer>();
        private ArrayList<NMJMovie> mMovies = new ArrayList<NMJMovie>();

        public LoaderAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        // This is necessary in order to avoid random ArrayOutOfBoundsException when changing the items (i.e. during a library update)
        public void setItems(ArrayList<Integer> movieKeys, ArrayList<NMJMovie> movies) {
            mMovieKeys = new ArrayList<Integer>(movieKeys);
            mMovies = new ArrayList<NMJMovie>(movies);
            notifyDataSetChanged();
        }

        @Override
        public boolean isEmpty() {
            return (!mLoading && mMovieKeys.size() == 0);
        }

        @Override
        public int getCount() {
            return mMovieKeys.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {

            final NMJMovie mMovie = mMovies.get(mMovieKeys.get(position));

            CoverItem holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.grid_cover_two_line, container, false);
                holder = new CoverItem();

                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.text.setSingleLine(true);
                holder.subtext = (TextView) convertView.findViewById(R.id.sub_text);
                holder.subtext.setSingleLine(true);

                holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            if (!mShowTitles) {
                holder.text.setVisibility(View.GONE);
                holder.subtext.setVisibility(View.GONE);
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.subtext.setVisibility(View.VISIBLE);

                holder.text.setText(mMovie.getTitle());
                System.out.println("Collection Title: " + mMovie.getTitle());
                holder.text.setText("Test1234");
                holder.subtext.setText(mMovie.getSubText(mCurrentSort));
            }

            holder.cover.setImageResource(R.color.card_background_dark);
            String mURL = "http://pchportal.duckdns.org/NMJManagerTablet_web/My_Book/";
            if (mResizedWidth > 0)
                mPicasso.load(mURL + mMovie.getNMJThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
            else
                mPicasso.load(mURL + mMovie.getNMJThumbnail()).config(mConfig).into(holder);

            return convertView;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }
    }

    private class MovieSectionLoader extends LibrarySectionAsyncTask<Void, Void, Boolean> {
        private int mPosition;
        private ArrayList<Integer> mTempKeys = new ArrayList<Integer>();

        public MovieSectionLoader(int position) {
            mPosition = position;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarVisible(true);
            mMovieKeys.clear();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (isCancelled())
                return false;

            switch (mPosition) {
                case 0:
                    for (int i = 0; i < mMovies.size(); i++)
                        mTempKeys.add(i);
                    break;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // Make sure that the loading was successful, that the Fragment is still added and
            // that the currently selected navigation index is the same as when we started loading
            if (success && isAdded()) {
                mMovieKeys.addAll(mTempKeys);

                notifyDataSetChanged();
                setProgressBarVisible(false);
            }
        }
    }
}
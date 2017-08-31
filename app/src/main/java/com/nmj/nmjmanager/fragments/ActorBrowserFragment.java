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

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nmj.abstractclasses.MovieApiService;
import com.nmj.functions.Actor;
import com.nmj.functions.CoverItem;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.utils.IntentUtils;
import com.nmj.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ActorBrowserFragment extends Fragment {

    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor;
    private ArrayList<Actor> mCast = new ArrayList<Actor>();
    private ArrayList<Actor> mCrew = new ArrayList<Actor>();
    private String loadType;
    private GridView mGridView = null;
    private ProgressBar mProgressBar;
    private Picasso mPicasso;
    private Config mConfig;
    private boolean mLoaded = false;
    private ImageAdapter mAdapter;
    private View mEmptyView;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ActorBrowserFragment() {
    }

    public static ActorBrowserFragment newInstance(String movieId, String loadType) {
        ActorBrowserFragment pageFragment = new ActorBrowserFragment();
        Bundle bundle = new Bundle();
        bundle.putString("movieId", movieId);
        bundle.putString("loadType", loadType);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mPicasso = NMJManagerApplication.getPicasso(getActivity());
        mConfig = NMJManagerApplication.getBitmapConfig();

        mAdapter = new ImageAdapter(getActivity());
        mToolbarColor = getArguments().getInt(IntentKeys.TOOLBAR_COLOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_grid_fragment, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mEmptyView = v.findViewById(R.id.empty_library_layout);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        mProgressBar.setVisibility(View.GONE);

        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int numColumns = (int) Math.floor(
                                mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                        if (numColumns > 0) {
                            mGridView.setNumColumns(numColumns);
                        }
                    }
                });
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), arg1.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(getActivity(), IntentUtils.getActorIntent(getActivity(), mCast.get(arg2), mToolbarColor), options.toBundle());
            }
        });

        // "No actors" message
        TextView title = (TextView) v.findViewById(R.id.empty_library_title);
        title.setText(R.string.no_actors);

        TextView description = (TextView) v.findViewById(R.id.empty_library_description);
        description.setText(R.string.no_actors_description);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadType = getArguments().getString("loadType");
        if (mCast.size() == 0)
            new GetPeopleDetails(getArguments().getString("movieId"), getActivity()).execute();
    }

    private class ImageAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private final Context mContext;

        public ImageAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mCast.size();
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0 && mLoaded;
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
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            CoverItem holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.grid_cover_two_line, container, false);
                holder = new CoverItem();

                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.hasWatched = (ImageView) convertView.findViewById(R.id.hasWatched);
                holder.inLibrary = (ImageView) convertView.findViewById(R.id.inLibrary);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.text.setSingleLine(true);
                holder.subtext = (TextView) convertView.findViewById(R.id.sub_text);
                holder.subtext.setSingleLine(true);

                holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            holder.cover.setImageResource(R.color.card_background_dark);
            holder.hasWatched.setVisibility(View.GONE);
            holder.inLibrary.setVisibility(View.GONE);
            holder.text.setText(mCast.get(position).getName());
            holder.subtext.setText(mCast.get(position).getCharacter());

            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
            if (!mCast.get(position).getUrl().endsWith("null"))
                mPicasso.load(mCast.get(position).getUrl()).error(R.drawable.noactor).config(mConfig).into(holder);
            else
                holder.cover.setImageResource(R.drawable.noactor);

            return convertView;
        }
    }

    protected class GetPeopleDetails extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final String mMovieId;

        public GetPeopleDetails(String movieId, Context context) {
            mMovieId = movieId;
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MovieApiService service = NMJManagerApplication.getMovieService(mContext);
            if (loadType.equals("cast"))
                mCast = new ArrayList<Actor>(NMJLib.getTMDbCast(mContext, loadType, mMovieId, "en"));
            else
                mCast = new ArrayList<Actor>(NMJLib.getTMDbCrew(mContext, loadType, mMovieId, "en"));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isAdded()) {
                mProgressBar.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();

                mEmptyView.setVisibility((mCast.size() == 0) ? View.VISIBLE : View.GONE);
            }
        }
    }
}
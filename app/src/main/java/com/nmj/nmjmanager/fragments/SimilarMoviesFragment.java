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
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.nmj.functions.CoverItem;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJLib;
import com.nmj.functions.WebMovie;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.utils.IntentUtils;
import com.nmj.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SimilarMoviesFragment extends Fragment {

	private Context mContext;
	private int mImageThumbSize, mImageThumbSpacing, mToolbarColor;
	private ImageAdapter mAdapter;
	private GridView mGridView;
	private Picasso mPicasso;
	private Config mConfig;
	private ProgressBar mProgressBar;
	private String mTmdbId, mLoadType, mVideoType;
	private List<WebMovie> mSimilarMovies = new ArrayList<>();
	private boolean mChecked = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public SimilarMoviesFragment() {}

	public static SimilarMoviesFragment newInstance(String tmdbId, String loadType, String videoType) {
		SimilarMoviesFragment pageFragment = new SimilarMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("tmdbId", tmdbId);
		bundle.putString("loadType", loadType);
		bundle.putString("videoType", videoType);

		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		mContext = getActivity();

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = NMJManagerApplication.getPicasso(getActivity());
		mConfig = NMJManagerApplication.getBitmapConfig();

		mTmdbId = getArguments().getString("tmdbId");
		mLoadType = getArguments().getString("loadType");
		mVideoType = getArguments().getString("videoType");
		mToolbarColor = getArguments().getInt(IntentKeys.TOOLBAR_COLOR);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.switch_button, menu);

		int padding = NMJLib.convertDpToPixels(getActivity(), 16);

		SwitchCompat switchCompat = (SwitchCompat) menu.findItem(R.id.switch_button).getActionView();
		switchCompat.setChecked(mChecked);
		switchCompat.setText(R.string.inLibrary);
		switchCompat.setSwitchPadding(padding);
		switchCompat.setPadding(0, 0, padding, 0);

		switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mChecked = isChecked;
				mAdapter.notifyDataSetChanged();
			}
		});

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = v.findViewById(R.id.progress);
		mGridView = v.findViewById(R.id.gridView);

		// Calculate the total column width to set item heights by factor 1.5
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
				if (mVideoType.equals("movie"))
					ActivityCompat.startActivity(getActivity(), IntentUtils.getTmdbMovieDetails(mContext, mAdapter.getItem(arg2), mToolbarColor), options.toBundle());
				else
					ActivityCompat.startActivity(getActivity(), IntentUtils.getTmdbShowDetails(mContext, mAdapter.getItem(arg2), mToolbarColor), options.toBundle());

			}
		});

		new MovieLoader(mContext, mTmdbId).execute();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private ArrayList<WebMovie> mMovies;
		private final LayoutInflater mInflater;

		public ImageAdapter(Context context) {
			mContext = context;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mMovies.size();
		}

		@Override
		public WebMovie getItem(int position) {
			return mMovies.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			final WebMovie movie = getItem(position);

			CoverItem holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_cover_two_line, container, false);
				holder = new CoverItem();

				holder.cover = convertView.findViewById(R.id.cover);
				holder.hasWatched = convertView.findViewById(R.id.hasWatched);
				holder.inLibrary = convertView.findViewById(R.id.inLibrary);
				holder.text = convertView.findViewById(R.id.text);
				holder.text.setSingleLine(true);
				holder.subtext = convertView.findViewById(R.id.sub_text);
				holder.subtext.setSingleLine(true);

				holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}
			holder.inLibrary.setVisibility(View.GONE);
			holder.hasWatched.setVisibility(View.GONE);

			holder.cover.setImageResource(R.color.card_background_dark);

			holder.text.setText(movie.getTitle());
			holder.subtext.setText(movie.getSubtitle());

			if(NMJManagerApplication.getNMJAdapter().movieExistsbyId("tmdb" + movie.getId()))
				holder.inLibrary.setVisibility(View.VISIBLE);

			if(NMJManagerApplication.getNMJAdapter().hasWatched(movie.getId()))
				holder.hasWatched.setVisibility(View.VISIBLE);

			if (!mSimilarMovies.get(position).getUrl().contains("null"))
				mPicasso.load(movie.getUrl()).error(R.drawable.loading_image).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}

		@Override
		public void notifyDataSetChanged() {

			// Initialize
			mMovies = new ArrayList<>();

			// Go through all movies
			for (WebMovie movie : mSimilarMovies) {
				if (mChecked && !movie.isInLibrary())
					continue;
				mMovies.add(movie);
			}

			super.notifyDataSetChanged();
		}
	}

	private class MovieLoader extends AsyncTask<Void, Void, Void> {

		private final Context mContext;
		private final String mTmdbId;

		public MovieLoader(Context context, String tmdbId) {
			mContext = context;
			mTmdbId = tmdbId;
		}

		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			mSimilarMovies = NMJLib.getTMDbMovies(mContext, mVideoType, mLoadType, mTmdbId, "en");

			for (int i = 0; i < mSimilarMovies.size(); i++) {
				String id = "tmdb" + mSimilarMovies.get(i).getId();
				mSimilarMovies.get(i).setInLibrary(NMJManagerApplication.getNMJAdapter().movieExistsbyId(id));
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);

				mAdapter = new ImageAdapter(getActivity());
				mGridView.setAdapter(mAdapter);
			}
		}
	}
}
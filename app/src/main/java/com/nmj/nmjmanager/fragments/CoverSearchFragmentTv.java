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
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nmj.abstractclasses.TvShowApiService;
import com.nmj.apis.thetvdb.TheTVDbService;
import com.nmj.apis.tmdb.TMDbTvShowService;
import com.nmj.functions.AsyncTask;
import com.nmj.functions.CoverItem;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.service.DownloadImageService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CoverSearchFragmentTv extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<String> mImages = new ArrayList<String>();
	private GridView mGridView = null;
	private String mShowId;
	private ProgressBar mProgressBar;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public CoverSearchFragmentTv() {}

	public static CoverSearchFragmentTv newInstance(String showId) {
		CoverSearchFragmentTv pageFragment = new CoverSearchFragmentTv();
		Bundle b = new Bundle();
		b.putString("showId", showId);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mShowId = getArguments().getString("showId");

		mPicasso = NMJManagerApplication.getPicasso(getActivity());
		mConfig = NMJManagerApplication.getBitmapConfig();

		new GetCoverImages().execute(mShowId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mImages.size() > 0) mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mGridView = (GridView) v.findViewById(R.id.gridView);

		mAdapter = new ImageAdapter(getActivity());
		mGridView.setAdapter(mAdapter);

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
				// Create the download Service
				Intent downloadService = new Intent(getActivity(), DownloadImageService.class);
				downloadService.putExtra(DownloadImageService.CONTENT_ID, mShowId);
				downloadService.putExtra(DownloadImageService.IMAGE_URL, mImages.get(arg2));
				downloadService.putExtra(DownloadImageService.IMAGE_TYPE, DownloadImageService.IMAGE_TYPE_TVSHOW_COVER);				
				getActivity().startService(downloadService);
				
				// End the browser Activity
				getActivity().finish();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private LayoutInflater inflater;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mImages.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_portrait_photo, container, false);
				holder = new CoverItem();

				holder.cover = (ImageView) convertView.findViewById(R.id.cover);

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(mImages.get(position)).placeholder(R.color.card_background_dark).config(mConfig).into(holder.cover);

			return convertView;
		}
	}

	protected class GetCoverImages extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			String id = params[0];

			TvShowApiService service = null;
			if (id.startsWith("tmdb_")) {
				id = id.replace("tmdb_", "");
				service = TMDbTvShowService.getInstance(getActivity());
			} else {
				service = TheTVDbService.getInstance(getActivity());
			}

			mImages.addAll(service.getCovers(id));

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			getActivity().onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
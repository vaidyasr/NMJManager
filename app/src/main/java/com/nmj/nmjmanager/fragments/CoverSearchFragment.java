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
import android.widget.ProgressBar;

import com.nmj.functions.Cover;
import com.nmj.functions.CoverItem;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.service.DownloadImageService;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class CoverSearchFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<Cover> mCovers = new ArrayList<Cover>();
	private ArrayList<String> mImageUrls = new ArrayList<String>();
	private GridView mGridView = null;
	private String mTmdbId, mJson;
	private String[] mItems = new String[]{};
	private ProgressBar mProgressBar;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public CoverSearchFragment() {}

	public static CoverSearchFragment newInstance(String tmdbId, String json, String baseUrl) {
		CoverSearchFragment pageFragment = new CoverSearchFragment();
		Bundle b = new Bundle();
		b.putString("tmdbId", tmdbId);
		b.putString("json", json);
		b.putString("baseUrl", baseUrl);
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

		mPicasso = NMJManagerApplication.getPicasso(getActivity());
		mConfig = NMJManagerApplication.getBitmapConfig();

		mTmdbId = getArguments().getString("tmdbId");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		for (int i = 0; i < mItems.length; i++)
			menu.add(0, i, i, mItems[i]);
		menu.setGroupCheckable(0, true, true);

		super.onCreateOptionsMenu(menu, inflater);
	}	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mImageUrls.size() > 0) mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

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
				downloadService.putExtra(DownloadImageService.CONTENT_ID, mTmdbId);
				downloadService.putExtra(DownloadImageService.IMAGE_URL, mImageUrls.get(arg2));
				downloadService.putExtra(DownloadImageService.IMAGE_TYPE, DownloadImageService.IMAGE_TYPE_MOVIE_COVER);				
				getActivity().startService(downloadService);
				
				// End the browser Activity
				getActivity().finish();
			}
		});

		mJson = getArguments().getString("json");
		loadJson(getArguments().getString("baseUrl"));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
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
			return mImageUrls.size();
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
			mPicasso.load(mImageUrls.get(position)).placeholder(R.color.card_background_dark).config(mConfig).into(holder.cover);

			return convertView;
		}
	}

	private void loadJson(String baseUrl) {
		try {
			mCovers.clear();
			mImageUrls.clear();

			JSONObject jObject = new JSONObject(mJson);

			JSONArray array = jObject.getJSONArray("posters");

			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);

				mCovers.add(new Cover(baseUrl + NMJLib.getImageUrlSize(getActivity()) + o.getString("file_path"), o.getString("iso_639_1")));
				mImageUrls.add(baseUrl + NMJLib.getImageUrlSize(getActivity()) + o.getString("file_path"));
			}
			
		} catch (Exception e) {}

		if (isAdded()) {
			showContent();
		}
	}

	private void showContent() {
		mProgressBar.setVisibility(View.GONE);
		mAdapter.notifyDataSetChanged();

		TreeSet<String> languages = new TreeSet<String>();
		for (int i = 0; i < mCovers.size(); i++)
			if (!mCovers.get(i).getLanguage().equals("Null"))
				languages.add(mCovers.get(i).getLanguage());

		mItems = new String[languages.size() + 1];
		mItems[0] = getString(R.string.stringShowAllLanguages);
		Iterator<String> itr = languages.iterator();
		int i = 1;
		while (itr.hasNext()) {
			mItems[i] = itr.next();
			i++;
		}

		getActivity().invalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			getActivity().onBackPressed();
			return true;
		}

		item.setChecked(true);

		if (item.getItemId() == 0) {
			mImageUrls.clear();
			for (int i = 0; i < mCovers.size(); i++)
				mImageUrls.add(mCovers.get(i).getUrl());
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		} else {
			mImageUrls.clear();
			for (int i = 0; i < mCovers.size(); i++)
				if (mCovers.get(i).getLanguage().equals(mItems[item.getItemId()]))
					mImageUrls.add(mCovers.get(i).getUrl());
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		}

		return super.onOptionsItemSelected(item);
	}
}
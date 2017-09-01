package com.nmj.nmjmanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.nmjmanager.fragments.SimilarMoviesFragment;
import com.nmj.utils.ViewUtils;

public class SimilarMovies extends NMJActivity {

	private static String TAG = "SimilarMoviesFragment";
    private int mToolbarColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String movieId = getIntent().getExtras().getString("movieId");
		String title = getIntent().getExtras().getString("title");
		String loadType = getIntent().getExtras().getString("loadType");
		String videoType = getIntent().getExtras().getString("videoType");

		mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);
		
		getSupportActionBar().setSubtitle(title);

		if (loadType.equals("similar")) {
			if (videoType.equals("movie"))
				getSupportActionBar().setTitle(R.string.relatedMovies);
			else
				getSupportActionBar().setTitle(R.string.relatedShows);
		}else {
			getSupportActionBar().setTitle(R.string.recommended);
		}

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null && savedInstanceState == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content, SimilarMoviesFragment.newInstance(movieId, loadType, videoType), TAG);
			ft.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
	}
	
	@Override
	protected int getLayoutResource() {
        return R.layout.empty_layout_with_toolbar;
	}
}

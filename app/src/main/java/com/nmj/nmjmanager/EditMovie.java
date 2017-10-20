package com.nmj.nmjmanager;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.nmjmanager.fragments.EditMovieFragment;
import com.nmj.utils.ViewUtils;

public class EditMovie extends NMJActivity {

    private int mToolbarColor;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String movieId = getIntent().getStringExtra("showId");
		mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, EditMovieFragment.newInstance(movieId)).commit();
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
		return 0;
	}
}

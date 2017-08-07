package com.nmj.nmjmanager;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.nmjmanager.fragments.EditTvShowFragment;
import com.nmj.utils.ViewUtils;

public class EditTvShow extends NMJActivity {

    private int mToolbarColor;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String showId = getIntent().getStringExtra("showId");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, EditTvShowFragment.newInstance(showId)).commit();
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

package com.nmj.nmjmanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.nmj.base.NMJActivity;
import com.nmj.functions.IntentKeys;
import com.nmj.nmjmanager.fragments.ActorBrowserTvFragment;
import com.nmj.utils.ViewUtils;

public class ActorBrowserTv extends NMJActivity {

	private static String TAG = "ActorBrowserTvFragment";
    private int mToolbarColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String showId = getIntent().getExtras().getString("showId");
		String title = getIntent().getExtras().getString("title");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);
		
		getSupportActionBar().setSubtitle(title);

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null && savedInstanceState == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content, ActorBrowserTvFragment.newInstance(showId), TAG);
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

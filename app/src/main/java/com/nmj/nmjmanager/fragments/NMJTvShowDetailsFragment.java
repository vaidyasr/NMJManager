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

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.nmj.apis.nmj.TvShow;
import com.nmj.apis.nmj.NMJMovieService;
import com.nmj.apis.tmdb.TMDbTvShowService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.base.NMJActivity;
import com.nmj.db.DbAdapterTvShowEpisodeMappings;
import com.nmj.db.DbAdapterTvShowEpisodes;
import com.nmj.db.DbAdapterTvShows;
import com.nmj.functions.Actor;
import com.nmj.functions.BlurTransformation;
import com.nmj.functions.EpisodeCounter;
import com.nmj.functions.FileSource;
import com.nmj.functions.Filepath;
import com.nmj.functions.GridSeason;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJLib;
import com.nmj.functions.PaletteLoader;
import com.nmj.functions.PreferenceKeys;
import com.nmj.functions.SimpleAnimatorListener;
import com.nmj.loader.TvShowLoader;
import com.nmj.nmjmanager.EditTvShow;
import com.nmj.nmjmanager.IdentifyTvShow;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.NMJTvShowDetails;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.ShowCoverFanartBrowser;
import com.nmj.nmjmanager.TvShowEpisode;
import com.nmj.utils.FileUtils;
import com.nmj.utils.IntentUtils;
import com.nmj.utils.LocalBroadcastUtils;
import com.nmj.utils.TypefaceUtils;
import com.nmj.utils.VideoUtils;
import com.nmj.utils.ViewUtils;
import com.nmj.views.HorizontalCardLayout;
import com.nmj.views.ObservableScrollView;
import com.nmj.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.nmj.apis.nmj.NMJTvShowService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.nmj.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

public class NMJTvShowDetailsFragment extends Fragment {

    private Activity mContext;
    private TvShow mShow;
    private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textCertification;
    private ImageView background, cover, mHasWatched, mInLibrary;
    private ObservableScrollView mScrollView;
    private View mDetailsArea;
    private boolean ignorePrefixes;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mBold, mCondensedRegular;
    private Bus mBus;
    private HorizontalCardLayout mSeasonsLayout, mCastLayout, mCrewLayout;
    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor = 0;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private NMJTvShowService mShowApiService;
    private PaletteLoader mPaletteLoader;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadImages();
        }
    };

    /**
     * Empty constructor as per the Fragment documentation
     */
    public NMJTvShowDetailsFragment() {
    }

    public static NMJTvShowDetailsFragment newInstance(String movieId, String showId) {
        NMJTvShowDetailsFragment pageFragment = new NMJTvShowDetailsFragment();
        Bundle b = new Bundle();
        b.putString("showId", showId);
        b.putString("movieId", movieId);
        pageFragment.setArguments(b);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mContext = getActivity();

        mBus = NMJManagerApplication.getBus();

        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_TITLE_PREFIXES, false);

        // Create and open database
        mShowApiService = NMJTvShowService.getInstance(mContext);

        // Get the database ID of the movie in question
        mShow = new TvShow();
        System.out.println("Movie Id: " + getArguments().getString("movieId"));
        System.out.println("Show Id: " + getArguments().getString("showId"));
        mShow.setShowId(getArguments().getString("showId"));
        mShow.setId(getArguments().getString("movieId"));

        mPicasso = NMJManagerApplication.getPicassoDetailsView(getActivity());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
    }

    @Override
    public void onResume() {
        super.onResume();

        mBus.register(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
    }

    public void onViewCreated(final View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        ((NMJActivity) getActivity()).setSupportActionBar(mToolbar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This needs to be re-initialized here and not in onCreate()
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mDetailsArea = v.findViewById(R.id.details_area);

        background = (ImageView) v.findViewById(R.id.imageBackground);
        textTitle = (TextView) v.findViewById(R.id.movieTitle);
        textPlot = (TextView) v.findViewById(R.id.textView2);
        textGenre = (TextView) v.findViewById(R.id.textView7);
        textRuntime = (TextView) v.findViewById(R.id.textView9);
        textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
        textRating = (TextView) v.findViewById(R.id.textView12);
        textCertification = (TextView) v.findViewById(R.id.textView11);
        cover = (ImageView) v.findViewById(R.id.traktIcon);
        mSeasonsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout);
        mCastLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra);
        mCastLayout.setVisibility(View.VISIBLE);
        mCrewLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra_1);
        mCrewLayout.setVisibility(View.VISIBLE);
        mScrollView = (ObservableScrollView) v.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) v.findViewById(R.id.fab);
        mHasWatched = (ImageView) v.findViewById(R.id.hasWatched);
        mInLibrary = (ImageView) v.findViewById(R.id.inLibrary);

        if (mShow.getShowId().equals("0"))
            mFab.setVisibility(View.INVISIBLE);
        else
            mFab.setVisibility(View.VISIBLE);

        mInLibrary.setVisibility(View.GONE);
        mHasWatched.setVisibility(View.GONE);

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtils.animateFabJump(v, new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        playFirstEpisode();
                    }
                });
            }
        });
        if (NMJLib.isTablet(mContext))
            mFab.setType(FloatingActionButton.TYPE_NORMAL);

        // Get rid of these...
        v.findViewById(R.id.textView3).setVisibility(View.GONE); // File
        v.findViewById(R.id.textView6).setVisibility(View.GONE); // Tagline

        final int height = NMJLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                ViewUtils.handleOnScrollChangedEvent(
                        getActivity(), v, background, mShow.getTitle(),
                        height, t, mToolbar, mToolbarColor);
            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, v,
                        background, mScrollView, this);
            }
        });
        if (mShow.getVideo().size() > 0)
            if (!mShow.getVideo().get(0).getPlayCount().equals("0"))
                mHasWatched.setVisibility(View.VISIBLE);

        new TvShowLoader().execute();

    }

    private void setupFields() {
        System.out.println("Setting up fields...");

        // Set the show title
        textTitle.setVisibility(View.VISIBLE);
        textTitle.setText(mShow.getTitle());
        textTitle.setTypeface(mCondensedRegular);

        textPlot.setTypeface(mCondensedRegular);
        textRuntime.setTypeface(mMedium);
        textRating.setTypeface(mMedium);
        textCertification.setTypeface(mMedium);

        textRuntime.setTypeface(mMedium);
        textCertification.setTypeface(mMedium);
        textRating.setTypeface(mMedium);

        // Set the show plot
        textPlot.setBackgroundResource(R.drawable.selectable_background);
        textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
        textPlot.setTag(true); // true = collapsed
        textPlot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Boolean) textPlot.getTag())) {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(textPlot, 50); // It seems highly unlikely that there would every be more than 50 lines

                    // Reverse the tag
                    textPlot.setTag(false);
                } else {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(textPlot, getResources().getInteger(R.integer.show_details_max_lines));

                    // Reverse the tag
                    textPlot.setTag(true);
                }
            }
        });
        textPlot.setEllipsize(TextUtils.TruncateAt.END);
        textPlot.setFocusable(true);

        if (NMJLib.isTablet(getActivity()))
            textPlot.setLineSpacing(0, 1.15f);

        textPlot.setText(mShow.getPlot());

        // Set the show genres
        textGenre.setTypeface(mMediumItalic);
        if (!TextUtils.isEmpty(mShow.getGenres())) {
            textGenre.setText(mShow.getGenres());
        } else {
            textGenre.setVisibility(View.GONE);
        }

        // Set the show runtime
        if (mShow.getShowId().equals("0"))
            textRuntime.setText(NMJLib.getPrettyRuntimeFromMinutes(getActivity(), Integer.parseInt(mShow.getRuntime())));
        else
            textRuntime.setText(NMJLib.getPrettyRuntimeFromSeconds(getActivity(), Integer.parseInt(mShow.getRuntime())));

        // Set the show release date
        textReleaseDate.setTypeface(mMedium);
        textReleaseDate.setText(NMJLib.getPrettyDate(getActivity(), mShow.getFirstAirdate()));

        // Set the show rating
        if (!mShow.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mShow.getRating()) * 10);
                textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
            } catch (NumberFormatException e) {
                textRating.setText(mShow.getRating());
            }
        } else {
            textRating.setText(R.string.stringNA);
        }

        // Set the show certification
        if (!TextUtils.isEmpty(mShow.getCertification())) {
            textCertification.setText(mShow.getCertification());
        } else {
            textCertification.setText(R.string.stringNA);
        }

        mSeasonsLayout.setTitle(R.string.seasons);
        mSeasonsLayout.setSeeMoreVisibility(true);
        mSeasonsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mSeasonsLayout.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mSeasonsLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mSeasonsLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;
                            loadSeasons(numColumns);
                            NMJLib.removeViewTreeObserver(mSeasonsLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mSeasonsLayout.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getTvShowSeasonsIntent(mContext, mShow.getTitle(), mShow.getId(), mToolbarColor));
            }
        });

/*
        mCastLayout.setTitle(R.string.detailsActors);
        mCastLayout.setSeeMoreVisibility(true);
        mCastLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mCastLayout.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mCastLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mCastLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                            loadActors(numColumns);
                            NMJLib.removeViewTreeObserver(mCastLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mCastLayout.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorBrowserTvShows(mContext, mShow.getTitle(), mShow.getTmdbId(), mToolbarColor, "cast"));
            }
        });

        mCrewLayout.setTitle(R.string.detailsActors);
        mCrewLayout.setSeeMoreVisibility(true);
        mCrewLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mCrewLayout.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mCrewLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mCrewLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                            loadActors(numColumns);
                            NMJLib.removeViewTreeObserver(mCrewLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mCrewLayout.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorBrowserTvShows(mContext, mShow.getTitle(), mShow.getTmdbId(), mToolbarColor, "cast"));
            }
        });
*/

        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mShow.getTitle(), mToolbarColor);

        loadImages();
    }

    private void loadImages() {
        mPicasso.load(mShow.getPoster()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.parse(mShow.getPoster().toString()), new PaletteLoader.OnPaletteLoadedCallback() {
                        @Override
                        public void onPaletteLoaded(int swatchColor) {
                            mToolbarColor = swatchColor;
                            ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mShow.getTitle(), mToolbarColor);
                        }
                    });

                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mCastLayout.getSeeMoreView());
                    mPaletteLoader.addView(mCrewLayout.getSeeMoreView());
                    mPaletteLoader.addView(mSeasonsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    mPaletteLoader.execute();
                } else {
                    // Clear old views after configuration change
                    mPaletteLoader.clearViews();

                    // Add views after configuration change
                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mCastLayout.getSeeMoreView());
                    mPaletteLoader.addView(mCrewLayout.getSeeMoreView());
                    mPaletteLoader.addView(mSeasonsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    // Re-color the views
                    mPaletteLoader.colorViews();
                }
            }

            @Override
            public void onError() {
            }
        });

        if (!NMJLib.isPortrait(getActivity())) {
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PreferenceKeys.BLUR_BACKDROPS, false)) {
                mPicasso.load(mShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).transform(new BlurTransformation(mContext, mShow.getBackdrop(), 8)).into(background);
            } else {
                mPicasso.load(mShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
            }
        } else {
            if (!mShow.getBackdrop().isEmpty())
            mPicasso.load(mShow.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
                @Override
                public void onError() {
                    if (!isAdded())
                        return;

                    mPicasso.load(mShow.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
                }

                @Override
                public void onSuccess() {
                }
            });
        }
    }

    private void loadActors(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<Actor> mActors;

            @Override
            protected Void doInBackground(Void... params) {
                NMJTvShowService service = NMJTvShowService.getInstance(mContext);
                mActors = service.getCast(mShow.getId());

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mCastLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
                mCrewLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
            }
        }.execute();
    }

    private void loadSeasons(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<GridSeason> mSeasons = new ArrayList<GridSeason>();

            @Override
            protected Void doInBackground(Void... params) {
                NMJManagerApplication.getNMJAdapter().getEpisodes(mContext, Integer.parseInt(mShow.getId()));

/*                HashMap<String, EpisodeCounter> seasons = NMJManagerApplication.getNMJAdapter().getSeasons(mContext, Integer.parseInt(mShow.getId());

                for (String key : seasons.keySet()) {
                    File temp = FileUtils.getTvShowSeason(mContext, mShow.getId(), key);
                    mSeasons.add(new GridSeason(mContext, mShow.getId(), Integer.valueOf(key), seasons.get(key).getEpisodeCount(), seasons.get(key).getWatchedCount(),
                            temp.exists() ? temp :
                                    FileUtils.getTvShowThumb(mContext, mShow.getId())));
                }

                seasons.clear();*/

                Collections.sort(mSeasons);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mSeasonsLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mSeasons, HorizontalCardLayout.SEASONS, mToolbarColor);
                mSeasonsLayout.setSeeMoreVisibility(true);
            }
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tv_show_details, menu);

        // If this is a tablet, we have more room to display icons
        if (NMJLib.isTablet(mContext))
            menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        // Favourite
        menu.findItem(R.id.show_fav).setIcon(mShow.isFavorite() ?
                R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                .setTitle(mShow.isFavorite() ?
                        R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_fav:
                favAction();
                break;
            case R.id.menuDeleteShow:
                deleteShow();
                break;
            case R.id.change_cover:
                searchCover();
                break;
            case R.id.identify_show:
                identifyShow();
                break;
            case R.id.editTvShow:
                editTvShow();
                break;
            case R.id.openInBrowser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                if (mShow.getIdType() == TvShow.TMDB) {
                    browserIntent.setData(Uri.parse("https://www.themoviedb.org/tv/" + mShow.getIdWithoutHack()));
                } else {
                    browserIntent.setData(Uri.parse("http://thetvdb.com/?tab=series&id=" + mShow.getId()));
                }
                startActivity(browserIntent);
                break;
            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, ((mShow.getIdType() == TvShow.THETVDB) ? "http://thetvdb.com/?tab=series&id=" : "http://www.themoviedb.org/tv/") + mShow.getIdWithoutHack());
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void identifyShow() {
        ArrayList<String> files = new ArrayList<String>();

        Cursor cursor = NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().getAllFilepaths(mShow.getId());
        while (cursor.moveToNext())
            files.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));

        cursor.close();

        Intent i = new Intent();
        i.setClass(mContext, IdentifyTvShow.class);
        i.putExtra("showTitle", mShow.getTitle());
        i.putExtra("showId", mShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(i, 0);
    }

    private void editTvShow() {
        Intent intent = new Intent(mContext, EditTvShow.class);
        intent.putExtra("showId", mShow.getId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(intent, 1);
    }

    public void favAction() {
        // Create and open database
        mShow.setFavorite(!mShow.isFavorite()); // Reverse the favourite boolean

/*        if (dbHelper.updateShowSingleItem(mShow.getTmdbId(), DbAdapterTvShows.KEY_SHOW_FAVOURITE, mShow.getFavorite())) {
            getActivity().invalidateOptionsMenu();

            Toast.makeText(mContext, getString(mShow.isFavorite() ? R.string.addedToFavs : R.string.removedFromFavs), Toast.LENGTH_SHORT).show();

            LocalBroadcastUtils.updateTvShowLibrary(mContext);

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();*/

        new Thread() {
            @Override
            public void run() {
                ArrayList<TvShow> show = new ArrayList<TvShow>();
                show.add(mShow);
                //Trakt.tvShowFavorite(show, getActivity().getApplicationContext());
            }
        }.start();
    }

    private void searchCover() {
        Intent i = new Intent();
        i.setClass(mContext, ShowCoverFanartBrowser.class);
        i.putExtra("id", mShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivity(i);
    }

    private void deleteShow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(getString(R.string.areYouSure))
                .setTitle(getString(R.string.removeShow))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //NMJLib.deleteShow(getActivity().getApplicationContext(), mShow, true);
                        LocalBroadcastUtils.updateTvShowLibrary(getActivity().getApplicationContext());
                        getActivity().finish();
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create().show();
    }

    private void playFirstEpisode() {

        DbAdapterTvShowEpisodes dbAdapter = NMJManagerApplication.getTvEpisodeDbAdapter();
        Cursor cursor = dbAdapter.getEpisodes(mShow.getId());
        TvShowEpisode episode = null;

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {

                    // We want to avoid specials
                    if (NMJLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON))) > 0) {

                        // Set the initial episode as a fallback if all episodes have been watched
                        if (episode == null) {
                            episode = new TvShowEpisode(getActivity(),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                            );

                            episode.setFilepaths(NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));
                        }

                        // Check if the episode has been watched - if not, add
                        // it as our episode to watch, and break the while loop
                        if (cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)).equals("0")) {
                            episode = new TvShowEpisode(getActivity(),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                            );

                            episode.setFilepaths(NMJManagerApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));

                            break;
                        }
                    }

                }
            } catch (Exception e) {
            } finally {
                cursor.close();
            }

            if (episode != null) {
                play(episode);
                Toast.makeText(mContext, String.format(mContext.getString(R.string.playing_season_episode),
                        mShow.getTitle(), episode.getSeason(), episode.getEpisode(), episode.getTitle()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, R.string.no_episodes_to_play, Toast.LENGTH_SHORT).show();
            }
        }
        ;

    }

    private void play(final TvShowEpisode episode) {
        ArrayList<Filepath> paths = episode.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (episode.hasOfflineCopy(path)) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode);
                if (playbackStarted) {
                    checkIn(episode);
                }
            } else {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode);
                if (playbackStarted) {
                    checkIn(episode);
                }
            }
        } else {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (episode.hasOfflineCopy(path)) {
                    boolean playbackStarted = VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode);
                    if (playbackStarted) {
                        checkIn(episode);
                    }

                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                NMJLib.showSelectFileDialog(getActivity(), episode.getFilepaths(), new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Filepath path = episode.getFilepaths().get(which);
                        boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode);
                        if (playbackStarted) {
                            checkIn(episode);
                        }
                    }
                });
            }
        }
    }

    private void checkIn(final TvShowEpisode episode) {
        new Thread() {
            @Override
            public void run() {
                Trakt.performEpisodeCheckin(episode, getActivity());
            }
        }.start();
    }

    private class TvShowLoader extends AsyncTask<String, Object, Object> {
        @Override
        protected Object doInBackground(String... params) {
            if (mShow.getShowId().equals("0")) {
                if (mShow.getIdType() == 1) {
                    mShow = mShowApiService.getCompleteTMDbTvShow(mShow.getId(), "en");
                } else {
                    mShow = mShowApiService.getCompleteTVDbTvShow(mShow.getId(), "en");
                }
            }
            else
                mShow = mShowApiService.getCompleteNMJTvShow(mShow.getShowId());
            for (int i = 0; i < mShow.getSimilarShows().size(); i++) {
                String id = mShow.getSimilarShows().get(i).getId();
                mShow.getSimilarShows().get(i).setInLibrary(NMJManagerApplication.getMovieAdapter().movieExists(id));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            getActivity().invalidateOptionsMenu();
            System.out.println("Post execute");

            setupFields();
        }
    }
}
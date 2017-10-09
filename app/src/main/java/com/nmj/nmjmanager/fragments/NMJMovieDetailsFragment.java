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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.nmj.abstractclasses.MovieApiService;
import com.nmj.apis.nmj.Movie;
import com.nmj.apis.nmj.NMJMovieService;
import com.nmj.apis.tmdb.TMDbMovieService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.base.NMJActivity;
import com.nmj.functions.Actor;
import com.nmj.functions.FileSource;
import com.nmj.functions.Filepath;
import com.nmj.functions.IntentKeys;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJLib;
import com.nmj.functions.Video;
import com.nmj.functions.PaletteLoader;
import com.nmj.functions.SimpleAnimatorListener;
import com.nmj.functions.TmdbTrailerSearch;
import com.nmj.functions.WebMovie;
import com.nmj.nmjmanager.EditMovie;
import com.nmj.nmjmanager.Main;
import com.nmj.nmjmanager.MovieCoverFanartBrowser;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.remoteplayback.RemotePlayback;
import com.nmj.utils.IntentUtils;
import com.nmj.utils.TypefaceUtils;
import com.nmj.utils.VideoUtils;
import com.nmj.utils.ViewUtils;
import com.nmj.views.HorizontalCardLayout;
import com.nmj.views.ObservableScrollView;
import com.nmj.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.nmj.functions.PreferenceKeys.ALWAYS_DELETE_FILE;
import static com.nmj.functions.PreferenceKeys.CHROMECAST_BETA_SUPPORT;
import static com.nmj.functions.PreferenceKeys.SHOW_FILE_LOCATION;

public class NMJMovieDetailsFragment extends Fragment {

    private Activity mContext;
    private TextView mTitle, mPlot, mSrc, mGenre, mRuntime, mReleaseDate, mRating, mTagline, mCertification;
    private ImageView mBackground, mCover, mHasWatched, mInLibrary;
    private Movie mMovie;
    private Video mVideo;
    private ObservableScrollView mScrollView;
    private View mProgressBar, mDetailsArea;
    private boolean mRetained = false, mShowFileLocation;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mBold, mCondensedRegular;
    private NMJMovieService mNMJMovieApiService;
    private TMDbMovieService mTMDbMovieApiService;
    private HorizontalCardLayout mCastLayout, mCrewLayout, mSimilarMoviesLayout, mRecommendedMoviesLayout;
    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor = 0;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private PaletteLoader mPaletteLoader;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public NMJMovieDetailsFragment() {
    }

    public static NMJMovieDetailsFragment newInstance(String movieId, String showId) {
        NMJMovieDetailsFragment pageFragment = new NMJMovieDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("movieId", movieId);
        bundle.putString("showId", showId);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mContext = getActivity();

        mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(SHOW_FILE_LOCATION, true);
        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        mNMJMovieApiService = NMJMovieService.getInstance(mContext);
        mTMDbMovieApiService = TMDbMovieService.getInstance(mContext);

        // Get the database ID of the movie in question
        mMovie = new Movie();
        System.out.println("Movie Id: " + getArguments().getString("movieId"));
        System.out.println("Show Id: " + getArguments().getString("showId"));
        mMovie.setShowId(getArguments().getString("showId"));
        mMovie.setTmdbId(getArguments().getString("movieId"));

        mPicasso = NMJManagerApplication.getPicassoDetailsView(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.movie_and_tv_show_details, container, false);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        ((NMJActivity) getActivity()).setSupportActionBar(mToolbar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This needs to be re-initialized here and not in onCreate()
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mProgressBar = v.findViewById(R.id.progress_layout);
        mDetailsArea = v.findViewById(R.id.details_area);

        mBackground = (ImageView) v.findViewById(R.id.imageBackground);
        mTitle = (TextView) v.findViewById(R.id.movieTitle);
        mPlot = (TextView) v.findViewById(R.id.textView2);
        mSrc = (TextView) v.findViewById(R.id.textView3);
        mGenre = (TextView) v.findViewById(R.id.textView7);
        mRuntime = (TextView) v.findViewById(R.id.textView9);
        mReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
        mRating = (TextView) v.findViewById(R.id.textView12);
        mTagline = (TextView) v.findViewById(R.id.textView6);
        mCertification = (TextView) v.findViewById(R.id.textView11);
        mCover = (ImageView) v.findViewById(R.id.traktIcon);
        mCastLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout);
        mCrewLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra);
        mSimilarMoviesLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra_1);
        mRecommendedMoviesLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra_2);
        mScrollView = (ObservableScrollView) v.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) v.findViewById(R.id.fab);
        mHasWatched = (ImageView) v.findViewById(R.id.hasWatched);
        mInLibrary = (ImageView) v.findViewById(R.id.inLibrary);

        if (mMovie.getShowId().equals("0"))
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
                        //watchTrailer();
                    }
                });
            }
        });
        if (NMJLib.isTablet(mContext))
            mFab.setType(FloatingActionButton.TYPE_NORMAL);

        // Get rid of these...
        //v.findViewById(R.id.textView3).setVisibility(View.GONE); // File

        final int height = NMJLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                ViewUtils.handleOnScrollChangedEvent(
                        getActivity(), v, mBackground, mMovie.getTitle(),
                        height, t, mToolbar, mToolbarColor);
            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, v,
                        mBackground, mScrollView, this);
            }
        });

        if (!mRetained) { // Nothing has been retained - load the data
            setLoading(true);
            new MovieLoader().execute();
            mRetained = true;
        } else {
            setupFields();
        }

        return v;
    }

    private void setupFields() {
        if (isAdded() && mMovie != null) {
            // Set the movie title
            mTitle.setVisibility(View.VISIBLE);
            mTitle.setText(mMovie.getTitle());
            mTitle.setTypeface(mCondensedRegular);

            mPlot.setTypeface(mCondensedRegular);
            mReleaseDate.setTypeface(mMedium);
            mRuntime.setTypeface(mMedium);
            mCertification.setTypeface(mMedium);
            mRating.setTypeface(mMedium);

            // Set the movie plot
            mPlot.setBackgroundResource(R.drawable.selectable_background);
            if (!mMovie.getTagline().isEmpty())
                mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.movie_details_max_lines));
            else
                mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
            mPlot.setTag(true); // true = collapsed
            mPlot.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((Boolean) mPlot.getTag())) {
                        // Animate
                        ViewUtils.animateTextViewMaxLines(mPlot, 50); // It seems highly unlikely that there would every be more than 50 lines

                        // Reverse the tag
                        mPlot.setTag(false);
                    } else {
                        // Animate
                        ViewUtils.animateTextViewMaxLines(mPlot, mMovie.getTagline().isEmpty() ?
                                getResources().getInteger(R.integer.show_details_max_lines) : getResources().getInteger(R.integer.movie_details_max_lines));

                        // Reverse the tag
                        mPlot.setTag(true);
                    }
                }
            });
            mPlot.setEllipsize(TextUtils.TruncateAt.END);
            mPlot.setFocusable(true);

            if (NMJLib.isTablet(getActivity()))
                mPlot.setLineSpacing(0, 1.15f);

            mPlot.setText(mMovie.getPlot());

            // Set the movie file source
            mSrc.setTypeface(mCondensedRegular);
            if (mShowFileLocation) {
                List<String> list = new ArrayList<String>();
                for (int i = 0; i < mMovie.getVideo().size(); i++) {
                    list.add(mMovie.getVideo().get(i).getPath());
                }
                mSrc.setText(TextUtils.join("\n", list));
            } else {
                mSrc.setVisibility(View.GONE);
            }

            // Set movie tag line
            mTagline.setTypeface(mBold);
            if (mMovie.getTagline().equals("NOTAGLINE") || mMovie.getTagline().isEmpty())
                mTagline.setVisibility(TextView.GONE);
            else
                mTagline.setText(mMovie.getTagline());

            // Set the movie genre
            mGenre.setTypeface(mMediumItalic);
            if (!TextUtils.isEmpty(mMovie.getGenres())) {
                mGenre.setText(mMovie.getGenres());
            } else {
                mGenre.setVisibility(View.GONE);
            }

            // Set the movie runtime
            if (mMovie.getShowId().equals("0"))
                mRuntime.setText(NMJLib.getPrettyRuntimeFromMinutes(getActivity(), Integer.parseInt(mMovie.getRuntime())));
            else
                mRuntime.setText(NMJLib.getPrettyRuntimeFromSeconds(getActivity(), Integer.parseInt(mMovie.getRuntime())));

            // Set the movie release date
            mReleaseDate.setTypeface(mMedium);
            mReleaseDate.setText(NMJLib.getPrettyDate(getActivity(), mMovie.getReleasedate()));

            // Set the movie rating
            if (!mMovie.getRating().equals("0.0")) {
                try {
                    int rating = (int) (Double.parseDouble(mMovie.getRating()) * 10);
                    mRating.setText(Html.fromHtml(rating + "<small> %</small>"));
                } catch (NumberFormatException e) {
                    mRating.setText(mMovie.getRating());
                }
            } else {
                mRating.setText(R.string.stringNA);
            }

            // Set the movie certification
            if (!TextUtils.isEmpty(mMovie.getCertification())) {
                mCertification.setText(mMovie.getCertification());
            } else {
                mCertification.setText(R.string.stringNA);
            }

            mCastLayout.setTitle(R.string.detailsCast);
            mCastLayout.setSeeMoreVisibility(true);
            mCastLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mCastLayout.getWidth() > 0) {
                                final int numColumns = (int) Math.floor(mCastLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                                mImageThumbSize = (mCastLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                                loadCast(numColumns);
                                NMJLib.removeViewTreeObserver(mCastLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mCastLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getActorBrowserMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor, "cast"));
                }
            });

            mCrewLayout.setVisibility(View.VISIBLE);
            mCrewLayout.setTitle(R.string.detailsCrew);
            mCrewLayout.setSeeMoreVisibility(true);
            mCrewLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mCrewLayout.getWidth() > 0) {
                                final int numColumns = (int) Math.floor(mCrewLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                                mImageThumbSize = (mCrewLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                                loadCrew(numColumns);
                                NMJLib.removeViewTreeObserver(mCrewLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mCrewLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getActorBrowserMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor, "crew"));
                }
            });

            mSimilarMoviesLayout.setVisibility(View.VISIBLE);
            mSimilarMoviesLayout.setTitle(R.string.relatedMovies);
            mSimilarMoviesLayout.setSeeMoreVisibility(true);
            mSimilarMoviesLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mSimilarMoviesLayout.getWidth() > 0) {
                                final int numColumns = (int) Math.floor(mSimilarMoviesLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                                mImageThumbSize = (mSimilarMoviesLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                                loadSimilarMovies(numColumns);
                                NMJLib.removeViewTreeObserver(mSimilarMoviesLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mSimilarMoviesLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getSimilarMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor));
                }
            });

            mRecommendedMoviesLayout.setVisibility(View.VISIBLE);
            mRecommendedMoviesLayout.setTitle(R.string.recommended);
            mRecommendedMoviesLayout.setSeeMoreVisibility(true);
            mRecommendedMoviesLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mRecommendedMoviesLayout.getWidth() > 0) {
                                final int numColumns = (int) Math.floor(mRecommendedMoviesLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                                mImageThumbSize = (mRecommendedMoviesLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                                loadRecommendedMovies(numColumns);
                                NMJLib.removeViewTreeObserver(mRecommendedMoviesLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mRecommendedMoviesLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getRecommendedMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor));
                }
            });


            ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mMovie.getTitle(), mToolbarColor);

            if(mMovie.getVideo().size() > 0)
                if (!mMovie.getVideo().get(0).getPlayCount().equals("0"))
                    mHasWatched.setVisibility(View.VISIBLE);

            setLoading(false);

            loadImages();
        }
    }

    private void loadCast(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<Actor> mActors = new ArrayList<Actor>();

            @Override
            protected Void doInBackground(Void... params) {
                List<Actor> actors = NMJLib.getTMDbCast(mContext, "movie", mMovie.getTmdbId(), "en");
                for (Integer i = 0; i < actors.size(); i++) {
                    mActors.add(new Actor(
                            actors.get(i).getName(),
                            actors.get(i).getCharacter(),
                            actors.get(i).getId(),
                            "cast",
                            actors.get(i).getUrl()));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mCastLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
            }
        }.execute();
    }

    private void loadCrew(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<Actor> mActors = new ArrayList<Actor>();

            @Override
            protected Void doInBackground(Void... params) {
                List<Actor> actors = NMJLib.getTMDbCrew(mContext, "movie", mMovie.getTmdbId(), "en");
                for (Integer i = 0; i < actors.size(); i++) {
                    mActors.add(new Actor(
                            actors.get(i).getName(),
                            actors.get(i).getCharacter(),
                            actors.get(i).getId(),
                            "crew",
                            actors.get(i).getUrl()));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mCrewLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
            }
        }.execute();
    }

    private void loadSimilarMovies(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<WebMovie> mSimilarMovies = new ArrayList<WebMovie>();

            @Override
            protected Void doInBackground(Void... params) {
                List<WebMovie> similar = NMJLib.getTMDbMovies(mContext, "movie", "similar", mMovie.getTmdbId(), "en");
                for (Integer i = 0; i < similar.size(); i++) {
                    mSimilarMovies.add(new WebMovie(mContext,
                            similar.get(i).getTitle(),
                            similar.get(i).getId(),
                            similar.get(i).getUrl(),
                            similar.get(i).getDate(), ""));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                NMJAdapter adapter = NMJManagerApplication.getNMJAdapter();
                for (int i = 0; i < mSimilarMovies.size(); i++) {
                    String id = "tmdb" + mSimilarMovies.get(i).getId();
                    mSimilarMovies.get(i).setInLibrary(adapter.movieExistsbyId(id));
                }
                mSimilarMoviesLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mSimilarMovies, HorizontalCardLayout.RELATED_MOVIES, mToolbarColor);
            }
        }.execute();
    }

    private void loadRecommendedMovies(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<WebMovie> mSimilarMovies = new ArrayList<WebMovie>();

            @Override
            protected Void doInBackground(Void... params) {
                List<WebMovie> similar = NMJLib.getTMDbMovies(mContext, "movie", "recommendations", mMovie.getTmdbId(), "en");
                for (Integer i = 0; i < similar.size(); i++) {
                    mSimilarMovies.add(new WebMovie(mContext,
                            similar.get(i).getTitle(),
                            similar.get(i).getId(),
                            similar.get(i).getUrl(),
                            similar.get(i).getDate(), ""));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                NMJAdapter adapter = NMJManagerApplication.getNMJAdapter();
                for (int i = 0; i < mSimilarMovies.size(); i++) {
                    String id = "tmdb" + mSimilarMovies.get(i).getId();
                    mSimilarMovies.get(i).setInLibrary(adapter.movieExistsbyId(id));
                }
                mRecommendedMoviesLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mSimilarMovies, HorizontalCardLayout.RELATED_MOVIES, mToolbarColor);
            }
        }.execute();
    }


    private void loadImages() {
        if (!mMovie.getPoster().isEmpty())
            mPicasso.load(mMovie.getPoster()).placeholder(R.drawable.gray).error(R.drawable.loading_image).into(mCover, new Callback() {
                @Override
                public void onSuccess() {
                    if (mPaletteLoader == null) {
                        mPaletteLoader = new PaletteLoader(mPicasso, Uri.parse(mMovie.getPoster()), new PaletteLoader.OnPaletteLoadedCallback() {
                            @Override
                            public void onPaletteLoaded(int swatchColor) {
                                mToolbarColor = swatchColor;
                            }
                        });

                        mPaletteLoader.addView(mDetailsArea);
                        mPaletteLoader.addView(mCastLayout.getSeeMoreView());
                        mPaletteLoader.addView(mCrewLayout.getSeeMoreView());
                        mPaletteLoader.addView(mSimilarMoviesLayout.getSeeMoreView());
                        mPaletteLoader.addView(mRecommendedMoviesLayout.getSeeMoreView());
                        mPaletteLoader.setFab(mFab);

                        mPaletteLoader.execute();
                    } else {
                        // Clear old views after configuration change
                        mPaletteLoader.clearViews();

                        // Add views after configuration change
                        mPaletteLoader.addView(mDetailsArea);
                        mPaletteLoader.addView(mCastLayout.getSeeMoreView());
                        mPaletteLoader.addView(mCrewLayout.getSeeMoreView());
                        mPaletteLoader.addView(mSimilarMoviesLayout.getSeeMoreView());
                        mPaletteLoader.addView(mRecommendedMoviesLayout.getSeeMoreView());
                        mPaletteLoader.setFab(mFab);

                        // Re-color the views
                        mPaletteLoader.colorViews();
                    }
                }

                @Override
                public void onError() {
                }
            });
        else
            mCover.setImageResource(R.drawable.gray);

        if (!mMovie.getBackdrop().isEmpty())
            mPicasso.load(mMovie.getBackdrop()).placeholder(R.drawable.gray).error(R.drawable.bg).into(mBackground, new Callback() {
                @Override
                public void onError() {
                    if (!isAdded())
                        return;

                    if (!mMovie.getPoster().isEmpty())
                        mPicasso.load(mMovie.getPoster()).placeholder(R.drawable.bg).error(R.drawable.bg).into(mBackground);
                    else
                        mBackground.setImageResource(R.drawable.bg);
                }

                @Override
                public void onSuccess() {
                }
            });
        else {
            if (!mMovie.getPoster().isEmpty())
                mPicasso.load(mMovie.getPoster()).placeholder(R.drawable.bg).error(R.drawable.bg).into(mBackground);
            else
                mBackground.setImageResource(R.drawable.bg);
        }
    }

    private void setLoading(boolean isLoading) {
        mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mScrollView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                //playMovie();
        }
    }

/*    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mMovie != null) {
            inflater.inflate(R.menu.tmdb_details, menu);

            if (NMJLib.isTablet(mContext)) {
                menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.checkIn).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.openInBrowser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            if (!Trakt.hasTraktAccount(mContext))
                menu.findItem(R.id.checkIn).setVisible(false);
        }
    }*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mMovie.getShowId().equals("0")) {
            if (mMovie != null) {
                inflater.inflate(R.menu.tmdb_details, menu);

                if (NMJLib.isTablet(mContext)) {
                    menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menu.findItem(R.id.openInBrowser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                if (!Trakt.hasTraktAccount(mContext))
                    menu.findItem(R.id.checkIn).setVisible(false);
            }
        } else {
            inflater.inflate(R.menu.movie_details, menu);

            // If this is a tablet, we have more room to display icons
            if (NMJLib.isTablet(mContext)) {
                menu.findItem(R.id.movie_fav).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.watched).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

        /*if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(CHROMECAST_BETA_SUPPORT, false)) {

            boolean add = false;
            for (Filepath path : mMovie.getFilepaths()) {
                if (path.isNetworkFile()) {
                    add = true;
                    break;
                }
            }

            if (add) {
                menu.add("Remote play").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final ArrayList<Filepath> networkFiles = new ArrayList<Filepath>();

                        for (Filepath path : mMovie.getFilepaths()) {
                            if (path.isNetworkFile()) {
                                networkFiles.add(path);
                            }
                        }

                        NMJLib.showSelectFileDialog(mContext, networkFiles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent i = new Intent(mContext, RemotePlayback.class);
                                i.putExtra("coverUrl", "");
                                i.putExtra("title", mMovie.getTitle());
                                i.putExtra("id", mMovie.getTmdbId());
                                i.putExtra("type", "movie");

                                if (networkFiles.get(which).getType() == FileSource.SMB) {
                                    String url = VideoUtils.startSmbServer(getActivity(), networkFiles.get(which).getFilepath(), mMovie);
                                    i.putExtra("videoUrl", url);
                                } else {
                                    i.putExtra("videoUrl", networkFiles.get(which).getFilepath());
                                }

                                startActivity(i);
                            }
                        });

                        return false;
                    }
                });
            }
        } */

            // Favourite
            menu.findItem(R.id.movie_fav).setIcon(mMovie.isFavourite() ?
                    R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                    .setTitle(mMovie.isFavourite() ?
                            R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);

            // Watchlist
            menu.findItem(R.id.watch_list).setIcon(mMovie.toWatch() ?
                    R.drawable.ic_video_collection_white_24dp : R.drawable.ic_queue_white_24dp)
                    .setTitle(mMovie.toWatch() ?
                            R.string.removeFromWatchlist : R.string.watchLater);

            // Watched / unwatched
            menu.findItem(R.id.watched).setTitle(mMovie.hasWatched() ?
                    R.string.stringMarkAsUnwatched : R.string.stringMarkAsWatched);

            // Only allow the user to browse artwork if it's a valid TMDb movie
            //menu.findItem(R.id.change_cover).setVisible(NMJLib.isValidTmdbId(mMovie.getTmdbId()));
        }

    }


    /*private void playMovie(String filepath, int filetype) {
        if (filepath.toLowerCase(Locale.getDefault()).matches(".*(cd1|part1).*")) {
            new MovieDetailsFragment.GetSplitFiles(filepath, filetype).execute();
        } else {
            mVideoPlaybackStarted = System.currentTimeMillis();
            boolean playbackStarted = VideoUtils.playVideo(getActivity(), filepath, filetype, mMovie);
            if (playbackStarted)
                checkIn();
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getActivity().getIntent().getExtras().getBoolean("isFromWidget")) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("startup", String.valueOf(Main.MOVIES));
                    i.setClass(mContext, Main.class);
                    startActivity(i);
                }

                getActivity().finish();
                return true;
            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovie.getTmdbId().replace("tmdb", ""));
                startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
                return true;
            case R.id.imdb:
                Intent imdbIntent = new Intent(Intent.ACTION_VIEW);
                imdbIntent.setData(Uri.parse("http://www.imdb.com/title/" + mMovie.getImdbId()));
                startActivity(imdbIntent);
                return true;
            case R.id.tmdb:
                Intent tmdbIntent = new Intent(Intent.ACTION_VIEW);
                tmdbIntent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovie.getTmdbId().replace("tmdb","")));
                startActivity(tmdbIntent);
                return true;
            case R.id.change_cover:
                searchCover();
                return true;
            case R.id.editMovie:
                editMovie();
                return true;
            case R.id.identify:
                identifyMovie();
                return true;
            case R.id.watched:
                watched(true);
                return true;
            case R.id.trailer:
                //VideoUtils.playTrailer(getActivity(), mMovie);
                return true;
            case R.id.watch_list:
                watchList();
                return true;
            case R.id.movie_fav:
                favAction();
                return true;
            case R.id.delete_movie:
                deleteMovie();
                return true;
            case R.id.openInBrowser:
                openInBrowser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                break;
            case R.id.share:
                shareMovie();
                break;
            case R.id.openInBrowser:
                openInBrowser();
                break;
            case R.id.checkIn:
                checkIn();
                break;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @SuppressLint("InflateParams")
    public void deleteMovie() {
/*        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        View dialogLayout = LayoutInflater.from(mContext).inflate(R.layout.delete_file_dialog_layout, null);
        final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);


        if (mMovie.getFilepaths().size() == 1 && mMovie.getFilepaths().get(0).getType() == FileSource.UPNP)
            cb.setEnabled(false);
        else
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ALWAYS_DELETE_FILE, false));

        builder.setTitle(getString(R.string.removeMovie))
                .setView(dialogLayout)
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (mIgnoreDeletedFiles) {
                            MovieDatabaseUtils.ignoreMovie(mMovie.getTmdbId());
                        } else {
                            MovieDatabaseUtils.deleteMovie(getActivity(), mMovie.getTmdbId());
                        }

                        if (cb.isChecked()) {
                            for (Filepath path : mMovie.getFilepaths()) {
                                Intent deleteIntent = new Intent(mContext, DeleteFile.class);
                                deleteIntent.putExtra("filepath", path.getFilepath());
                                mContext.startService(deleteIntent);
                            }
                        }

                        boolean movieExists = mDatabase.movieExists(mMovie.getTmdbId());

                        // We only want to delete movie images, if there are no other versions of the same movie
                        if (!movieExists) {
                            try { // Delete cover art image
                                File coverArt = mMovie.getPoster();
                                if (coverArt.exists() && coverArt.getAbsolutePath().contains("com.nmj.mizuu")) {
                                    NMJLib.deleteFile(coverArt);
                                }
                            } catch (NullPointerException e) {} // No file to delete

                            try { // Delete thumbnail image
                                File thumbnail = mMovie.getThumbnail();
                                if (thumbnail.exists() && thumbnail.getAbsolutePath().contains("com.nmj.mizuu")) {
                                    NMJLib.deleteFile(thumbnail);
                                }
                            } catch (NullPointerException e) {} // No file to delete

                            try { // Delete backdrop image
                                File backdrop = mMovie.getBackdrop();
                                if (backdrop.exists() && backdrop.getAbsolutePath().contains("com.nmj.mizuu")) {
                                    NMJLib.deleteFile(backdrop);
                                }
                            } catch (NullPointerException e) {} // No file to delete
                        }

                        notifyDatasetChanges();
                        getActivity().finish();
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create().show();*/
    }

    private void watched(boolean showToast) {
        mMovie.setHasWatched(!mMovie.hasWatched()); // Reverse the hasWatched boolean

/*        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_HAS_WATCHED, mMovie.getHasWatched());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (showToast)
                if (mMovie.hasWatched()) {
                    Toast.makeText(mContext, getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
                }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        if (mRemoveMoviesFromWatchlist)
            removeFromWatchlist();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
                watchedMovies.add(mMovie);
                Trakt.markMovieAsWatched(watchedMovies, mContext);
            }
        }.start();*/
    }

    public void watchList() {
        mMovie.setToWatch(!mMovie.toWatch()); // Reverse the toWatch boolean

/*        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_TO_WATCH, mMovie.getToWatch());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (mMovie.toWatch()) {
                Toast.makeText(mContext, getString(R.string.addedToWatchList), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, getString(R.string.removedFromWatchList), Toast.LENGTH_SHORT).show();
            }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> watchlist = new ArrayList<Movie>();
                watchlist.add(mMovie);
                Trakt.movieWatchlist(watchlist, mContext);
            }
        }.start();*/
    }

    public void favAction() {
        mMovie.setFavourite(!mMovie.isFavourite()); // Reverse the favourite boolean

/*        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_FAVOURITE, mMovie.getFavourite());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (mMovie.isFavourite()) {
                Toast.makeText(mContext, getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
                getActivity().setResult(2); // Favorite removed
            }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> movie = new ArrayList<Movie>();
                movie.add(mMovie);
                Trakt.movieFavorite(movie, mContext);
            }
        }.start();*/
    }

    public void shareMovie() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovie.getTmdbId().replace("tmdb", ""));
        startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
    }

    public void identifyMovie() {
/*        if (mMovie.getFilepaths().size() == 1) {
            getActivity().startActivityForResult(getIdentifyIntent(mMovie.getFilepaths().get(0).getFullFilepath()), 0);
        } else {
            NMJLib.showSelectFileDialog(mContext, mMovie.getFilepaths(), new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(getIdentifyIntent(mMovie.getFilepaths().get(which).getFullFilepath()));

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
        }*/
    }

    public void editMovie() {
        Intent intent = new Intent(mContext, EditMovie.class);
        intent.putExtra("movieId", mMovie.getTmdbId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        getActivity().startActivityForResult(intent, 1);
    }

    public void searchCover() {
        if (NMJLib.isOnline(mContext)) { // Make sure that the device is connected to the web
            Intent intent = new Intent(mContext, MovieCoverFanartBrowser.class);
            intent.putExtra("tmdbId", mMovie.getTmdbId());
            intent.putExtra("collectionId", mMovie.getCollectionId());
            intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
            startActivity(intent); // Start the intent for result
        } else {
            // No movie ID / Internet connection
            Toast.makeText(mContext, getString(R.string.coverSearchFailed), Toast.LENGTH_LONG).show();
        }
    }

    public void openInBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovie.getTmdbId().replace("tmdb", "")));
        startActivity(Intent.createChooser(intent, getString(R.string.openWith)));
    }

    public void watchTrailer() {
        new TmdbTrailerSearch(getActivity(), mMovie.getTmdbId(), mMovie.getTitle()).execute();
    }

    public void checkIn() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return Trakt.performMovieCheckin(mMovie.getTmdbId(), mContext);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result)
                    Toast.makeText(mContext, getString(R.string.checked_in), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(mContext, getString(R.string.errorSomethingWentWrong), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private class MovieLoader extends AsyncTask<String, Object, Object> {
        @Override
        protected Object doInBackground(String... params) {
            System.out.println("TMDB Id: " + mMovie.getTmdbId());

            if (mMovie.getShowId().equals("0"))
                mMovie = mTMDbMovieApiService.getCompleteTMDbMovie(mMovie.getTmdbId(), "en");
            else
                mMovie = mNMJMovieApiService.getCompleteNMJMovie(mMovie.getShowId(), "en");
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            getActivity().invalidateOptionsMenu();

            setupFields();
        }
    }
}
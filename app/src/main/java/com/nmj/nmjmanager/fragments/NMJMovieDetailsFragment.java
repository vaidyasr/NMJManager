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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.nmj.apis.nmj.Movie;
import com.nmj.apis.nmj.NMJMovieService;
import com.nmj.apis.trakt.Trakt;
import com.nmj.base.NMJActivity;
import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;
import com.nmj.functions.PaletteLoader;
import com.nmj.functions.SimpleAnimatorListener;
import com.nmj.functions.TmdbTrailerSearch;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
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

public class NMJMovieDetailsFragment extends Fragment {

    private Activity mContext;
    private TextView mTitle, mPlot, mGenre, mRuntime, mReleaseDate, mRating, mTagline, mCertification;
    private ImageView mBackground, mCover;
    private Movie mMovie;
    private ObservableScrollView mScrollView;
    private View mProgressBar, mDetailsArea;
    private boolean mRetained = false;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mBold, mCondensedRegular;
    private NMJMovieService mMovieApiService;
    private HorizontalCardLayout mCastLayout, mCrewLayout, mSimilarMoviesLayout;
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

        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        mMovieApiService = NMJMovieService.getInstance(mContext);

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
        mScrollView = (ObservableScrollView) v.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) v.findViewById(R.id.fab);

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
        v.findViewById(R.id.textView3).setVisibility(View.GONE); // File

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
            mRuntime.setText(NMJLib.getPrettyRuntime(getActivity(), Integer.parseInt(mMovie.getRuntime())));

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

                                mCastLayout.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mMovie.getCast(), HorizontalCardLayout.ACTORS, mToolbarColor);
                                NMJLib.removeViewTreeObserver(mCastLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mCastLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getCastBrowserMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor));
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

                                mCrewLayout.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mMovie.getCrew(), HorizontalCardLayout.ACTORS, mToolbarColor);
                                NMJLib.removeViewTreeObserver(mCrewLayout.getViewTreeObserver(), this);
                            }
                        }
                    });
            mCrewLayout.setSeeMoreOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(IntentUtils.getCrewBrowserMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor));
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

                                mSimilarMoviesLayout.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mMovie.getSimilarMovies(), HorizontalCardLayout.RELATED_MOVIES, mToolbarColor);
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

            ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mMovie.getTitle(), mToolbarColor);

            setLoading(false);

            loadImages();
        }
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

    @Override
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
    }

    public void shareMovie() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovie.getTmdbId());
        startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
    }

    public void openInBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovie.getTmdbId()));
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
            mMovie = mMovieApiService.getCompleteNMJMovie(mMovie.getShowId());

            for (int i = 0; i < mMovie.getSimilarMovies().size(); i++) {
                String id = mMovie.getSimilarMovies().get(i).getId();
                mMovie.getSimilarMovies().get(i).setInLibrary(NMJManagerApplication.getMovieAdapter().movieExists(id));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            getActivity().invalidateOptionsMenu();

            setupFields();
        }
    }

    private class GetSplitFiles extends AsyncTask<String, Void, List<NMJMovieDetailsFragment.SplitFile>> {

        private ProgressDialog progress;
        private String orig_filepath;
        private int fileType;

        public GetSplitFiles(String filepath, int filetype) {
            this.orig_filepath = filepath;
            fileType = filetype;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setIndeterminate(true);
            progress.setTitle(getString(R.string.loading_movie_parts));
            progress.setMessage(getString(R.string.few_moments));
            progress.show();
        }

        @Override
        protected List<NMJMovieDetailsFragment.SplitFile> doInBackground(String... params) {
            List<NMJMovieDetailsFragment.SplitFile> parts = new ArrayList<NMJMovieDetailsFragment.SplitFile>();
            List<String> temp;

            try {
                if (fileType == FileSource.SMB)
                    temp = NMJLib.getSplitParts(orig_filepath, NMJLib.getLoginFromFilepath(NMJLib.TYPE_MOVIE, orig_filepath));
                else
                    temp = NMJLib.getSplitParts(orig_filepath, null);

                for (int i = 0; i < temp.size(); i++)
                    parts.add(new NMJMovieDetailsFragment.SplitFile(temp.get(i)));

            } catch (Exception e) {
            }

            return parts;
        }

        @Override
        protected void onPostExecute(final List<NMJMovieDetailsFragment.SplitFile> result) {
            progress.dismiss();

            //if (result.size() > 0)
            //    mVideoPlaybackStarted = System.currentTimeMillis();

            /*if (result.size() > 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(getString(R.string.playPart));
                builder.setAdapter(new MovieDetailsFragment.SplitAdapter(mContext, result), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        boolean playbackStarted = VideoUtils.playVideo(getActivity(), result.get(which).getFilepath(), fileType, mMovie);
                        if (playbackStarted)
                            checkIn();
                    }});
                builder.show();
            } else if (result.size() == 1) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), result.get(0).getFilepath(), fileType, mMovie);
                if (playbackStarted)
                    checkIn();
            } else {
                Toast.makeText(mContext, getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
            }*/
        }
    }

    private class SplitFile {

        String filepath;

        public SplitFile(String filepath) {
            this.filepath = filepath;
        }

        public String getFilepath() {
            return filepath;
        }

        public String getUserFilepath() {
            return NMJLib.transformSmbPath(filepath);
        }

        public int getPartNumber() {
            return NMJLib.getPartNumberFromFilepath(getUserFilepath());
        }
    }
}
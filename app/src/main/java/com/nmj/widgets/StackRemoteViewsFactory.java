package com.nmj.widgets;

/**
 * Created by VSRIRAM2 on 11/6/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.nmj.db.DbAdapterMovies;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.functions.SmallMovie;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.nmj.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private ArrayList<SmallMovie> movies = new ArrayList<SmallMovie>();
    private DbAdapterMovies dbHelper;
    private boolean ignorePrefixes;
    private Picasso mPicasso;
    private Bitmap.Config mConfig;
    private RemoteViews mLoadingView;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);

        mPicasso = NMJManagerApplication.getPicasso(mContext);
        mConfig = NMJManagerApplication.getBitmapConfig();

        mLoadingView = new RemoteViews(mContext.getPackageName(), R.layout.movie_cover_widget_item);
    }

    public void onCreate() {
        update();
    }

    public void onDestroy() {
        movies.clear();
    }

    public int getCount() {
        return movies.size();
    }

    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

        try {
            Bitmap cover = mPicasso.load(movies.get(position).getThumbnail()).config(mConfig).get();
            view.setImageViewBitmap(R.id.widget_item, cover);
        } catch (IOException e) {
            view.setImageViewResource(R.id.widget_item, R.drawable.loading_image);

            // Text
            view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
            view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
        }

        Intent fillInIntent = new Intent(MovieStackWidgetProvider.MOVIE_STACK_WIDGET);
        fillInIntent.putExtra("tmdbId", movies.get(position).getTmdbId());
        view.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return view;
    }

    private void update() {
        movies.clear();

        // Create and open database
        dbHelper = NMJManagerApplication.getMovieAdapter();

        Cursor cursor = dbHelper.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
        ColumnIndexCache cache = new ColumnIndexCache();

        try {
            while (cursor.moveToNext()) {
                movies.add(new SmallMovie(mContext,
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TITLE)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
                        ignorePrefixes
                ));
            }
        } catch (NullPointerException e) {
        } finally {
            cursor.close();
            cache.clear();
        }

        Collections.sort(movies);
    }

    public RemoteViews getLoadingView() {
        return mLoadingView;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return false;
    }

    public void onDataSetChanged() {
        update();
    }
}
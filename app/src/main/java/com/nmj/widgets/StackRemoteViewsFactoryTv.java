package com.nmj.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViewsService;

import com.nmj.db.DbAdapterTvShows;
import com.nmj.db.DbAdapterTvShows;
import com.nmj.functions.ColumnIndexCache;
import com.nmj.nmjmanager.NMJManagerApplication;
import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.TvShow;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

import static com.nmj.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

/**
 * Created by VSRIRAM2 on 11/6/2017.
 */

class StackRemoteViewsFactoryTv implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private ArrayList<TvShow> shows = new ArrayList<TvShow>();
    private Cursor cursor;
    private DbAdapterTvShows dbHelper;
    private boolean ignorePrefixes;

    public StackRemoteViewsFactoryTv(Context context, Intent intent) {
        mContext = context;
    }

    public void onCreate() {
        update();
    }

    public void onDestroy() {
        shows.clear();
    }

    public int getCount() {
        return shows.size();
    }

    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

        // Image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        options.inPreferQualityOverSpeed = true;

        Bitmap cover = BitmapFactory.decodeFile(shows.get(position).getThumbnail().getAbsolutePath(), options);

        if (cover != null)
            view.setImageViewBitmap(R.id.widget_item, cover);
        else {
            view.setImageViewResource(R.id.widget_item, R.drawable.loading_image);

            // Text
            view.setTextViewText(R.id.widgetTitle, shows.get(position).getTitle());
            view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
        }

        Intent fillInIntent = new Intent(ShowStackWidgetProvider.SHOW_STACK_WIDGET);
        fillInIntent.putExtra("showId", shows.get(position).getId());
        view.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return view;
    }

    private void update() {
        shows.clear();

        ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);

        // Create and open database
        dbHelper = NMJManagerApplication.getTvDbAdapter();

        cursor = dbHelper.getAllShows();
        ColumnIndexCache cache = new ColumnIndexCache();

        try {
            while (cursor.moveToNext()) {
                shows.add(new TvShow(
                        mContext,
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_TITLE)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_PLOT)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RATING)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_GENRES)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ACTORS)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RUNTIME)),
                        ignorePrefixes,
                        cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
                        NMJManagerApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)))
                ));
            }
        } catch (NullPointerException e) {
        } finally {
            cursor.close();
            cache.clear();
        }

        Collections.sort(shows);
    }

    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        update();
    }
}
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

package com.nmj.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.nmj.nmjmanager.R;
import com.nmj.nmjmanager.TvShowDetails;

public class ShowStackWidgetProvider extends AppWidgetProvider {

	public final static String SHOW_STACK_WIDGET = "showStackWidget";
	
	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {  
		String action = intent.getAction();
		
		if (SHOW_STACK_WIDGET.equals(action)) {
			Intent openShow = new Intent();
			openShow.putExtra("showId", intent.getStringExtra("showId"));
			openShow.putExtra("isFromWidget", true);
			openShow.setClass(context, TvShowDetails.class);
			openShow.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(openShow);
		} else {
			super.onReceive(context, intent);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// update each of the widgets with the remote adapter
		for (int i = 0; i < appWidgetIds.length; ++i) {

			// Here we setup the intent which points to the StackViewService which will
			// provide the views for this collection.
			Intent intent = new Intent(context, ShowStackWidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			// When intents are compared, the extras are ignored, so we need to embed the extras
			// into the data so that the extras will not be ignored.
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			rv.setRemoteAdapter(appWidgetIds[i], R.id.stack_view, intent);

			// The empty view is displayed when the collection has no items. It should be a sibling
			// of the collection view.
			rv.setEmptyView(R.id.stack_view, R.id.empty_view);

			// Here we setup the a pending intent template. Individuals items of a collection
			// cannot setup their own pending intents, instead, the collection as a whole can
			// setup a pending intent template, and the individual items can set a fillInIntent
			// to create unique before on an item to item basis.
			Intent toastIntent = new Intent(context, ShowStackWidgetProvider.class);
			toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setPendingIntentTemplate(R.id.stack_view, toastPendingIntent);

			appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}
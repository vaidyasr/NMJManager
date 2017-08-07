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

package com.nmj.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.nmj.nmjmanager.R;
import com.nmj.widgets.MovieBackdropWidgetProvider;
import com.nmj.widgets.MovieCoverWidgetProvider;
import com.nmj.widgets.MovieStackWidgetProvider;
import com.nmj.widgets.ShowBackdropWidgetProvider;
import com.nmj.widgets.ShowCoverWidgetProvider;
import com.nmj.widgets.ShowStackWidgetProvider;

public class WidgetUtils {

	private WidgetUtils() {} // No instantiation

	/**
	 * Updates all movie widgets.
	 * @param context
	 */
	public static void updateMovieWidgets(Context context) {
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}

	/**
	 * Updates all TV show widgets.
	 * @param context
	 */
	public static void updateTvShowWidgets(Context context) {
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}
}

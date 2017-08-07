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

package com.nmj.apis.trakt;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class TraktTvShow {

	private String mId, mTitle;
	private Multimap<String, String> mSeasonEpisodesMapping = LinkedListMultimap.create();
	
	public TraktTvShow(String id, String title) {
		mId = id;
		mTitle = title;
	}
	
	public void addEpisode(String season, String episode) {
		mSeasonEpisodesMapping.put(season, episode);
	}
	
	public Multimap<String, String> getSeasons() {
		return mSeasonEpisodesMapping;
	}
	
	public boolean contains(String season, String episode) {
		return getSeasons().get(season).contains(episode);
	}
	
	public String getId() {
		return mId;
	}
	
	public String getTitle() {
		return mTitle;
	}
}
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

package com.nmj.apis.nmj;

public class Season {

	private String mCoverPath, mSeasonId, mSeasonTitle;
	private int mSeason, mEpisode;
	
	public String getCoverPath() {
		return mCoverPath;
	}

    public void setCoverPath(String cover) {
        mCoverPath = cover;
    }
	
	public int getSeason() {
		return mSeason;
    }

    public void setSeason(int season) {
        mSeason = season;
    }

    public void setSeasonId(String seasonId){
        mSeasonId = seasonId;
    }

    public void setSeasonTitle(String seasonTitle){
        mSeasonTitle = seasonTitle;
    }

    public String getSeasonId(){
        return mSeasonId;
    }

    public String getSeasonTitle(){
        return mSeasonTitle;
    }

    public void setEpisodeCount(int episode) { mEpisode = episode;}

    public int getEpisodeCount() { return mEpisode;}
}

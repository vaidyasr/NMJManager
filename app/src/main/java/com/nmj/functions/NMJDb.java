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

package com.nmj.functions;

import android.content.Context;

public class NMJDb {

	private String mName, mDbPath, mDrivePath, mNMJType, mWritable, mImage, mJukebox, mMachineType, mDeviceType, mPath;

	public NMJDb(Context context, String name, String dbpath, String drivepath) {

        // Set up movie fields based on constructor
		mName = name;
		mDbPath = dbpath;
		mDrivePath = drivepath;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getDbPath() {
		return mDbPath;
	}

	public void setDbPath(String dbPath) {
		this.mDbPath = dbPath;
	}

	public String getDrivePath() {
		return mDrivePath;
	}

	public void setDrivePath(String drivePath) {
		this.mDrivePath = drivePath;
	}

	public String getNMJType() {
		return mNMJType;
	}

	public void setNMJType(String nmjType) {
		mNMJType = nmjType;
	}

	public String getJukebox() {
		return mJukebox;
	}

	public void setJukebox(String jukebox) {
		mJukebox = jukebox;
	}

	public void setPath(String path) {
		mPath = path;
	}

	public void setDeviceType(String deviceType) {
		mDeviceType = deviceType;
	}

	public String getPath(){
		return mPath;
	}

	public String getDeviceType(){
		return mDeviceType;
	}

	public String getWritable() {
		return mWritable;
	}

	public void setWritable(String writable) {
		mWritable = writable;
	}
}
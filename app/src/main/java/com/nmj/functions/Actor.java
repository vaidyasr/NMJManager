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

import android.text.TextUtils;

import com.nmj.utils.StringUtils;

public class Actor {

	private String mName, mCharacter, mId, mUrl, personType;

	public Actor(String name, String character, String id, String pType, String url) {
		mName = name;
		mCharacter = character;
		personType = pType;
		mId = id;
		mUrl = url;
	}

	public String getName() {
		return mName;
	}

	public String getId() {
		return mId;
	}

	public String getPersonType() {
		return personType;
	}

	public String getUrl() {
		return mUrl;
	}

	public String getCharacter() {
		String character = StringUtils.replacePipesWithCommas(mCharacter);
		if (TextUtils.isEmpty(character) || character.equals("null"))
			return "";
		return character;
	}
}
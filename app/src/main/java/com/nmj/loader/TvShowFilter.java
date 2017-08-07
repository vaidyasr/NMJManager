package com.nmj.loader;/*
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

public class TvShowFilter {

    public static final int INDEX = 0,
            GENRE = 1,
            CERTIFICATION = 2,
            RELEASE_YEAR = 3,
            USER_RATING = 4,
            VIDEO_RESOLUTION = 5,
            OTHERS = 6;

    private final int mType;
    private String mFilter;

    public TvShowFilter(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setFilter(String filter) {
        mFilter = filter;
    }

    public String getFilter() {
        return mFilter;
    }

    private String getEqualsComparison() {
        return getType() + "_" + getFilter();
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj == null || !(obj instanceof TvShowFilter)) &&
                getEqualsComparison().equals(((TvShowFilter) obj).getEqualsComparison());
    }

    @Override
    public int hashCode() {
        return getEqualsComparison().hashCode();
    }

    @Override
    public String toString() {
        return getEqualsComparison();
    }
}

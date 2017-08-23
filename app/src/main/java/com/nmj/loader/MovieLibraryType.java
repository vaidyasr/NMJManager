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

package com.nmj.loader;

public enum MovieLibraryType {

        ALL_MOVIES(MovieLoader.ALL_MOVIES),
        FAVORITES(MovieLoader.FAVORITES),
        NEW_RELEASES(MovieLoader.NEW_RELEASES),
        WATCHLIST(MovieLoader.WATCHLIST),
        WATCHED(MovieLoader.WATCHED),
        UNWATCHED(MovieLoader.UNWATCHED),
        COLLECTIONS(MovieLoader.COLLECTIONS),
        LISTS(MovieLoader.LISTS),
        UPCOMING(MovieLoader.UPCOMING),
        NOW_PLAYING(MovieLoader.NOW_PLAYING),
        POPULAR(MovieLoader.POPULAR),
        TOP_RATED(MovieLoader.TOP_RATED),
    COLLECTION_MOVIES(MovieLoader.COLLECTION_MOVIES),
    LIST_MOVIES(MovieLoader.LIST_MOVIES);

        private final int mType;

        MovieLibraryType(int type) {
            mType = type;
        }

        public static MovieLibraryType fromInt(int type) {
            switch (type) {
                case MovieLoader.ALL_MOVIES:
                    return ALL_MOVIES;
                case MovieLoader.FAVORITES:
                    return FAVORITES;
                case MovieLoader.NEW_RELEASES:
                    return NEW_RELEASES;
                case MovieLoader.WATCHLIST:
                    return WATCHLIST;
                case MovieLoader.WATCHED:
                    return WATCHED;
                case MovieLoader.UNWATCHED:
                    return UNWATCHED;
                case MovieLoader.COLLECTIONS:
                    return COLLECTIONS;
                case MovieLoader.LISTS:
                    return LISTS;
                case MovieLoader.UPCOMING:
                    return UPCOMING;
                case MovieLoader.NOW_PLAYING:
                    return NOW_PLAYING;
                case MovieLoader.POPULAR:
                    return POPULAR;
                case MovieLoader.TOP_RATED:
                    return TOP_RATED;
                case MovieLoader.LIST_MOVIES:
                    return LIST_MOVIES;
                case MovieLoader.COLLECTION_MOVIES:
                    return COLLECTION_MOVIES;
                default:
                    return ALL_MOVIES;
            }
        }

    public int getType() {
        return mType;
        }
    }
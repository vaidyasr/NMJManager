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


public class Video {

    private String mshowId, mvideoId, mSize, mPath, width = "", height = "", threeD = "", runtime = "",
            aspectRatio = "", system = "", videoCodec = "", FPS = "", playCount = "", subtitle = "",
            bookmarkTime = "", bookmarkThumbnail = "", resolution = "";

    public Video(String showId, String videoId, String size, String path) {
            mshowId = showId;
            mvideoId = videoId;
            mSize = size;
            mPath = path;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setShowId(String showId) {
        this.mshowId = showId;
    }

    public String getShowId(){
        return mshowId;
    }

    public void setVideoId(String videoId) {
        this.mvideoId = videoId;
    }

    public String getVideoId() {
        return mvideoId;
    }

    public void setSize(String size) {
        this.mSize = size;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getSize() {
        return mSize;
    }

    public String getVideoIdByShowId(String showId) {
        return this.mvideoId;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getWidth() {
        return width;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    public String getResolution() {
        resolution = this.width + "x" + this.height;
        return resolution;
    }

    public void setResolution() {
        this.resolution = this.width + "x" + this.height;
    }


    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getSystem() {
        return system;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setFPS(String fps) {
        this.FPS = fps;
    }

    public String getFPS() {
        return FPS;
    }

    public void setThreeD(String threeD) {
        this.threeD = threeD;
    }

    public String getThreeD(){
        return threeD;
    }

    public void setBookmarkTime(String bookmarkTime) {
        this.bookmarkTime = bookmarkTime;
    }

    public String getBookmarkTime() {
        return bookmarkTime;
    }

    public void setPlayCount(String playCount) {
        this.playCount = playCount;
    }

    public String getPlayCount() {
        return playCount;
    }

    public void setBookmarkThumbnail(String bookmarkThumbnail) {
        this.bookmarkThumbnail = bookmarkThumbnail;
    }

    public String getBookmarkThumbnail() {
        return bookmarkThumbnail;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
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

package com.nmj.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class EpisodePhotoImageView extends ImageView {

    public EpisodePhotoImageView(Context context) {
        super(context);
    }

    public EpisodePhotoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EpisodePhotoImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int height = MeasureSpec.getSize(heightMeasureSpec);
    	int width = (int) (height * 1.7);
        setMeasuredDimension(width, height);
    }
}
package com.hapi.avcapture.screen;

import com.hapi.avcapture.VideoFrame;

public interface ImageListener {
    public void onImageAvailable(VideoFrame videoFrame);

    void onBindingDied();
}
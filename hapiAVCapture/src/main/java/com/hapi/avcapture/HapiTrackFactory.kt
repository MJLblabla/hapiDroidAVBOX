package com.hapi.avcapture

import androidx.lifecycle.LifecycleOwner
import com.hapi.avcapture.screen.ScreenRecordService
import com.hapi.avparam.AVChannelConfig
import com.hapi.avparam.AVSampleFormat

object HapiTrackFactory {

    /**
     * 创建摄像头轨道
     *
     * @param lifecycleOwner
     * @param width
     * @param height
     * @param fps
     * @return
     */
    fun createCameraXTrack(
        lifecycleOwner: LifecycleOwner?,
        width: Int,
        height: Int,
        fps: Int = 30
    ): CameraXTrack {
        return if (lifecycleOwner == null) {
            CameraXTrack(width, height, fps)
        } else {
            CameraXTrack(width, height, fps, lifecycleOwner)
        }
    }

    /**
     * 创建麦克风轨道
     *
     * @param lifecycleOwner
     * @param sampleRateInHz
     * @param AVChannelConfig
     * @param audioFormat
     * @return
     */
    fun createMicrophoneTrack(
        lifecycleOwner: LifecycleOwner? = null,
        sampleRateInHz: Int = DEFAULT_SAMPLE_RATE,
        AVChannelConfig: AVChannelConfig = DEFAULT_CHANNEL_LAYOUT,
        audioFormat: AVSampleFormat = DEFAULT_SAMPLE_FORMAT
    ): MicrophoneTrack {
        return MicrophoneTrack(lifecycleOwner, sampleRateInHz, AVChannelConfig, audioFormat)
    }

    /**
     * Create custom audio track
     *
     * @return
     */
    fun createCustomAudioTrack(): CustomAudioTrack {
        return CustomAudioTrack()
    }

    /**
     * Create custom video track
     *
     * @return
     */
    fun createCustomVideoTrack(): CustomVideoTrack {
        return CustomVideoTrack()
    }

    /**
     * Create 屏幕共享轨道
     *
     * @param lifecycleOwner
     * @param with
     * @param height
     * @return
     */
    fun createScreenCaptureTrack(
        lifecycleOwner: LifecycleOwner? = null,
        with: Int = ScreenRecordService.screenWidth,
        height: Int = ScreenRecordService.screenHeight,
    ): ScreenCaptureTrack {
        return ScreenCaptureTrack(lifecycleOwner, with, height)
    }
}
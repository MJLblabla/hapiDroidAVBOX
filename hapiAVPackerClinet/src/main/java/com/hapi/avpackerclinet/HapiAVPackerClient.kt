package com.hapi.avpackerclinet

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.lifecycle.LifecycleOwner
import com.hapi.ioutput.ConnectedStatus
import com.hapi.avcapture.*
import com.hapi.avencoder.*
import com.hapi.avparam.*
import com.hapi.ioutput.MuxerCallBack
import com.hapi.ioutput.OutputStreamerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.jvm.Throws

/**
 * Hapi a v packer client
 *
 * @constructor Create empty Hapi a v packer client
 */
class HapiAVPackerClient : LifecycleEventObserver {

    var packerClientListener: PackerClientListener? = null
    private val mAVStreamList = AVStreamList().apply {
        encoderStatusCallBack = object : EncoderStatusCallBack {
            override fun onEncoderStatusChange(encodeStatus: EncoderStatus) {
                GlobalScope.launch(Dispatchers.Main) {
                    packerClientListener?.onEncoderStatusChange(encodeStatus)
                }
            }
        }
    }

    private val mInnerMuxerCallBack = object : MuxerCallBack {
        override fun onMuxerConnectedStatus(status: ConnectedStatus) {
            packerClientListener?.onMuxerConnectedStatus(status)
            connectedStatus = status
            if (status == ConnectedStatus.CONNECTED_STATUS_CONNECTED) {
                mAVStreamList.mediaStreams.forEach {
                    it.start()
                }
            }
            if (status == ConnectedStatus.CONNECTED_STATUS_DISCONNECTED) {
                mAVStreamList.mediaStreams.forEach {
                    it.stop()
                }
            }
        }

        override fun onOutputStreamerEvent(event: OutputStreamerEvent, msg: String) {
            packerClientListener?.onOutputStreamerEvent(event, msg)
        }
    }

    private var mMuxer: AVOutPutContext = AVOutPutContext().apply {
        muxerCallBack = this@HapiAVPackerClient.mInnerMuxerCallBack
        bitrateRegulatorCallBack = { bit, streamID ->
            mAVStreamList.findAVStreamByStreamID(streamID)?.encoderContext?.updateBitRate(bit)
        }
    }

    /**
     * 打包器连接状态
     */
    var connectedStatus = ConnectedStatus.CONNECTED_STATUS_NULL
        private set

    /**
     * 绑定轨道
     *
     * @param track
     * @return
     */
    fun attachTrack(track: Track<*>, encodeParam: EncodeParam): Boolean {
        if ((mAVStreamList.findFistStreamEncodeStatus()
                ?: EncoderStatus.STATE_PREPARE) != EncoderStatus.STATE_PREPARE
        ) {
            return false
        }
        if (encodeParam is VideoEncodeParam) {
            return mAVStreamList.addVideoAVStream(track as IVideoTrack, encodeParam, mMuxer)
        }
        if (encodeParam is AudioEncodeParam) {
            return mAVStreamList.addAudioAVStream(track as IAudioTrack, encodeParam, mMuxer)
        }
        return false
    }

    /**
     * 取消绑定轨道
     * @param track
     */
    fun detachTrack(track: Track<*>) {
        if (mAVStreamList.findFistStreamEncodeStatus() != EncoderStatus.STATE_PREPARE) {
            return
        }
        mAVStreamList.removeAVStream(track)
    }

    /**
     * Mute track
     * 禁用某个轨道数据
     * @param isMute
     * @param track
     */
    fun muteTrack(isMute: Boolean, track: Track<*>) {
        mAVStreamList.findAVStreamByTrackID(track.trackID)?.mute(isMute)
    }

    /**
     * Start
     *
     * @param url
     * @param param
     * @return
     */
    @Throws(Exception::class)
    fun start(url: String): Boolean {
        if (mAVStreamList.mediaStreams.isEmpty()) {
            return false
        }
        if (mAVStreamList.findFistStreamEncodeStatus() != EncoderStatus.STATE_PREPARE) {
            return false
        }
        mMuxer.open(url)
        return true
    }

    /**
     * Stop
     *
     * @return
     */
    fun stop() {
        mAVStreamList.mediaStreams.forEach {
            it.stop()
        }
        mMuxer.close()
    }

    /**
     * Pause
     *
     * @return
     */
    fun pause(): Boolean {
        var ret = false
        mAVStreamList.mediaStreams.forEach {
            ret = it.encoderContext?.pause() ?: false
        }
        return ret
    }

    /**
     * Resume
     *
     * @return
     */
    fun resume(): Boolean {
        var ret = false
        mAVStreamList.mediaStreams.forEach {
            ret = it.encoderContext?.resume() ?: false
        }
        return ret
    }

    /**
     * Release
     */
    fun release() {
        mMuxer.release()
        mAVStreamList.mediaStreams.forEach {
            it.release()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            stop()
            release()
        }
    }
}
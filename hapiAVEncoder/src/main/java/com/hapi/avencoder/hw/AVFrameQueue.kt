package com.hapi.avencoder.hw

import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class AVFrameQueue<T> {

    private val mFrameQueue: LinkedBlockingDeque<T> by lazy {
        LinkedBlockingDeque<T>()
    }
    private val mFreeBuffer = LinkedList<ByteBuffer>()
    val maxFreeBufferSize = 10

    fun popFrame(timeout: Long, unit: TimeUnit): T {
        return mFrameQueue.poll(timeout, unit)
    }

    fun frameSize(): Int {
        return mFrameQueue.size
    }

    fun pushFrame(frame: T) {
        mFrameQueue.push(frame)
    }

    fun clear() {
        mFrameQueue.clear()
        mFreeBuffer.clear()
    }

    fun allocateAVFrameBuffer(size: Int): ByteBuffer {
        return if (mFreeBuffer.isEmpty()) {
            ByteBuffer.allocateDirect(size)
        } else {
            mFreeBuffer.pop()
        }
    }

    fun recycleByteBuffer(buffer: ByteBuffer) {
        if (mFreeBuffer.size < maxFreeBufferSize) {
            mFreeBuffer.push(buffer)
        }
    }
}
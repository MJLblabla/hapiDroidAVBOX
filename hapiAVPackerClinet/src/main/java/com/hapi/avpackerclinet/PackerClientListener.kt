package com.hapi.avpackerclinet

import com.hapi.ioutput.MuxerCallBack
import com.hapi.avencoder.EncoderStatusCallBack

interface PackerClientListener : MuxerCallBack, EncoderStatusCallBack {
}
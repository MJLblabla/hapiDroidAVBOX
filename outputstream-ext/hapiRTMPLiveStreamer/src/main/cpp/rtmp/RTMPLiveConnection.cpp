//
// Created by 满家乐 on 2022/12/16.
//

#include "RTMPLiveConnection.h"

#define STR2AVAL(av, str)    av.av_val = str; av.av_len = strlen(av.av_val)

RTMPLiveConnection::RTMPLiveConnection() : IConnection() {
    this->mRtmp = RTMP_Alloc();
    RTMP_Init(mRtmp);
    LOGCATE("RTMPLiveConnection::RTMP_Alloc() %p", mRtmp);
    RTMP_LogSetCallback([](int level, const char *fmt, va_list args) {
        if(level==5){
            return ;
        }
        char log[1024];
        vsprintf(log, fmt, args);
        LOGCATE("RTMP_LogSetCallback %d %s", level,log);
    });
}

void RTMPLiveConnection::open(const char *url) {

    strcpy(m_OutUrl, url);
    new std::thread([this]() {
       // mRtmp->Link.lFlags |= RTMP_LF_FTCU; // let librtmp free tcUrl on close  会导致主动close报错
       // mRtmp->Link.timeout = 3;
        int ret = RTMP_SetupURL(mRtmp, m_OutUrl);
        RTMP_EnableWrite(mRtmp);
        beforeOpen();
        ret = RTMP_Connect(mRtmp, nullptr);
        if (ret) {
            //seek 到某一处
            ret = RTMP_ConnectStream(mRtmp, 0);
            onOpen(true);
            LOGCATE("RTMP_ConnectStream::RTMP_ConnectStream %d %s %p", ret, m_OutUrl, mRtmp);
        } else {
            changeConnectedStatus(CONNECTED_STATUS_CONNECT_FAIL);
        }
    });
}

void RTMPLiveConnection::sendFLVPacket(Packet &packet) {
    if (!isOpen) {
        return;
    }
    auto *copy = new Packet(packet);
    pushPacket(copy);
}

void RTMPLiveConnection::sendOutPacket(Packet *packet) {
   // LOGCATE("sendOutPacket %p",mRtmp);
    int ret = RTMP_Write(mRtmp, packet->data, packet->dataSize);
    if (ret <= 0) {
        sendEvent(EVENT_SEND_PACKET_FAIL, "todo");
        LOGCATE("RTMPLiveConnection::RTMP_Write %d  ", ret);
        bool isConnected = RTMP_IsConnected(mRtmp);
        if (!isConnected) {
            changeConnectedStatus(CONNECTED_STATUS_OFFLINE);
        }
    }
}

void RTMPLiveConnection::close() {
    if (isOpen && mRtmp != nullptr) {
        //LOGCATE("RTMPLiveConnection::close before() %p",mRtmp);
        onClose();
        // std::this_thread::sleep_for(std::chrono::seconds (2));
        LOGCATE("RTMPLiveConnection::close() %p", mRtmp);
         RTMP_DeleteStream(mRtmp);
         RTMP_Close(mRtmp);
    }
}

RTMPLiveConnection::~RTMPLiveConnection() {
    LOGCATE(":RTMP_Free(mRtmp)RTMP_Free(mRtmp) %p", mRtmp);
    if (mRtmp != nullptr) {
        RTMP_Free(mRtmp);
    }
    mRtmp = nullptr;
}
//
// Created by 满家乐 on 2022/12/16.
//

#include "SrtLiveConnection.h"

SrtLiveConnection::SrtLiveConnection() : IConnection() {
    srt_startup();
}

SrtLiveConnection::~SrtLiveConnection() {
    srt_cleanup();
    if(srtConfig!= nullptr){
        delete srtConfig;
    }
    srtConfig= nullptr;
}

void SrtLiveConnection::open(SrtConfig &config) {
    mSRTSocket = srt_create_socket();
    if(srtConfig!= nullptr){
        delete srtConfig;
    }
    srtConfig = new SrtConfig(std::move(config));
    beforeOpen();
    new std::thread([this]() {

        int minversion = SRT_VERSION_FEAT_HSv5;
        if (srtConfig->payload_size <= 0) {
            srtConfig->payload_size = PAYLOAD_SIZE;
        }
        srt_setsockopt(mSRTSocket, 0, SRTO_MINVERSION, &minversion, sizeof minversion);
        srt_setsockopt(mSRTSocket, 0, SRTO_PAYLOADSIZE, &srtConfig->payload_size,
                       sizeof srtConfig->payload_size);
        int transMode = SRTT_LIVE;
        srt_setsockopt(mSRTSocket, 0, SRTO_TRANSTYPE, &transMode, sizeof transMode);
        srt_setsockopt(mSRTSocket, 0, SRTO_STREAMID, srtConfig->streamId, srtConfig->streamIdLen);
        int yes = 1;
        srt_setsockopt(mSRTSocket, 0, SRTO_SENDER, &yes, sizeof yes);

        struct sockaddr_in servaddr{};
        bzero(&servaddr, sizeof(servaddr));

        servaddr.sin_family = AF_INET;
        servaddr.sin_addr.s_addr = inet_addr(srtConfig->ipAddress);
        servaddr.sin_port = htons(srtConfig->port);

        int ret = srt_connect(mSRTSocket, (struct sockaddr *) &servaddr, sizeof servaddr);

        if (ret != SRT_ERROR) {
            srt_setsockopt(mSRTSocket, 0, SRTO_MAXBW, &srtConfig->maxBW, sizeof srtConfig->maxBW);
            srt_setsockopt(mSRTSocket, 0, SRTO_INPUTBW, &srtConfig->inputBW,
                           sizeof srtConfig->inputBW);
            onOpen(false);
        } else {
            changeConnectedStatus(CONNECTED_STATUS_CONNECT_FAIL);
        }
        LOGCATE("srt_connect  srt_connect %d  %s %s ", ret, srtConfig->streamId,
                srtConfig->ipAddress);
    });
}

void SrtLiveConnection::senSRTPacket(SRTMsgPacket &packet) {
    if(!isOpen){
        return;
    }
    SRT_MSGCTRL mc = srt_msgctrl_default;
    mc.msgttl = packet.msgttl;
    mc.srctime = packet.srctime;
    mc.boundary = packet.boundary;
    int retSend = srt_sendmsg2(mSRTSocket, (packet.data), packet.dataSize, &mc);
    if (retSend <= 0) {
        sendEvent(EVENT_SEND_PACKET_FAIL, "todo");
        SRT_SOCKSTATUS sockstatus = srt_getsockstate(mSRTSocket);
        if (sockstatus > SRTS_BROKEN) {
            changeConnectedStatus(CONNECTED_STATUS_OFFLINE);
        }
    }
}

void SrtLiveConnection::close() {
    onClose();
    srt_close(mSRTSocket);
}
void SrtLiveConnection::biStats(SRT_TRACEBSTATS *status) const {
    srt_bistats(mSRTSocket, status, 1, 1);
}
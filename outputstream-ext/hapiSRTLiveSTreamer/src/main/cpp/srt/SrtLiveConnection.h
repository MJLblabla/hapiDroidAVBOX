//
// Created by 满家乐 on 2022/12/16.
//

#ifndef SRTMUXER_SRTLIVECONNECTION_H
#define SRTMUXER_SRTLIVECONNECTION_H

#include <IConnection.h>
#include "LogUtil.h"
#include "srt/srt.h"

class SrtConfig {
public:
    char *streamId = nullptr;
    int streamIdLen{};
    char *ipAddress = nullptr;
    int port{};
    int payload_size{};
    int maxBW{};
    int inputBW{};

    SrtConfig() = default;

    SrtConfig(char *sid,
              char *ip,
              int port,
              int payload_size,
              int maxBW,
              int inputBW) {
        streamIdLen = strlen(sid);
        strncpy(this->streamId, sid, streamIdLen + 1);
        strncpy(this->ipAddress, ip, strlen(ip) + 1);
        this->port = port;
        this->payload_size = payload_size;
        this->maxBW = maxBW;
        this->inputBW = inputBW;
    }

    SrtConfig(SrtConfig &&config) {
        streamIdLen = config.streamIdLen;

        (this->streamId) = std::move(config.streamId);
        (this->ipAddress) = std::move(config.ipAddress);

        this->port = config.port;
        this->payload_size = config.payload_size;
        this->maxBW = config.maxBW;
        this->inputBW = config.inputBW;

        config.streamId = nullptr;
        config.ipAddress = nullptr;
    }

    ~SrtConfig() {
        if (streamId != nullptr) {
            free(streamId);
        }
        if (ipAddress != nullptr) {
            free(ipAddress);
        }
        streamId = nullptr;
        ipAddress = nullptr;
    }
};

class SRTMsgPacket : public Packet {
public:
    int boundary = 0;
    long srctime = 0;
    int msgttl = 0;

    SRTMsgPacket() = default;

    ~SRTMsgPacket() override = default;
};

class SrtLiveConnection : public IConnection {
private:
    int PAYLOAD_SIZE = 1316;
    int PAT_PACKET_PERIOD = 40;
    SRTSOCKET mSRTSocket;
    SrtConfig *srtConfig = nullptr;
protected:
    void sendOutPacket(Packet *packet) override;

public:
    SrtLiveConnection();

    ~SrtLiveConnection() override;

    void open( SrtConfig &config);

    void senSRTPacket(int boundary, long srctime, int msgttl,  char *data, int size);

    void close();

    void biStats(SRT_TRACEBSTATS *status) const;

};


#endif //SRTMUXER_SRTLIVECONNECTION_H

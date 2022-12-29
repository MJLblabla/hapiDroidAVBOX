//
// Created by 满家乐 on 2022/12/15.
//

#ifndef MUXER_CONNECTION_ICONNECTION_H
#define MUXER_CONNECTION_ICONNECTION_H

#include "istream"
#include "BlockQueue.h"
#include "thread"
#include "LogUtil.h"


using namespace std;
typedef std::function<void(int)> ConnectCallBack;
typedef std::function<void(int, string)> EventCallBack;

enum ConnectedStatus {
    CONNECTED_STATUS_NULL = 1,
    CONNECTED_STATUS_START = 2,
    CONNECTED_STATUS_CONNECTED = 3,
    CONNECTED_STATUS_CONNECT_FAIL = 4,
    CONNECTED_STATUS_OFFLINE = 5,
    CONNECTED_STATUS_RECONNECTED = 6,
    CONNECTED_STATUS_CLOSE = 8
};

enum OutputStreamerEvent {
    EVENT_SEND_PACKET_FAIL=0,
};

enum AVPacketType {
    TYPE_VIDEO = 0,
    TYPE_AUDIO
};

class Packet {
public:
    AVPacketType packetType = TYPE_VIDEO;
    char *data = nullptr;
    int dataSize = 0;
    bool canBuffer = false;

    Packet() = default;

    Packet(const Packet &packet) {
        this->data = static_cast<char *>(malloc(packet.dataSize));
        memcpy(this->data, packet.data, packet.dataSize);
        this->dataSize = packet.dataSize;
        this->packetType = packet.packetType;
        this->canBuffer = packet.canBuffer;
    }

    virtual ~Packet() {
        if (data != nullptr) {
            free(data);
            data = nullptr;
        }
    }
};

class IConnection {
private:
    BlockQueue<Packet *> packetQueue{10};
    thread *workThread = nullptr;
protected:

    int mConnectedStatus = CONNECTED_STATUS_NULL;
    volatile bool isOpen = false;
    void changeConnectedStatus(int status) const;
    void sendEvent(int envent,string msg) const;

    virtual void sendOutPacket(Packet &packet);

    void pushPacket(Packet *packet);

    void beforeOpen();

    void onOpen(bool startLoop);

    void onClose();

public:
    ConnectCallBack connectCallBack = nullptr;
    EventCallBack enventCallBack = nullptr;

    virtual ~IConnection();

};


#endif //MUXER_CONNECTION_ICONNECTION_H

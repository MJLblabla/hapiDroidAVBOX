//
// Created by 满家乐 on 2022/12/16.
//

#ifndef RTMPLIVECONNECTION_H
#define RTMPLIVECONNECTION_H

#include <IConnection.h>
#include "LogUtil.h"
#include <librtmp/rtmp.h>
#include <librtmp/log.h>

class RTMPLiveConnection : public IConnection {
private:
    char m_OutUrl[1024] = {0};
    long mStartTime = 0;
   // volatile bool isOpen = false;
protected:
    void sendOutPacket(Packet *packet) override;

public:
    RTMP *mRtmp = nullptr;
    RTMPLiveConnection();

    ~RTMPLiveConnection() override;

    void open(const char *url);

    void sendFLVPacket(Packet &packet);

    void close();

};

#endif //RTMPLIVECONNECTION_H

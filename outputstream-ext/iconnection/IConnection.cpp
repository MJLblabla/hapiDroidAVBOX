//
// Created by 满家乐 on 2022/12/15.
//

#include "IConnection.h"

#include <utility>

void IConnection::changeConnectedStatus(int status) {
    if (connectCallBack != nullptr) {
        connectCallBack(status);
    }
}

void IConnection::sendEvent(int envent, string msg) {
    if (enventCallBack != nullptr) {
        enventCallBack(envent, msg);
    }
}

void IConnection::beforeOpen() {
    changeConnectedStatus(CONNECTED_STATUS_START);
}

void IConnection::pushPacket(Packet *packet) {
    if (mConnectedStatus == CONNECTED_STATUS_CLOSE ||
        !isOpen || packetQueue.Size() >= packetQueue.Capacity() - 1
            ) {
        delete packet;
        packet = nullptr;
    } else {
        packetQueue.PushBack(packet);
    }
}

void IConnection::onOpen(bool startLoop) {

    changeConnectedStatus(CONNECTED_STATUS_CONNECTED);
    isOpen = true;
    if (!startLoop) {
        return;
    }
    if (workThread != nullptr) {
        delete workThread;
        workThread = nullptr;
    }
    packetQueue.Reset();

    workThread = new std::thread([this] {
        while (isOpen) {
            if (mConnectedStatus == CONNECTED_STATUS_OFFLINE ||
                mConnectedStatus == CONNECTED_STATUS_CLOSE) {
                break;
            }
            Packet *packet = nullptr;
            packetQueue.PopFront(packet);
            if (packet) {
                sendOutPacket(packet);
                delete packet;
                packet = nullptr;
            }
        }

        while (!packetQueue.Empty()) {
            Packet *frame = nullptr;
            packetQueue.PopBack(frame);
            delete frame;
        }
    });
}

void IConnection::onClose() {
    isOpen = false;
    changeConnectedStatus(CONNECTED_STATUS_CLOSE);
    packetQueue.Close();
    if (workThread != nullptr) {
        workThread->join();
        delete workThread;
        workThread = nullptr;
    }
}

IConnection::~IConnection() {
    connectCallBack = nullptr;
    enventCallBack = nullptr;

    if (workThread != nullptr) {
        workThread->join();
        delete workThread;
        workThread = nullptr;
    }
}
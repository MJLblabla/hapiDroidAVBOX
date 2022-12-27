//
// Created by 1 on 2022/4/7.
//

#ifndef HAPI_JNIRTMPCONNECTION_H
#define HAPI_JNIRTMPCONNECTION_H

#include "IConnectionJNICaller.h"
#include "RTMPLiveConnection.h"

class JNIRTMPConnection {

public:
    IConnectionJNICaller jniCaller;
    RTMPLiveConnection connection;

    JNIRTMPConnection(JNIEnv *jniEnv, jobject obj) {
        jniCaller.create(jniEnv, obj);
        connection.connectCallBack = [this](int status) {
            jniCaller.callJavaConnectedStatusChange(status);
        };
        connection.enventCallBack = [this](int e,string msg){
            jniCaller.callJavaSendEvent(e,msg);
        };
    }

    ~JNIRTMPConnection() = default;
};

#endif

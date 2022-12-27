//
// Created by 1 on 2022/4/7.
//

#ifndef HAPISRT_JNISRTCONNECTION_H
#define HAPISRT_JNISRTCONNECTION_H

#include "SrtLiveConnection.h"
#include "IConnectionJNICaller.h"
#include <jni.h>

class JNISRTConnection {

public:
    SrtLiveConnection srtConnection;
    IConnectionJNICaller jniCaller;
    JNISRTConnection(JNIEnv *jniEnv, jobject obj) {
        jniCaller.create(jniEnv, obj);
        srtConnection.connectCallBack = [this](int status) {
            jniCaller.callJavaConnectedStatusChange(status);
        };
        srtConnection.enventCallBack = [this](int e,string msg){
            jniCaller.callJavaSendEvent(e,msg);
        };
    }
    ~JNISRTConnection() = default;
};

#endif //HAPISRT_JNISRTCONNECTION_H

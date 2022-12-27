//
// Created by 满家乐 on 2022/12/16.
//

#ifndef MUXER_CONNECTION_ICONNECTIONJNICALLER_H
#define MUXER_CONNECTION_ICONNECTIONJNICALLER_H

#include <jni.h>
#include "istream"
#include "LogUtil.h"

class IConnectionJNICaller {
private:
    JNIEnv *getJNIEnv(bool *isAttach) const;
public:
    JavaVM *mJavaVM = nullptr;
    jobject mJavaObj = nullptr;

    void callJavaConnectedStatusChange(int status);
    void callJavaSendEvent(int event, const std::string& msg);

    void create(JNIEnv *env, jobject jConnection);


    ~IConnectionJNICaller();
};

#endif //MUXER_CONNECTION_ICONNECTIONJNICALLER_H

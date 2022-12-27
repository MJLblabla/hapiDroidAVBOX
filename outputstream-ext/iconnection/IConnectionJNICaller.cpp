//
// Created by 满家乐 on 2022/12/16.
//

#include "IConnectionJNICaller.h"


void IConnectionJNICaller::create(JNIEnv *env, jobject jConnection) {
    env->GetJavaVM(&mJavaVM);
    mJavaObj = env->NewGlobalRef(jConnection);
}

JNIEnv *IConnectionJNICaller::getJNIEnv(bool *isAttach) const {
    JNIEnv *env;
    int status;
    if (nullptr == mJavaVM) {
        LOGCATE("FFMediaPlayer::GetJNIEnv m_JavaVM == nullptr");
        return nullptr;
    }
    *isAttach = false;
    status = mJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);
    if (status != JNI_OK) {
        status = mJavaVM->AttachCurrentThread(&env, nullptr);
        if (status != JNI_OK) {
            LOGCATE("FFMediaPlayer::GetJNIEnv failed to attach current thread");
            return nullptr;
        }
        *isAttach = true;
    }
    return env;
}

void IConnectionJNICaller::callJavaConnectedStatusChange(int status) {

    bool isAttach = false;
    JNIEnv *env = this->getJNIEnv(&isAttach);
    jmethodID callJavaStatusChange = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                   "jniConnectedStatusChange", "(I)V");
    env->CallVoidMethod(this->mJavaObj, callJavaStatusChange, status);

}
void IConnectionJNICaller::callJavaSendEvent(int event, const std::string& msg){
    bool isAttach = false;
    JNIEnv *env = this->getJNIEnv(&isAttach);
    jmethodID callJavaStatusChange = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                      "jniMsgCall", "(ILjava/lang/String;)V");
    jstring jmsg = env->NewStringUTF(msg.c_str());
    env->CallVoidMethod(this->mJavaObj, callJavaStatusChange, event, jmsg);
    env->DeleteLocalRef(jmsg);
}

IConnectionJNICaller::~IConnectionJNICaller() {
    bool isAttach = false;
    getJNIEnv(&isAttach)->DeleteGlobalRef(mJavaObj);
    if (isAttach)
        mJavaVM->DetachCurrentThread();
}

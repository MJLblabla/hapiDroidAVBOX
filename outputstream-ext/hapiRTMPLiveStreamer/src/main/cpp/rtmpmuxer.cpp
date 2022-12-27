#include <jni.h>
#include <string>
#include "JNIRTMPConnection.h"
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_hapi_rtmplive_RTMPConnection_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
//
extern "C"
JNIEXPORT jlong JNICALL
Java_com_hapi_rtmplive_RTMPConnection_native_1init(JNIEnv *env, jobject thiz) {
    auto *connection = new JNIRTMPConnection(env, thiz);
    return reinterpret_cast<jlong>(connection);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_rtmplive_RTMPConnection_native_1uninit(JNIEnv *env, jobject thiz, jlong handler) {
    auto *connection = reinterpret_cast<JNIRTMPConnection *>(handler);
    if(connection== nullptr){
        return;
    }
    delete connection;
    connection = nullptr;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_rtmplive_RTMPConnection_native_1open(JNIEnv *env, jobject thiz, jlong handler,
                                                    jstring url) {
    auto *connection = reinterpret_cast<JNIRTMPConnection *>(handler);
    const char *curl = env->GetStringUTFChars(url, nullptr);
    connection->connection.open(curl);
    env->ReleaseStringUTFChars(url, curl);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_rtmplive_RTMPConnection_native_1close(JNIEnv *env, jobject thiz, jlong handler) {
    auto *connection = reinterpret_cast<JNIRTMPConnection *>(handler);
    connection->connection.close();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_rtmplive_RTMPConnection_native_1sendPacket(JNIEnv *env, jobject thiz, jlong handler,
                                                          jobject packet, jint offset, jint limit) {

    auto *connection = reinterpret_cast<JNIRTMPConnection *>(handler);
    char *buf = (char *) env->GetDirectBufferAddress(packet);

    Packet avPacket;
    avPacket.dataSize = limit - offset;
    avPacket.data = &buf[offset];
    connection->connection.sendFLVPacket( avPacket);
    avPacket.data = nullptr;
}
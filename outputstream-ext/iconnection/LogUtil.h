
#ifndef HAPI_LOGUTIL_H
#define HAPI_LOGUTIL_H

#include<android/log.h>
#include <jni.h>

#define  LOG_TAG "hapi"
#define LOG_ABLE  1
#define  LOGCATE(...) {  if(LOG_ABLE){ __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__); }}
#define  LOGCATV(...) {  if(LOG_ABLE){ __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__);}}
#define  LOGCATD(...) { if(LOG_ABLE){ __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__);}}
#define  LOGCATI(...) { if(LOG_ABLE){ __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);}}

//class LogUtil{
//
//public:
//    static int logAble = 1;
//
//    static void logD(const char* tag, const char* fmt, ...){
//        if(logAble){
//            __android_log_print(ANDROID_LOG_ERROR,tag,__VA_ARGS__);
//        }
//        }
//};
//

#endif //HAPI_LOGUTIL_H

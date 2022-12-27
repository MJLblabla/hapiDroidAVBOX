
#ifndef HAPI_LOGUTIL_H
#define HAPI_LOGUTIL_H

#include<android/log.h>
#include <sys/time.h>
#include <time.h>

#define  LOG_TAG "hapirecorder"
#define LOG_ABLE  1
#define  LOGCATE(...) {  if(LOG_ABLE){ __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__); }}
#define  LOGCATV(...) {  if(LOG_ABLE){ __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__);}}
#define  LOGCATD(...) { if(LOG_ABLE){ __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__);}}
#define  LOGCATI(...) { if(LOG_ABLE){ __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);}}

#define ByteFlowPrintE LOGCATE
#define ByteFlowPrintV LOGCATV
#define ByteFlowPrintD LOGCATD
#define ByteFlowPrintI LOGCATI

#define FUN_BEGIN_TIME(FUN) {\
    LOGCATE("%s:%s func start", __FILE__, FUN); \
    long long t0 = GetSysCurrentTime();

#define FUN_END_TIME(FUN) \
    long long t1 = GetSysCurrentTime(); \
    LOGCATE("%s:%s func cost time %ldms", __FILE__, FUN, (long)(t1-t0));}

#define BEGIN_TIME(FUN) {\
    LOGCATE("%s func start", FUN); \
    long long t0 = GetSysCurrentTime();

#define END_TIME(FUN) \
    long long t1 = GetSysCurrentTime(); \
    LOGCATE("%s func cost time %ldms", FUN, (long)(t1-t0));}

static long long GetSysCurrentTime() {
    struct timeval time;
    gettimeofday(&time, NULL);
    long long curTime = ((long long) (time.tv_sec)) * 1000 + time.tv_usec / 1000;
    return curTime;
}

static long long GetSysCurrentTimeNS() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

#define GO_CHECK_GL_ERROR(...)   LOGCATE("CHECK_GL_ERROR %s glGetError = %d, line = %d, ",  __FUNCTION__, glGetError(), __LINE__)

#define DEBUG_LOGCATE(...) LOGCATE("DEBUG_LOGCATE %s line = %d",  __FUNCTION__, __LINE__)

#endif //HAPI_LOGUTIL_H

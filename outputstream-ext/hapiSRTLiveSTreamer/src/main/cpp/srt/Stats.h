
#include "Models.h"
#include "srt/srt.h"
#include "LogUtil.h"
class Stats {
public:
    static jobject getJava(JNIEnv *env, SRT_TRACEBSTATS tracebstats) {
        jclass statsClazz = env->FindClass(STATS_CLASS);
        if (!statsClazz) {

            LOGCATE("%s %dCan't find Srt Stats class",
                    __FUNCTION__, __LINE__);
            return nullptr;
        }

        jmethodID statsConstructorMethod = env->GetMethodID(statsClazz, "<init>",
                                                            "(JJJIIIIIIIJIIIJJJJJJJJJIIIIIIIIDDJIDJIIIJJJJJJJDIIIDDIIDIIIIIIIIIIIIIIIIIIJJJJJJJJ)V");
        if (!statsConstructorMethod) {
            LOGCATE("%s %dCan't find Srt Stats class",
                    __FUNCTION__, __LINE__);
            env->DeleteLocalRef(statsClazz);
            return nullptr;
        }

        jobject srtSocket = env->NewObject(statsClazz, statsConstructorMethod,
                                           tracebstats.msTimeStamp,
                                           tracebstats.pktSentTotal,
                                           tracebstats.pktRecvTotal,
                                           tracebstats.pktSndLossTotal,
                                           tracebstats.pktRcvLossTotal,
                                           tracebstats.pktRetransTotal,
                                           tracebstats.pktSentACKTotal,
                                           tracebstats.pktRecvACKTotal,
                                           tracebstats.pktSentNAKTotal,
                                           tracebstats.pktRecvNAKTotal,
                                           tracebstats.usSndDurationTotal,

                                           tracebstats.pktSndDropTotal,
                                           tracebstats.pktRcvDropTotal,
                                           tracebstats.pktRcvUndecryptTotal,
                                           (jlong) tracebstats.byteSentTotal,
                                           (jlong) tracebstats.byteRecvTotal,
                                           (jlong) tracebstats.byteRcvLossTotal,
                                           (jlong) tracebstats.byteRetransTotal,
                                           (jlong) tracebstats.byteSndDropTotal,
                                           (jlong) tracebstats.byteRcvDropTotal,
                                           (jlong) tracebstats.byteRcvUndecryptTotal,
                                           tracebstats.pktSent,
                                           tracebstats.pktRecv,
                                           tracebstats.pktSndLoss,
                                           tracebstats.pktRcvLoss,
                                           tracebstats.pktRetrans,
                                           tracebstats.pktRcvRetrans,
                                           tracebstats.pktSentACK,
                                           tracebstats.pktRecvACK,
                                           tracebstats.pktSentNAK,
                                           tracebstats.pktRecvNAK,
                                           tracebstats.mbpsSendRate,
                                           tracebstats.mbpsRecvRate,
                                           tracebstats.usSndDuration,
                                           tracebstats.pktReorderDistance,
                                           tracebstats.pktRcvAvgBelatedTime,
                                           tracebstats.pktRcvBelated,

                                           tracebstats.pktSndDrop,
                                           tracebstats.pktRcvDrop,
                                           tracebstats.pktRcvUndecrypt,
                                           (jlong) tracebstats.byteSent,
                                           (jlong) tracebstats.byteRecv,
                                           (jlong) tracebstats.byteRcvLoss,
                                           (jlong) tracebstats.byteRetrans,
                                           (jlong) tracebstats.byteSndDrop,
                                           (jlong) tracebstats.byteRcvDrop,
                                           (jlong) tracebstats.byteRcvUndecrypt,

                                           tracebstats.usPktSndPeriod,
                                           tracebstats.pktFlowWindow,
                                           tracebstats.pktCongestionWindow,
                                           tracebstats.pktFlightSize,
                                           tracebstats.msRTT,
                                           tracebstats.mbpsBandwidth,
                                           tracebstats.byteAvailSndBuf,
                                           tracebstats.byteAvailRcvBuf,

                                           tracebstats.mbpsMaxBW,
                                           tracebstats.byteMSS,

                                           tracebstats.pktSndBuf,
                                           tracebstats.byteSndBuf,
                                           tracebstats.msSndBuf,
                                           tracebstats.msSndTsbPdDelay,

                                           tracebstats.pktRcvBuf,
                                           tracebstats.byteRcvBuf,
                                           tracebstats.msRcvBuf,
                                           tracebstats.msRcvTsbPdDelay,

                                           tracebstats.pktSndFilterExtraTotal,
                                           tracebstats.pktRcvFilterExtraTotal,
                                           tracebstats.pktRcvFilterSupplyTotal,
                                           tracebstats.pktRcvFilterLossTotal,

                                           tracebstats.pktSndFilterExtra,
                                           tracebstats.pktRcvFilterExtra,
                                           tracebstats.pktRcvFilterSupply,
                                           tracebstats.pktRcvFilterLoss,
                                           tracebstats.pktReorderTolerance,

                                           (jlong) tracebstats.pktSentUniqueTotal,
                                           (jlong) tracebstats.pktRecvUniqueTotal,
                                           (jlong) tracebstats.byteSentUniqueTotal,
                                           (jlong) tracebstats.byteRecvUniqueTotal,

                                           (jlong) tracebstats.pktSentUnique,
                                           (jlong) tracebstats.pktRecvUnique,
                                           (jlong) tracebstats.byteSentUnique,
                                           (jlong) tracebstats.byteRecvUnique
        );

        env->DeleteLocalRef(statsClazz);

        return srtSocket;
    }
};
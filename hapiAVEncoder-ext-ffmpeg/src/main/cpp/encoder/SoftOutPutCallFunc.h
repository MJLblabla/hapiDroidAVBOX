//
// Created by 满家乐 on 2022/11/30.
//

#ifndef AVENCODER_SOFTOUTPUTCALLFUNC_H
#define AVENCODER_SOFTOUTPUTCALLFUNC_H
extern "C"
{
#include <libavcodec/avcodec.h>
#include "libavutil/opt.h"
}
using namespace std;

typedef std::function<void(AVPacket *, AVCodecContext *)> OutPutCallFunc;



#endif //AVENCODER_SOFTOUTPUTCALLFUNC_H

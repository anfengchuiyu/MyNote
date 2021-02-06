//
// Created by zhe on 2021/1/11.
//

#include <stdlib.h>
#include <cstring>
#include <pthread.h>
#include "com_example_medialearn_LivePush.h"
#include "PushPacketQueue.h"
#include "AndroidLogUtil.h"


char *rtmpUrl = NULL;
PushPacketQueue *packetQueue = NULL;
RTMP *rtmp = NULL;
bool isPushing = false;
pthread_t connectPthreadTid;
uint32_t startTime;

static JNIEnv *mEnv = NULL;
jobject j_livepush_intance;
jmethodID connectSuccessMid;
jmethodID connectErrorMid;

int register_native_livepush(JNIEnv *env) {
    /*if (mEnv == NULL) {
        mEnv = env;
    }*/

}


void connsuccess() {
    /*JavaVM *vm;
    mEnv->GetJavaVM(&vm);

    JNIEnv *env;
    vm->AttachCurrentThread(&env, 0);
    env->CallVoidMethod(j_livepush_intance, connectSuccessMid);
    vm->DetachCurrentThread();*/
}

void connerror(int code) {
    /*JavaVM *vm;
    mEnv->GetJavaVM(&vm);

    JNIEnv *env;
    vm->AttachCurrentThread(&env, 0);
    env->CallVoidMethod(j_livepush_intance, connectErrorMid, code);
    vm->DetachCurrentThread();*/
}


void *connect(void *context) {
    printf("rtmpUrl=%s\n", rtmpUrl);
    rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);

    rtmp->Link.timeout = 5;
    rtmp->Link.lFlags |= RTMP_LF_LIVE;
    RTMP_SetupURL(rtmp, rtmpUrl);
    RTMP_EnableWrite(rtmp);

    if (!RTMP_Connect(rtmp, NULL)) {
        LOGE("RTMP_Connect failed!");
        connerror(-1);
        return (void *) -1;
    }

    if (!RTMP_ConnectStream(rtmp, 0)) {
        LOGE("RTMP_ConnectStream failed!");
        connerror(-2);
        return (void *) -1;
    }

    LOGD("rtmp connect success!");
    connsuccess();

    isPushing = true;
    startTime = RTMP_GetTime();
    while (isPushing) {
        RTMPPacket *packet = packetQueue->pop();
        if (packet) {
            RTMP_SendPacket(rtmp, packet, 1);
            RTMPPacket_Free(packet);
            free(packet);
        }
    }

    LOGD("rtmp push stoped!");
    return 0;
}

void pushSpsPps(jbyte *spsData, jint spsLen, jbyte *ppsData, jint ppsLen) {
    // frame type : 1关键帧，2 非关键帧 (4bit)
    // CodecID : 7表示 AVC (4bit)  , 与 frame type 组合起来刚好是 1 个字节  0x17
    // fixed : 0x00 0x00 0x00 0x00 (4byte)
    // configurationVersion  (1byte)  0x01版本
    // AVCProfileIndication  (1byte)  sps[1] profile
    // profile_compatibility (1byte)  sps[2] compatibility
    // AVCLevelIndication    (1byte)  sps[3] Profile level
    // lengthSizeMinusOne    (1byte)  0xff   包长数据所使用的字节数

    // sps + pps 的数据
    // sps number            (1byte)  0xe1   sps 个数
    // sps data length       (2byte)  sps 长度
    // sps data                       sps 的内容
    // pps number            (1byte)  0x01   pps 个数
    // pps data length       (2byte)  pps 长度
    // pps data                       pps 的内容

    // 数据的长度（大小） = sps 大小 + pps 大小 + 16字节
    int bodySize = spsLen + ppsLen + 16;
    //构建 RTMPPacket
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    // 构建 body 按上面的一个一个开始赋值
    char *body = packet->m_body;
    int index = 0;
    body[index++] = 0x17; //frametye and AVC

    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;

    body[index++] = 0x01;
    body[index++] = spsData[1];
    body[index++] = spsData[2];
    body[index++] = spsData[3];
    body[index++] = 0xff;

    body[index++] = 0xe1;
    body[index++] = (spsLen >> 8) & 0xFF;
    body[index++] = spsLen & 0xFF;
    memcpy(&body[index++], spsData, spsLen);
    index += spsLen;


    body[index++] = (ppsLen >> 8) & 0xFF;
    body[index++] = ppsLen & 0xFF;
    memcpy(&body[index++], ppsData, ppsLen);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x04;
    packet->m_nInfoField2 = rtmp->m_stream_id;


    packetQueue->push(packet);
}

void pushVideo(jbyte *videoData, jint dataLen, jboolean keyFrame) {
    // frame type : 1关键帧，2 非关键帧 (4bit)
    // CodecID : 7表示 AVC (4bit)  , 与 frame type 组合起来刚好是 1 个字节  0x17
    // fixed : 0x01 0x00 0x00 0x00 (4byte)  0x01  表示 NALU 单元

    // video data length       (4byte)  video 长度
    // video data
    // 数据的长度（大小） =  dataLen + 9
    int bodySize = dataLen + 9;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, dataLen);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    int index = 0;
    if (keyFrame) {
        body[index++] = 0x17;
    } else {
        body[index++] = 0x27;
    }

    // fixed : 0x01 0x00 0x00 0x00 (4byte)  0x01  表示 NALU 单元
    body[index++] = 0x01;
    body[index++] = 0x00;
    body[index++] = 0x00;
    body[index++] = 0x00;

    // video data length       (4byte)  video 长度
    body[index++] = (dataLen >> 24) & 0xFF;
    body[index++] = (dataLen >> 16) & 0xFF;
    body[index++] = (dataLen >> 8) & 0xFF;
    body[index++] = dataLen & 0xFF;
    // video data
    memcpy(&body[index++], videoData, dataLen);

    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x04;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    packetQueue->push(packet);
}

void pushAudio(jbyte *audioData, jint dataLen) {
    // 2 字节头信息
    // 前四位表示音频数据格式 AAC  10  ->  1010  ->  A
    // 五六位表示采样率 0 = 5.5k  1 = 11k  2 = 22k  3(11) = 44k
    // 七位表示采样采样的精度 0 = 8bits  1 = 16bits
    // 八位表示音频类型  0 = mono  1 = stereo
    // 我们这里算出来第一个字节是 0xAF   1010   11 11

    int bodySize = dataLen + 2;

    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    int index = 0;

    //1010  11     1        1    -->转为16进制  0xAF
    //aac   44k    16bits stereo
    body[index++] = 0xAF;
    // 0x01 代表 aac 原始数据
    body[index++] = 0x01;
    memcpy(&body[index++], audioData, dataLen);

    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x04;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    packetQueue->push(packet);
}

void stop() {
    isPushing = false;
    if (packetQueue) {
        packetQueue->notify();
    }
    if (connectPthreadTid) {
        pthread_join(connectPthreadTid, NULL);
    }
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_startPush
        (JNIEnv *env, jobject jobj, jstring j_url) {
    const char *url = env->GetStringUTFChars(j_url, NULL);
    rtmpUrl = (char *) malloc(strlen(url) + 1);
    strcpy(rtmpUrl, url);
    env->ReleaseStringUTFChars(j_url, url);

    packetQueue = new PushPacketQueue();
    pthread_create(&connectPthreadTid, NULL, connect, NULL);

    j_livepush_intance = env->NewGlobalRef(jobj);
    jclass claz = env->GetObjectClass(jobj);
    connectSuccessMid = env->GetMethodID(claz, "nativeCall_ConnectSuccess", "()V");
    connectErrorMid = env->GetMethodID(claz, "nativeCall_ConnectError", "(Ljava/lang/String;)V");
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_stopPush
        (JNIEnv *env, jobject jobj) {
    stop();
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_setVideoSpsPps
        (JNIEnv *env, jobject jobj, jbyteArray spsData_, jint spsLen, jbyteArray ppsData_,
         jint ppsLen) {
    LOGD("setVideoSpsPps: spsLen=%d, spsLen=%d", spsLen, ppsLen);
    jbyte *spsData = env->GetByteArrayElements(spsData_, NULL);
    jbyte *ppsData = env->GetByteArrayElements(ppsData_, NULL);
    pushSpsPps(spsData, spsLen, ppsData, ppsLen);
    env->ReleaseByteArrayElements(spsData_, spsData, 0);
    env->ReleaseByteArrayElements(ppsData_, ppsData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_sendVideoPacket
        (JNIEnv *env, jobject instance, jbyteArray videoData_, jint dataLen, jboolean keyFrame) {
    LOGD("sendVideoPacket dataLen=%d", dataLen);
    jbyte *videoData = env->GetByteArrayElements(videoData_, NULL);
    pushVideo(videoData, dataLen, keyFrame);
    env->ReleaseByteArrayElements(videoData_, videoData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_sendAudioPacket
        (JNIEnv *env, jobject instance, jbyteArray audioData_, jint dataLen) {
    LOGD("sendAudioPacket dataLen=%d", dataLen);
    jbyte *audioData = env->GetByteArrayElements(audioData_, NULL);
    pushAudio(audioData, dataLen);
    env->ReleaseByteArrayElements(audioData_, audioData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_medialearn_LivePush_release
        (JNIEnv *env, jobject instance) {
    if (isPushing) {
        stop();
    }
    if (rtmp) {
        RTMP_DeleteStream(rtmp);
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = NULL;
    }
    if (rtmpUrl) {
        free(rtmpUrl);
        rtmpUrl = NULL;
    }
    if (packetQueue) {
        packetQueue->clear();
        delete packetQueue;
        packetQueue = NULL;
    }
}
//
// Created by zhe on 2021/1/19.
//

#ifndef MY_APPLICATION_PUSHPACKETQUEUE_H
#define MY_APPLICATION_PUSHPACKETQUEUE_H

#include <queue>
#include <pthread.h>
#include <malloc.h>

extern "C" {
#include <rtmp.h>
};


class PushPacketQueue {
public:
    std::queue<RTMPPacket *> *pPacketQueue;
    pthread_mutex_t packetMutex;
    pthread_cond_t packetCond;

public:
    PushPacketQueue();

    ~PushPacketQueue();

    void push(RTMPPacket *packet);

    RTMPPacket *pop();

    void clear();

    void notify();

};


#endif //MY_APPLICATION_PUSHPACKETQUEUE_H

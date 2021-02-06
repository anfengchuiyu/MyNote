//
// Created by zhe on 2021/1/19.
//

#include "PushPacketQueue.h"

PushPacketQueue::PushPacketQueue() {
    pPacketQueue = new std::queue<RTMPPacket *>();
    pthread_mutex_init(&packetMutex, NULL);
    pthread_cond_init(&packetCond, NULL);
}

PushPacketQueue::~PushPacketQueue() {
    if (pPacketQueue) {
        clear();
        delete (pPacketQueue);
        pPacketQueue = NULL;
    }
    pthread_mutex_destroy(&packetMutex);
    pthread_cond_destroy(&packetCond);
}

void PushPacketQueue::push(RTMPPacket *packet) {
    pthread_mutex_lock(&packetMutex);

    pPacketQueue->push(packet);
    pthread_cond_signal(&packetCond);

    pthread_mutex_unlock(&packetMutex);
}

RTMPPacket *PushPacketQueue::pop() {
    RTMPPacket *packet = NULL;
    pthread_mutex_lock(&packetMutex);

    if (pPacketQueue->empty()) {
        pthread_cond_wait(&packetCond, &packetMutex);
    } else {
        packet = pPacketQueue->front();
        pPacketQueue->pop();
    }

    pthread_mutex_unlock(&packetMutex);
    return packet;
}

void PushPacketQueue::clear() {
    pthread_mutex_lock(&packetMutex);

    while (!pPacketQueue->empty()) {
        RTMPPacket *packet = pPacketQueue->front();
        pPacketQueue->pop();
        RTMPPacket_Free(packet);
        free(packet);
    }

    pthread_mutex_unlock(&packetMutex);
}

void PushPacketQueue::notify() {
    pthread_mutex_lock(&packetMutex);
    pthread_cond_signal(&packetCond);
    pthread_mutex_unlock(&packetMutex);
}

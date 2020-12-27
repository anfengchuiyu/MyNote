//
// Created by zhe on 2020/12/27.
//

#ifndef MY_APPLICATION_LOOPER_H
#define MY_APPLICATION_LOOPER_H

#include <pthread.h>
#include <sys/types.h>
#include <semaphore.h>

struct LooperMessage {
    int what;
    int arg1;
    int arg2;
    void *obj;
    LooperMessage *next;
    bool quit;
};

class Looper {
public:
    Looper();

    virtual ~Looper();

    void postMessage(int what, bool flush = false);

    void postMessage(int what, void *obj, bool flush = false);

    void postMessage(int what, int arg1, int arg2, bool flush = false);

    void postMessage(int what, int arg1, int arg2, void *obj, bool flush = false);

    void quit();

    virtual void handleMessage(LooperMessage *msg);

private:
    void addMessage(LooperMessage *msg, bool flush);

    static void *trampoline(void *p);

    void loop(void);

    LooperMessage *head;
    pthread_t worker;
    sem_t headwriteprotect;
    sem_t headdataavailable;
    bool running;

};


#endif //MY_APPLICATION_LOOPER_H

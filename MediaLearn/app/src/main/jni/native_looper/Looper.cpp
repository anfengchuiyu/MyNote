//
// Created by zhe on 2020/12/27.
//

#include "Looper.h"

void *Looper::trampoline(void *p) {
    ((Looper *) p)->loop();
    return NULL;
}

Looper::Looper() {
    head = NULL;
    sem_init(&headdataavailable, 0, 0);
    sem_init(&headwriteprotect, 0, 1);
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_create(&worker, &attr, trampoline, this);
    running = true;
}

Looper::~Looper() {
    if (running) {
        quit();
    }
}

void Looper::postMessage(int what, bool flush) {
    postMessage(what, 0, 0, NULL, flush);
}

void Looper::postMessage(int what, void *obj, bool flush) {
    postMessage(what, 0, 0, obj, flush);
}

void Looper::postMessage(int what, int arg1, int arg2, bool flush) {
    postMessage(what, arg1, arg2, NULL, flush);
}

void Looper::postMessage(int what, int arg1, int arg2, void *obj, bool flush) {
    LooperMessage *msg = new LooperMessage();
    msg->what = what;
    msg->arg1 = arg1;
    msg->arg2 = arg2;
    msg->obj = obj;
    msg->next = NULL;
    msg->quit = false;
    addMessage(msg, flush);
}

void Looper::addMessage(LooperMessage *msg, bool flush) {
    sem_wait(&headwriteprotect);
    LooperMessage *h = head;
    if (flush) {
        while (h) {
            LooperMessage *next = h->next;
            delete h;
            h = next;
        }
        h = NULL;
    }
    if (h) {
        while (h->next) {
            h = h->next;
        }
        h->next = msg;
    } else {
        head = msg;
    }

    sem_post(&headwriteprotect);
    sem_post(&headdataavailable);
}

void Looper::loop() {
    while (true) {
        sem_wait(&headdataavailable);
        sem_wait(&headwriteprotect);

        LooperMessage *msg = head;
        if (msg == NULL) {
            sem_post(&headwriteprotect);
            continue;
        }
        head = msg->next;
        sem_post(&headwriteprotect);

        if (msg->quit) {
            delete msg;
            return;
        }

        handleMessage(msg);
        delete msg;
    }
}

void Looper::quit() {
    LooperMessage *msg = new LooperMessage();
    msg->what = 0;
    msg->obj = NULL;
    msg->next = NULL;
    msg->quit = true;
    addMessage(msg, false);
    void *retval;
    pthread_join(worker, &retval);
    sem_destroy(&headdataavailable);
    sem_destroy(&headwriteprotect);
    running = false;
}
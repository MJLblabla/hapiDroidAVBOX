//
// Created by 满家乐 on 2022/12/7.
//

#ifndef MUXER_CONNECTION_BLOCKQUEUE_H
#define MUXER_CONNECTION_BLOCKQUEUE_H

#include <mutex>
#include <deque>
#include <mutex>
#include <deque>
#include <condition_variable>

template<typename T>
class BlockQueue {
public:
    BlockQueue(int MaxSize);

    ~BlockQueue();

    bool Empty();

    bool Full();

    int Size();

    int Capacity();

    T Front();

    T Back();

    void PushFront(const T &item);

    void PushBack(const T &item);

    bool PopFront(T &item);

    bool PopBack(T &item);

    void Flush();

    void Close();

    void Reset();

private:
    bool m_isClose;
    int m_maxSize;
    std::deque <T> m_deque;
    std::mutex m_mutex;
    std::condition_variable m_noFullCondVar;
    std::condition_variable m_noEmptyCondVar;
};


#include "BlockQueue.h"

template<typename T>
BlockQueue<T>::BlockQueue(int MaxSize) :
        m_isClose(false),
        m_maxSize(MaxSize) {
}

template<typename T>
BlockQueue<T>::~BlockQueue() {
    Close();
}

template<typename T>
void BlockQueue<T>::Close() {
    {
        std::unique_lock <std::mutex> locker(m_mutex);
        m_isClose = true;
    }
    m_noFullCondVar.notify_all();
    m_noEmptyCondVar.notify_all();
}

template<typename T>
void BlockQueue<T>::Reset(){
    m_isClose = false;
}

template<typename T>
bool BlockQueue<T>::Empty() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_deque.empty();
}

template<typename T>
bool BlockQueue<T>::Full() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_deque.empty() >= m_maxSize;
}


template<typename T>
int BlockQueue<T>::Size() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_deque.size();
}

template<typename T>
int BlockQueue<T>::Capacity() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_maxSize;
}

template<typename T>
T BlockQueue<T>::Front() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_deque.front();
}

template<typename T>
T BlockQueue<T>::Back() {
    std::unique_lock <std::mutex> locker(m_mutex);
    return m_deque.back();
}

template<typename T>
void BlockQueue<T>::PushFront(const T &item) {
    std::unique_lock <std::mutex> locker(m_mutex);
    while (m_deque.size() >= m_maxSize) {
        m_noFullCondVar.wait(locker);
    }
    m_deque.emplace_front(item);
    m_noEmptyCondVar.notify_one();
}

template<typename T>
void BlockQueue<T>::PushBack(const T &item) {
    std::unique_lock <std::mutex> locker(m_mutex);
    while (m_deque.size() >= m_maxSize) {
        m_noFullCondVar.wait(locker);
    }
    m_deque.emplace_back(item);
    m_noEmptyCondVar.notify_one();
}

template<typename T>
bool BlockQueue<T>::PopFront(T &item) {
    std::unique_lock <std::mutex> locker(m_mutex);
    while (m_deque.empty()) {
        m_noEmptyCondVar.wait(locker);
        if (m_isClose) return false;
    }

    item = m_deque.front();
    m_deque.pop_front();
    m_noFullCondVar.notify_one();
    return true;
}

template<typename T>
bool BlockQueue<T>::PopBack(T &item) {
    std::unique_lock <std::mutex> locker(m_mutex);
    while (m_deque.empty()) {
        m_noEmptyCondVar.wait(locker);
        if (m_isClose) return false;
    }

    item = m_deque.back();
    m_deque.pop_back();
    m_noFullCondVar.notify_one();
    return true;
}

template<typename T>
void BlockQueue<T>::Flush() {
    std::unique_lock <std::mutex> locker(m_mutex);
    m_noEmptyCondVar.notify_one();
}
#endif //MY_APPLICATION_BLOCKQUEUE_H

/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cobar.net.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xianmao.hexm
 * ByteBuffer的队列
 */
public final class BufferQueue {

    private int takeIndex;						/* 下一个从队列中拿的下标 */
    private int putIndex;							/* 下一个放byteBuffer的项的下标	*/ 
    private int count;								/* items中非null的项个数	*/
    private final ByteBuffer[] items;	/* 保存缓存 							*/
    private final ReentrantLock lock;	/* lock 									*/
    private final Condition notFull;	/* 用于在full的情况下，等待	*/
    private ByteBuffer attachment;		/* 用于写操作的时候，这里记录上次没有处理完的buffer */

    public BufferQueue(int capacity) {
        items = new ByteBuffer[capacity];
        lock = new ReentrantLock();
        notFull = lock.newCondition();
    }

    public ByteBuffer attachment() {
        return attachment;
    }

    public void attach(ByteBuffer buffer) {
        this.attachment = buffer;
    }

    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public void put(ByteBuffer buffer) throws InterruptedException {
        final ByteBuffer[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            try {
                while (count == items.length) {
                    notFull.await();	/* 等待的么 */
                }
            } catch (InterruptedException ie) {
                notFull.signal();
                throw ie;
            }
            insert(buffer);
        } finally {
            lock.unlock();
        }
    }

    public ByteBuffer poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == 0) {
                return null;
            }
            return extract();
        } finally {
            lock.unlock();
        }
    }

    private void insert(ByteBuffer buffer) {
        items[putIndex] = buffer;
        putIndex = inc(putIndex);
        ++count;
    }

    private ByteBuffer extract() {
        final ByteBuffer[] items = this.items;
        ByteBuffer buffer = items[takeIndex];
        items[takeIndex] = null;
        takeIndex = inc(takeIndex);
        --count;
        notFull.signal();
        return buffer;
    }

    private int inc(int i) {
        return (++i == items.length) ? 0 : i;
    }

}

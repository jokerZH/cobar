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
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xianmao.hexm
 * 实现一点大小的ByteBuffer的缓存池，如果超过大小，就从系统分配
 */
public final class BufferPool {

    private final int chunkSize;			/* 单个buffer的大小 */
    private final ByteBuffer[] items;	/* 存放buffer的数组 */
    private final ReentrantLock lock;	/* lock */
    private int putIndex;							/* 当前insert的下标 */
    private int takeIndex;						/* 下一个返回的空闲下标 */
    private int count;								/* buffer的个数 */
    private volatile int newCount;		/* 由于开始创建的buffer不够，新建的buffer */

    public BufferPool(int bufferSize, int chunkSize) {
        this.chunkSize = chunkSize;
        int capacity = bufferSize / chunkSize;
        capacity = (bufferSize % chunkSize == 0) ? capacity : capacity + 1;
        this.items = new ByteBuffer[capacity];
        this.lock = new ReentrantLock();
        for (int i = 0; i < capacity; i++) {
            insert(create(chunkSize));
        }
    }

    public int capacity() {
        return items.length;
    }

    public int size() {
        return count;
    }

    public int getNewCount() {
        return newCount;
    }

		/* 获得一个缓存 */
    public ByteBuffer allocate() {
        ByteBuffer node = null;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            node = (count == 0) ? null : extract();
        } finally {
            lock.unlock();
        }
        if (node == null) {
            ++newCount;
            return create(chunkSize);
        } else {
            return node;
        }
    }

		/* 真正的释放还是要靠gc， 这里保证一定缓存大小，不会反复的申请释放 */
    public void recycle(ByteBuffer buffer) {
        // 拒绝回收null和容量大于chunkSize的缓存
        if (buffer == null || buffer.capacity() > chunkSize) {
            return;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count != items.length) {
                buffer.clear();
                insert(buffer);
            }
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
        ByteBuffer item = items[takeIndex];
        items[takeIndex] = null;
        takeIndex = inc(takeIndex);
        --count;
        return item;
    }

    private int inc(int i) {
        return (++i == items.length) ? 0 : i;
    }

    private ByteBuffer create(int size) {
        return ByteBuffer.allocate(size);
    }

}

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
package com.alibaba.cobar.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.alibaba.cobar.config.ErrorCode;
import com.alibaba.cobar.util.TimeUtil;

/**
 * @author xianmao.hexm
 */
public abstract class BackendConnection extends AbstractConnection {

    protected long id;
    protected String host;		/* 服务端ip 	*/
    protected int port;				/* 服务端port	*/
    protected int localPort;	/* 本地port		*/
    protected long idleTimeout;	/* 空闲时间超过这个时间就判断连接为空闲 */
    protected NIOConnector connector;		/* 建立连接的对象，processor也由这个对象分配 */
    protected NIOHandler handler;				/* 读取数据的时候的处理对象, 开始auth，然后auth中会改成具体的操作的 */
    protected boolean isFinishConnect;	/* 是否完成连接 */

    public BackendConnection(SocketChannel channel) {
        super(channel);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public boolean isIdleTimeout() {
        return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + idleTimeout;
    }

    public void setConnector(NIOConnector connector) {
        this.connector = connector;
    }

		/* 连接对端 */
    public void connect(Selector selector) throws IOException {
        channel.register(selector, SelectionKey.OP_CONNECT, this);
        channel.connect(new InetSocketAddress(host, port));
    }

		/* 完成连接后调用 */
    public boolean finishConnect() throws IOException {
        if (channel.isConnectionPending()) {
            channel.finishConnect();
            localPort = channel.socket().getLocalPort();
            isFinishConnect = true;
            return true;
        } else {
            return false;
        }
    }

    public void setProcessor(NIOProcessor processor) {
        this.processor = processor;
        this.readBuffer = processor.getBufferPool().allocate();
        processor.addBackend(this);
    }

    public void setHandler(NIOHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(byte[] data) {
        try {
            handler.handle(data);
        } catch (Throwable e) {
            error(ErrorCode.ERR_HANDLE_DATA, e);
        }
    }

    @Override
    protected void idleCheck() {
        // nothing
    }

}

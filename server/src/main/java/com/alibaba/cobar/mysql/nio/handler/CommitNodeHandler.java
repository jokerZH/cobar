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
/**
 * (created at 2012-4-28)
 */
package com.alibaba.cobar.mysql.nio.handler;

import java.util.List;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import com.alibaba.cobar.mysql.nio.MySQLConnection;
import com.alibaba.cobar.net.mysql.ErrorPacket;
import com.alibaba.cobar.net.mysql.OkPacket;
import com.alibaba.cobar.route.RouteResultsetNode;
import com.alibaba.cobar.server.ServerConnection;
import com.alibaba.cobar.server.session.NonBlockingSession;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public class CommitNodeHandler extends MultiNodeHandler {
    private static final Logger logger = Logger.getLogger(CommitNodeHandler.class);
    private OkPacket okPacket;

    public CommitNodeHandler(NonBlockingSession session) {
        super(session);
    }

    public void commit() {
        commit(null);
    }

    public void commit(OkPacket packet) {
        final int initCount = session.getTargetCount();
        lock.lock();
        try {
            reset(initCount);
            okPacket = packet;
        } finally {
            lock.unlock();
        }
        if (session.closed()) {
            decrementCountToZero();
            return;
        }

        // 执行
        Executor executor = session.getSource().getProcessor().getExecutor();
        int started = 0;
        for (RouteResultsetNode rrn : session.getTargetKeys()) {
            if (rrn == null) {
                try {
                    logger.error("null is contained in RoutResultsetNodes, source = " + session.getSource());
                } catch (Exception e) {
                }
                continue;
            }
            final MySQLConnection conn = session.getTarget(rrn);
            if (conn != null) {	/* 调用commit函数的时候，MySQLConnection必须是连接的 */
                conn.setRunning(true);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isFail.get() || session.closed()) {
                            backendConnError(conn, "cancelled by other thread");
                            return;
                        }
                        conn.setResponseHandler(CommitNodeHandler.this);
                        conn.commit();
                    }
                });
                ++started;
            }
        }

        if (started < initCount && decrementCountBy(initCount - started)) {
            /**
             * assumption: only caused by front-end connection close. <br/>
             * Otherwise, packet must be returned to front-end
             */
            session.clearConnections();
        }
    }

    @Override
    public void connectionAcquired(MySQLConnection conn) {
        logger.error("unexpected invocation: connectionAcquired from commit");
        conn.release();
    }

    @Override
    public void connectionError(Throwable e, MySQLConnection conn) {
        backendConnError(conn, "connection err for " + conn);
    }

		/* 只接受ok包 */
    @Override
    public void okResponse(byte[] ok, MySQLConnection conn) {
        conn.setRunning(false);
        if (decrementCountBy(1)) {
            if (isFail.get() || session.closed()) {
                notifyError((byte) 1);
            } else {
                session.releaseConnections();
                if (okPacket == null) {
                    ServerConnection source = session.getSource();
                    source.write(ok);
                } else {
                    okPacket.write(session.getSource());
                }
            }
        }
    }

    @Override
    public void errorResponse(byte[] data, MySQLConnection conn) {
        ErrorPacket err = new ErrorPacket();
        err.read(data);
        backendConnError(conn, err);
    }

    @Override
    public void rowEofResponse(byte[] eof, MySQLConnection conn) {
        backendConnError(conn, "Unknown response packet for back-end commit");
    }

    @Override
    public void fieldEofResponse(byte[] header, List<byte[]> fields, byte[] eof, MySQLConnection conn) {
        logger.error(new StringBuilder().append("unexpected packet for ")
                                        .append(conn)
                                        .append(" bound by ")
                                        .append(session.getSource())
                                        .append(": field's eof")
                                        .toString());
    }

    @Override
    public void rowResponse(byte[] row, MySQLConnection conn) {
        logger.warn(new StringBuilder().append("unexpected packet for ")
                                       .append(conn)
                                       .append(" bound by ")
                                       .append(session.getSource())
                                       .append(": row data packet")
                                       .toString());
    }
}

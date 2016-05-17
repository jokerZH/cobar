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
package com.alibaba.cobar.config.model;

/**
 * 描述一个mysql实例的配置
 * 
 * @author xianmao.hexm 2011-1-11 下午02:14:38
 */
public final class DataSourceConfig {

    private static final int DEFAULT_SQL_RECORD_COUNT = 10;

    private String name;			/* dataSource的名字 */
    private String type;			/* 类型 如mysql等 */
    private String host;			/* 后端主机 */
    private int port;					/* 后端端口 */
    private String user;			/* 后端用户名 */
    private String password;	/* 后端密码 */
    private String database;	/* 后端dbName */
    private String sqlMode;		/* TODO */
    private int sqlRecordCount = DEFAULT_SQL_RECORD_COUNT;	/* 慢请求记录 */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSqlMode() {
        return sqlMode;
    }

    public void setSqlMode(String sqlMode) {
        this.sqlMode = sqlMode;
    }

    public int getSqlRecordCount() {
        return sqlRecordCount;
    }

    public void setSqlRecordCount(int sqlRecordCount) {
        this.sqlRecordCount = sqlRecordCount;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[name=")
                                  .append(name)
                                  .append(",host=")
                                  .append(host)
                                  .append(",port=")
                                  .append(port)
                                  .append(",database=")
                                  .append(database)
                                  .append(']')
                                  .toString();
    }

}

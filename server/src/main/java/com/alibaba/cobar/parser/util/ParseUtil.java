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
package com.alibaba.cobar.parser.util;

/**
 * @author xianmao.hexm 2011-5-9 下午02:40:29
 */
public final class ParseUtil {

	/* 空格 \t \n \r ; */
    public static boolean isEOF(char c) {
        return (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ';');
    }

    public static long getSQLId(String stmt) {
        int offset = stmt.indexOf('=');
        if (offset != -1 && stmt.length() > ++offset) {
            String id = stmt.substring(offset).trim();
            try {
                return Long.parseLong(id);
            } catch (NumberFormatException e) {
            }
        }
        return 0L;
    }

    /**
     * <code>'abc'</code>
     * 
     * @param offset stmt.charAt(offset) == first <code>'</code>
	 * 从stmt中获得一个单引号括起来的字符串　TODO
	 *
	 * 将字符串中的\x转换成对应的特殊字符串
	 * 碰到一个单引号 则返回，如果碰到两个单引号，则写入一个单引号，这里应该是对单引号的处理，有点转义的味道
     */
    private static String parseString(String stmt, int offset) {
        StringBuilder sb = new StringBuilder();
        loop: for (++offset; offset < stmt.length(); ++offset) {
            char c = stmt.charAt(offset);
            if (c == '\\') {
                switch (c = stmt.charAt(++offset)) {
                case '0':
                    sb.append('\0');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'Z':
                    sb.append((char) 26);
                    break;
                default:
                    sb.append(c);
                }
            } else if (c == '\'') {
                if (offset + 1 < stmt.length() && stmt.charAt(offset + 1) == '\'') {
                    ++offset;
                    sb.append('\'');
                } else {
                    break loop;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * <code>"abc"</code>
     * 
     * @param offset stmt.charAt(offset) == first <code>"</code>
	 *
	 * 和上面类似，只是这里是双引号
     */
    private static String parseString2(String stmt, int offset) {
        StringBuilder sb = new StringBuilder();
        loop: for (++offset; offset < stmt.length(); ++offset) {
            char c = stmt.charAt(offset);
            if (c == '\\') {
                switch (c = stmt.charAt(++offset)) {
                case '0':
                    sb.append('\0');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'Z':
                    sb.append((char) 26);
                    break;
                default:
                    sb.append(c);
                }
            } else if (c == '"') {
                if (offset + 1 < stmt.length() && stmt.charAt(offset + 1) == '"') {
                    ++offset;
                    sb.append('"');
                } else {
                    break loop;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * <code>AS `abc`</code>
     * 
     * @param offset stmt.charAt(offset) == first <code>`</code>
	 *
	 * 找到第一个`,返回前面的字符串,两个连续的``表示一个`
     */
    private static String parseIdentifierEscape(String stmt, int offset) {
        StringBuilder sb = new StringBuilder();
        loop: for (++offset; offset < stmt.length(); ++offset) {
            char c = stmt.charAt(offset);
            if (c == '`') {
                if (offset + 1 < stmt.length() && stmt.charAt(offset + 1) == '`') {
                    ++offset;
                    sb.append('`');
                } else {
                    break loop;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * @param aliasIndex for <code>AS id</code>, index of 'i'
	 *
	 * 获得stmt中aliasIndex开始的一个identidifer eg 'xxx' "xxx" `xxx` xxx
     */
    public static String parseAlias(String stmt, final int aliasIndex) {
        if (aliasIndex < 0 || aliasIndex >= stmt.length()) {
            return null;
        }
        switch (stmt.charAt(aliasIndex)) {
        case '\'':
            return parseString(stmt, aliasIndex);
        case '"':
            return parseString2(stmt, aliasIndex);
        case '`':
            return parseIdentifierEscape(stmt, aliasIndex);
        default:
            int offset = aliasIndex;
            for (; offset < stmt.length() && CharTypes.isIdentifierChar(stmt.charAt(offset)); ++offset);
            return stmt.substring(aliasIndex, offset);
        }
    }

	/* 如果offset指向了/*! xxx */ 或者 #xxx 开头，则返回末尾的下表，否则返回offset */
    public static int comment(String stmt, int offset) {
        int len = stmt.length();
        int n = offset;
        switch (stmt.charAt(n)) {
        case '/':
            if (len > ++n && stmt.charAt(n++) == '*' && len > n + 1 && stmt.charAt(n) != '!') {
                for (int i = n; i < len; ++i) {
                    if (stmt.charAt(i) == '*') {
                        int m = i + 1;
                        if (len > m && stmt.charAt(m) == '/')
                            return m;
                    }
                }
            }
            break;
        case '#':
            for (int i = n + 1; i < len; ++i) {
                if (stmt.charAt(i) == '\n')
                    return i;
            }
            break;
        }
        return offset;
    }

	/* 判断stmt中offset的字符是否是空格类的 */
    public static boolean currentCharIsSep(String stmt, int offset) {
        if (stmt.length() > offset) {
            switch (stmt.charAt(offset)) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                return true;
            default:
                return false;
            }
        }
        return true;
    }

    /*****
     * 检查下一个字符是否为分隔符，并把偏移量加1
     */
    public static boolean nextCharIsSep(String stmt, int offset) {
        return currentCharIsSep(stmt, ++offset);
    }

    /*****
     * 检查下一个字符串是否为期望的字符串，并把偏移量移到从offset开始计算，expectValue之后的位置
     * 
     * @param stmt 被解析的sql
     * @param offset 被解析的sql的当前位置
     * @param nextExpectedString 在stmt中准备查找的字符串
     * @param checkSepChar 当找到expectValue值时，是否检查其后面字符为分隔符号
     * @return 如果包含指定的字符串，则移动相应的偏移量，否则返回值=offset
     */
    public static int nextStringIsExpectedWithIgnoreSepChar(String stmt, int offset, String nextExpectedString,
                                                            boolean checkSepChar) {
        if (nextExpectedString == null || nextExpectedString.length() < 1)
            return offset;
        int i = offset;
        int index = 0;
        char expectedChar;
        char actualChar;
        boolean isSep;
        for (; i < stmt.length() && index < nextExpectedString.length(); ++i) {
            if (index == 0) {
                isSep = currentCharIsSep(stmt, i); 	/* 去掉开头的空格 */
                if (isSep) {
                    continue;
                }
            }
            actualChar = stmt.charAt(i);
            expectedChar = nextExpectedString.charAt(index++);
            if (actualChar != expectedChar) {
                return offset;
            }
        }
        if (index == nextExpectedString.length()) {
            boolean ok = true;
            if (checkSepChar) {
                ok = nextCharIsSep(stmt, i);
            }
            if (ok)
                return i;
        }
        return offset;
    }

    private static final String JSON = "json";
    private static final String EQ = "=";

    // private static final String WHERE = "where";
    // private static final String SET = "set";

    /**********
     * 检查下一个字符串是否json= *
     * 
     * @param stmt 被解析的sql
     * @param offset 被解析的sql的当前位置
     * @return 如果包含指定的字符串，则移动相应的偏移量，否则返回值=offset
     */
    public static int nextStringIsJsonEq(String stmt, int offset) {
        int i = offset;

        // / drds 之后的符号
        if (!currentCharIsSep(stmt, ++i)) {
            return offset;
        }

        // json 串
        int k = nextStringIsExpectedWithIgnoreSepChar(stmt, i, JSON, false);
        if (k <= i) {
            return offset;
        }
        i = k;

        // 等于符号
        k = nextStringIsExpectedWithIgnoreSepChar(stmt, i, EQ, false);
        if (k <= i) {
            return offset;
        }
        return i;
    }

	/* 向后移动length， 不计算开头的注视和空格 */
    public static int move(String stmt, int offset, int length) {
        int i = offset;
        for (; i < stmt.length(); ++i) {
            switch (stmt.charAt(i)) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                continue;
            case '/':
            case '#':
                i = comment(stmt, i);
                continue;
            default:
                return i + length;
            }
        }
        return i;
    }

	/* 检查空offset开头的字符串是否是keyword */
    public static boolean compare(String s, int offset, char[] keyword) {
        if (s.length() >= offset + keyword.length) {
            for (int i = 0; i < keyword.length; ++i, ++offset) {
                if (Character.toUpperCase(s.charAt(offset)) != keyword[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}

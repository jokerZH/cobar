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
 * (created at 2011-7-25)
 */
package com.alibaba.cobar.parser.ast.expression;

import java.util.Map;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 *
 * 增加对计算结果金进行缓存的功能
 */
public abstract class AbstractExpression implements Expression {
    private boolean cacheEvalRst = true;	/* 是否缓存结果 	*/
    private boolean evaluated;				/* 是否已经缓存了结果	*/	
    private Object evaluationCache;			/* 缓存的计算结果	*/

    @Override
    public Expression setCacheEvalRst(boolean cacheEvalRst) {
        this.cacheEvalRst = cacheEvalRst;
        return this;
    }

	/* 在计算函数外面包上缓存相关的东西 */
    @Override
    public final Object evaluation(Map<? extends Object, ? extends Object> parameters) {
        if (cacheEvalRst) {
            if (evaluated) {
                return evaluationCache;
            }
            evaluationCache = evaluationInternal(parameters);
            evaluated = true;
            return evaluationCache;
        }
        return evaluationInternal(parameters);
    }

	/* 根据参数计算结果的函数 */
    protected abstract Object evaluationInternal(Map<? extends Object, ? extends Object> parameters);

}

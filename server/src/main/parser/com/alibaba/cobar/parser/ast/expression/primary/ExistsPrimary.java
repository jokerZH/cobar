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
 * (created at 2011-1-21)
 */
package com.alibaba.cobar.parser.ast.expression.primary;

import com.alibaba.cobar.parser.ast.expression.misc.QueryExpression;
import com.alibaba.cobar.parser.visitor.SQLASTVisitor;

/**
 * <code>'EXISTS' '(' subquery ')'</code>
 * 
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 * EXIST( select语句 )  如果select中有数据，则exist返回true， 否则返回false
 */
public class ExistsPrimary extends PrimaryExpression {
    private final QueryExpression subquery;

    public ExistsPrimary(QueryExpression subquery) {
        if (subquery == null)
            throw new IllegalArgumentException("subquery is null for EXISTS expression");
        this.subquery = subquery;
    }

    /**
     * @return never null
     */
    public QueryExpression getSubquery() {
        return subquery;
    }

    @Override
    public void accept(SQLASTVisitor visitor) {
        visitor.visit(this);
    }
}

package org.nlpcn.es4sql.parse;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import org.nlpcn.es4sql.Util;
import org.nlpcn.es4sql.domain.Where;
import org.nlpcn.es4sql.exception.SqlParseException;

import java.util.List;

/**
 * Created by Eliran on 12/11/2015.
 */
public class NestedType {
    //字段名称
    public String field;
    //排序时的路径和条件
    public String path;
    public Where where;
    public String mode;
    private boolean reverse;
    private boolean simple;

    public boolean tryFillFromExpr(SQLExpr expr) throws SqlParseException {
        if (!(expr instanceof SQLMethodInvokeExpr)) return false;
        SQLMethodInvokeExpr method = (SQLMethodInvokeExpr) expr;
        String methodNameLower = method.getMethodName().toLowerCase();
        if (!(methodNameLower.equals("nested") || methodNameLower.equals("reverse_nested"))) return false;

        reverse = methodNameLower.equals("reverse_nested");

        List<SQLExpr> parameters = method.getParameters();
        if (parameters.size() != 3 && parameters.size() != 2 && parameters.size() != 1)
            throw new SqlParseException("on nested object only allowed 2 parameters (field,path)/(path,conditions..) or 1 parameter (field) ");

        String field = Util.extendedToString(parameters.get(0));
        this.field = field;
        if (parameters.size() == 1) {
            //calc path myself..
            if (!field.contains(".")) {
                if (!reverse)
                    throw new SqlParseException("nested should contain . on their field name");
                else {
                    this.path = null;
                    this.simple = true;
                }
            } else {
                int lastDot = field.lastIndexOf(".");
                this.path = field.substring(0, lastDot);
                this.simple = true;

            }

        } else if (parameters.size() == 2) {
            SQLExpr lastParameter = parameters.get(1);
            if (lastParameter instanceof SQLTextLiteralExpr || lastParameter instanceof SQLIdentifierExpr || lastParameter instanceof SQLPropertyExpr) {
                String pathString = Util.extendedToString(lastParameter);
                if (pathString.equals(""))
                    this.path = null;
                else
                    this.path = pathString;
                this.simple = true;
            } else {
                this.path = field;
                Where where = Where.newInstance();
                new SqlParser().parseWhere(lastParameter, where);
                if (where.getWheres().size() == 0)
                    throw new SqlParseException("unable to parse filter where.");
                this.where = where;
                simple = false;
            }
        } else if (parameters.size() == 3) {
            this.path = field;
            SQLExpr secondParameter = parameters.get(1);
            if (secondParameter instanceof SQLAggregateExpr){
                this.mode = ((SQLAggregateExpr) secondParameter).getMethodName();
                this.field = ((SQLAggregateExpr) secondParameter).getArguments().get(0).toString();
            }
            SQLExpr lastParameter = parameters.get(2);
            Where where = Where.newInstance();
            new SqlParser().parseWhere(lastParameter, where);
            if (where.getWheres().size() == 0)
                throw new SqlParseException("unable to parse filter where.");
            this.where = where;
            simple = false;
        }

        return true;
    }

    public boolean isSimple() {
        return simple;
    }

    public boolean isReverse() {
        return reverse;
    }
}

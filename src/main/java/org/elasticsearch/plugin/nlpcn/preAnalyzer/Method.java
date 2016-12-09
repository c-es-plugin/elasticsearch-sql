package org.elasticsearch.plugin.nlpcn.preAnalyzer;

/**
 * Created by fangbb on 2016-12-8.
 */
public class Method {
    String parentMethod;
    String childenMethod;
    String params;

    public Method(String parentMethod, String childenMethod, String params) {
        this.parentMethod = parentMethod;
        this.childenMethod = childenMethod;
        this.params = params;
    }

    public Method() {
    }

    public String getParentMethod() {
        return parentMethod;
    }

    public void setParentMethod(String parentMethod) {
        this.parentMethod = parentMethod;
    }

    public String getChildenMethod() {
        return childenMethod;
    }

    public void setChildenMethod(String childenMethod) throws Exception{
        if (this.parentMethod.equals("seg") && childenMethod != null) {
            throw new Exception("seg("+childenMethod+"()) is erro");
        } else {
            this.childenMethod = childenMethod;
        }

    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getFunName() {
        String name = "";
        if (this.parentMethod != null && !this.parentMethod.equals("seg")) {
            name = this.parentMethod;
        } else if (this.childenMethod != null && !this.childenMethod.equals("seg")) {
            name = this.childenMethod;
        }
        return name;
    }

    public boolean containSeg() {
        if (this.parentMethod != null && this.parentMethod.equals("seg")
                ||this.childenMethod!=null && this.childenMethod.equals("seg")) {
            return true;
        }
        return false;
    }
}

package FlowSlicer.XMLObject;

import lombok.Getter;
import soot.baf.internal.BPopInst;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "reference")
public class Reference implements Serializable {
    private final static long serialVersionUID = 1L;
    @Getter
    @XmlAttribute(name = "type")
    protected String type;
    @XmlElement(name = "statementType")
    protected Boolean isAssignment; // true if the statement is an Assignment statement; false if the statement is an invoke statement.
    @Getter
    @XmlElement(name = "StatementLeftBox")
    protected String statementLeftBox; // not null only when isAssignment is true
    @Getter
    @XmlElement(name = "StatementInvoker")
    protected String statementInvoker; // always not null

    @Getter
    @XmlElement(name = "InvokeType")
    protected String invokeType; // always not null

    @Getter
    @XmlElement(name = "statementMethod")
    protected String statementMethod;
    @Getter
    @XmlElement(name = "statementParams")
    protected List<String> statementParams;
    @Getter
    @XmlElement(name = "method")
    protected String method;
    @Getter
    @XmlElement(name = "classname")
    protected String classname;

    public Reference(String type, String classname, String method,
                     Boolean isAssignment, String statementLeftBox,
                     String statementInvoker, String invokeType,
                     String statementMethod, List<String> statementParams)
    {
        this.type = type;
        this.classname = classname;
        this.method = method;
        this.statementMethod = statementMethod;
        this.statementParams = statementParams;
        this.isAssignment = isAssignment;
        this.statementLeftBox = statementLeftBox;
        this.statementInvoker = statementInvoker;
        this.invokeType = invokeType;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setIsAssignment (Boolean isAssignment) {
        this.isAssignment = isAssignment;
    }

    public Boolean IsAssignment () {
        return this.isAssignment;
    }

    public void setStatementLeftBox (String statementLeftBox) {
        this.statementLeftBox = statementLeftBox;
    }

    public void setStatementInvoker (String statementInvoker) {
        this.statementInvoker = statementInvoker;
    }

    public void setInvokeType (String invokeType) {
        this.invokeType = invokeType;
    }

    public void setStatementMethod(String value) {
        this.statementMethod = value;
    }

    public void setStatementParams(List<String> value) {
        this.statementParams = value;
    }

    public void setMethod(String value) {
        this.method = value;
    }

    public void setClassname(String value) {
        this.classname = value;
    }

    // 重写equals方法，判断两个对象是否相同
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reference reference = (Reference) o;
        return isAssignment.equals(reference.isAssignment) &&
                statementMethod.equals(reference.statementMethod) &&
                statementParams.equals(reference.statementParams) &&
                method.equals(reference.method) &&
                classname.equals(reference.classname);
    }

    @Override
    public int hashCode() {
        int result = 17; // 选择一个非零常数作为初始值, 使用素数31乘以当前结果，然后加上域的哈希码
        result = 31 * result + isAssignment.hashCode();
        result = 31 * result + statementMethod.hashCode();
        result = 31 * result + statementParams.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + classname.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "isAssignment = " + isAssignment +
                ", classname = " + classname +
                ", method = " + method +
                ", statementMethod = " + statementMethod +
                ", statementParams = " + statementParams +
                "\n";
    }
}

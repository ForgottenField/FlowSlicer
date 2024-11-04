package FlowSlicer.XMLObject;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Source")
@Getter
public class Source implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "Statement")
    private String statement;
    @XmlAttribute(name = "Method")
    private String method;
    @XmlAttribute(name = "MethodSourceSinkDefinition")
    private String methodSourceSinkDefinition;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public void setMethodSourceSinkDefinition(String methodSourceSinkDefinition) {
        this.methodSourceSinkDefinition = methodSourceSinkDefinition;
    }
}

package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "node")
@Getter
public class Node implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "method")
    private String method;
    @XmlAttribute(name = "type")
    private String type;
    @XmlAttribute(name = "unit")
    private String unit;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}

package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "method")
@Getter
public class IntentMethod implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "value")
    private String value;

    public void setValue(String value){
        this.value = value;
    }
}

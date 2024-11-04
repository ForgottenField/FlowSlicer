package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "methodtrace")
@Getter
public class IntentMethodTrace implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "value")
    private String value;

    public void setValue(String value){
        this.value = value;
    }
}

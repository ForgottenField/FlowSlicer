package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "source")
@Getter
public class IntentSource implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "name")
    private String name;

    public void setName(String name){
        this.name = name;
    }
}

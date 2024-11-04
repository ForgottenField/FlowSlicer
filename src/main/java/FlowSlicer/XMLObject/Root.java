package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "root")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class Root implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "Component")
    private List<Component> components;

    public void setComponents(List<Component> components) {
        this.components = components;
    }
}

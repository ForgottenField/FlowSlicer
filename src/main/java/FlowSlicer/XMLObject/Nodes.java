package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "nodes")
@Getter
public class Nodes implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "node")
    private List<Node> nodes;

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}

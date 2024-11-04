package FlowSlicer.XMLObject;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "DataFlowResults")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class DataFlowResults implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "Results")
    private Leaks leaks;

    public void setLeaks(Leaks leaks) {
        this.leaks = leaks;
    }
}

package FlowSlicer.XMLObject;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Results")
@Getter
public class Leaks implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "Result")
    private List<Leak> leaks;

    public void setLeaks(List<Leak> leaks) {
        this.leaks = leaks;
    }
}

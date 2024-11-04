package FlowSlicer.XMLObject;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Sources")
@Getter
public class Sources implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "Source")
    private List<Source> sources;

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }
}

package FlowSlicer.XMLObject;

import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Result")
@Getter
public class Leak implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "Sink")
    private Sink sink;
    @XmlElement(name = "Sources")
    private Sources sources;

    public void setSink(Sink sink) {
        this.sink = sink;
    }

    public void setType(Sources sources) {
        this.sources = sources;
    }
}

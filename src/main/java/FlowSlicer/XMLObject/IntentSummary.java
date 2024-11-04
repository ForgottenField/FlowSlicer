package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "intentSummary")
@Getter
public class IntentSummary implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "source")
    private IntentSource source;

    @XmlElement(name = "destination")
    private IntentDestination destination;

    @XmlElement(name = "sendICCInfo")
    private SendICCInfo sendICCInfo;

    @XmlElement(name = "receiveICCInfo")
    private ReceiveICCInfo receiveICCInfo;

    @XmlElement(name = "method")
    private IntentMethod method;

    @XmlElement(name = "methodtrace")
    private IntentMethodTrace methodtrace;

    @XmlElement(name = "nodes")
    private Nodes nodes;

    public void setSource(IntentSource source) {
        this.source = source;
    }

    public void setDestination(IntentDestination destination) {
        this.destination = destination;
    }

    public void setSendICCInfo(SendICCInfo sendICCInfo) {
        this.sendICCInfo = sendICCInfo;
    }

    public void setReceiveICCInfo(ReceiveICCInfo receiveICCInfo) {
        this.receiveICCInfo = receiveICCInfo;
    }

    public void setMethod(IntentMethod method) {
        this.method = method;
    }

    public void setMethodtrace(IntentMethodTrace methodtrace) {
        this.methodtrace = methodtrace;
    }

    public void setNodes(Nodes nodes) {
        this.nodes = nodes;
    }
}

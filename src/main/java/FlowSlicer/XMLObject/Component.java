package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Component")
@Getter
public class Component implements Serializable {
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "source")
    private String source;

    @XmlElement(name = "intentSummary")
    private List<IntentSummary> intentSummaries;

    public void setSource(String source) {
        this.source = source;
    }

    public void setIntentSummaries(List<IntentSummary> intentSummaries) {
        this.intentSummaries = intentSummaries;
    }
}

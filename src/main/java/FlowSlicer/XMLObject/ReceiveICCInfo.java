package FlowSlicer.XMLObject;
import lombok.Getter;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "sendICCInfo")
@Getter
public class ReceiveICCInfo implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "action")
    private String action;

    @XmlAttribute(name = "extras")
    private String extras;

    @XmlElement(name = "info")
    private ICCInfo iccInfo;

    public void setAction(String action) {
        this.action = action;
    }

    public void setExtras(String extras) {
        this.extras = extras;
    }

    public void setICCInfo(ICCInfo iccInfo) {
        this.iccInfo = iccInfo;
    }
}

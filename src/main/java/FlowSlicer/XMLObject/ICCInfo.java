package FlowSlicer.XMLObject;
import lombok.Getter;
import javax.xml.bind.annotation.*;
import java.io.Serializable;

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "info")
public class ICCInfo implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "unit")
    private String unit;
    @XmlAttribute(name = "methodSig")
    private String methodSig;
    @XmlAttribute(name = "instructionId")
    private String instructionId;
    @XmlAttribute(name = "key")
    private String key;
    @XmlAttribute(name = "value")
    private String value;

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setMethodSig(String methodSig) {
        this.methodSig = methodSig;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

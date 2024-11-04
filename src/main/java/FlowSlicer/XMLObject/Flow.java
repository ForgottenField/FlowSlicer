package FlowSlicer.XMLObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "flow")
public class Flow implements Serializable{
    private final static long serialVersionUID = 1L;
    @XmlElement(name = "reference")
    private List<Reference> reference;

    public Flow(Reference from, Reference to){
        reference = new ArrayList<>();
        reference.add(from);
        reference.add(to);
    }

    public List<Reference> getReference() {
        if (reference == null) {
            reference = new ArrayList<Reference>();
        }
        return this.reference;
    }

    public void setReference(List<Reference> reference){
        this.reference = reference;
    }
}

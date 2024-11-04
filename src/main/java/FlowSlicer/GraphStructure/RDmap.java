package FlowSlicer.GraphStructure;

import java.util.HashMap;

public class RDmap {//Singleton Model
    private static final HashMap<String, Double> rdmap = new HashMap<>();
    private static RDmap instance = new RDmap();
    private RDmap() {
        rdmap.put("ExtendsRelation", 0.5);
        rdmap.put("ImplementRelation", 0.5);
        rdmap.put("InvokeRelation", 0.5);
        rdmap.put("OuterClassRelation", 0.5);
        rdmap.put("AssociationRelation", 0.5);
        rdmap.put("FragmentLoadRelation", 0.5);
        rdmap.put("ICCRelation", 1.0);
    }

    public static RDmap getInstance() {
        return instance;
    }

    public Double findWeightFromRelation(String type){
        return rdmap.get(type);
    }
}

package FlowSlicer.Mode;

import FlowSlicer.XMLObject.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;
@Slf4j
@Getter
public class ICCEdgesHandler {
    private static List<Flow> ICCEdges = new ArrayList<>();
    private static HashSet<Map<String, String>> IntentPairs = new HashSet<>();

    public static void parseICCFile(String ICCFilePath){
        try {
            File xmlFile = new File(ICCFilePath);
            JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Root root = (Root)jaxbUnmarshaller.unmarshal(xmlFile);

            // Extract ICCEdges
            List<Reference> fromReferenceList = new ArrayList<>();
            List<Reference> toReferenceList = new ArrayList<>();

            List<Component> components = root.getComponents();
            for (Component component : components) {
                for (IntentSummary intentSummary : component.getIntentSummaries()){
                    IntentSource intentSource =  intentSummary.getSource();
                    String classNameFrom = (intentSource == null)? null : intentSource.getName();
                    IntentDestination intentDestination = intentSummary.getDestination();
                    String classNameTo = (intentDestination == null)? null : intentSummary.getDestination().getName();

                    if(classNameFrom != null && classNameTo != null){
                        Map<String, String> map = new HashMap<>();
                        map.put(classNameFrom, classNameTo);
                        IntentPairs.add(map);
                    }

                    SendICCInfo sendICCInfo = intentSummary.getSendICCInfo();
                    if(sendICCInfo != null){
                        ICCInfo iccInfo = sendICCInfo.getIccInfo();
                        if(iccInfo != null){
//                            fromReferenceList.add(new Reference("from", classNameFrom, iccInfo.getMethodSig(), iccInfo.getUnit()));
                        }
                    }
                    ReceiveICCInfo receiveICCInfo = intentSummary.getReceiveICCInfo();
                    if(receiveICCInfo != null){
                        ICCInfo iccInfo = receiveICCInfo.getIccInfo();
                        if(iccInfo != null){
//                            toReferenceList.add(new Reference("to", classNameFrom, iccInfo.getMethodSig(), iccInfo.getUnit()));
                        }
                    }
                }
            }

            // eliminate repeat
            Set<Reference> fromReferenceSet = new HashSet<>(fromReferenceList);
            Set<Reference> toReferenceSet = new HashSet<>(toReferenceList);

            for (Map<String, String> pair : IntentPairs){
                for (String fromClassName : pair.keySet()){
                    String toClassName = pair.get(fromClassName);
                    for (Reference from : fromReferenceSet){
                        for(Reference to : toReferenceSet){
                            if (from.getClassname().equals(fromClassName) && to.getClassname().equals(toClassName)){
                                ICCEdges.add(new Flow(from,to));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e + ": Error reading input ICCFile.");
        }
    }

    public static void addInputEdges(DependenceGraph sdg) {
        if (ICCEdges != null) {
            for (Flow edge : ICCEdges) {
                Unit from = null;
                for (Reference ref : edge.getReference()){
                    if (ref.getType().equals("from")) {
                        from = findUnitForReference(ref);
                        break;
                    }
                }

                if (from != null) {
                    Unit to = null;
                    for (Reference ref : edge.getReference()){
                        if (ref.getType().equals("to")) {
                            to = findUnitForReference(ref);
                            break;
                        }
                    }

                    if (to != null) {
                        final Edge inputEdge = new Edge(from, to, DependenceGraph.EDGE_TYPE_ICC);
                        sdg.addEdge(inputEdge);
                        log.info("Adding input edge from " + from + " to " + to);
                    }
                }
            }
        }
    }

    private static Unit findUnitForReference(Reference reference) {
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.isConcrete() && sc.getName().equals(reference.getClassname())) {
                for (final SootMethod sm : sc.getMethods()) {
                    if (sm.getSignature().equals(reference.getMethod())) {
                        final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
                        if (body == null) {
                            continue;
                        }

                        // Find unit (precise)
                        for (final Unit unit : body.getUnits()) {
                            if (unit.toString().equals(reference.getStatementMethod())) {
                                return Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(unit, unit);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}

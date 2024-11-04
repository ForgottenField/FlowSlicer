package FlowSlicer.GraphStructure;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;

public class SCCNode extends Node {
    private ArrayList<GraphNode> sccList;
    protected HashSet<Edge<SCCNode>> fromEdgeSet;
    protected HashSet<Edge<SCCNode>> toEdgeSet;
    private ArrayList<SCCNode> sccClosure;
    @Getter
    private Boolean hasActivity; //置为true代表该节点中存在Activity组件
    private ArrayList<GraphNode> componentList;
    private Boolean isIncluded; //记录构建闭包之后，该SCCNode是否被任意闭包包括
    private int closureVertex;

    public SCCNode(Integer vertex, ArrayList<GraphNode> sccList) {
        super(vertex);
        this.fromEdgeSet = new HashSet<>();
        this.toEdgeSet = new HashSet<>();
        this.sccList = sccList;
        this.sccClosure = new ArrayList<>();
        this.hasActivity = false;
        this.componentList = new ArrayList<>();
        this.isIncluded = false;
        this.closureVertex = -1;
    }

    public ArrayList<GraphNode> getSccList() {
        return sccList;
    }

    public HashSet<Edge<SCCNode>> getFromEdgeSet() {
        return fromEdgeSet;
    }

    public HashSet<Edge<SCCNode>> getToEdgeSet() {
        return toEdgeSet;
    }

    public void setHasActivityTrue(){
        this.hasActivity = true;
    }

    public ArrayList<SCCNode> getSccClosure(){
        return sccClosure;
    }

    public void setSccClosure(ArrayList<SCCNode> sccClosure){
        this.sccClosure = sccClosure;
    }

    public ArrayList<GraphNode> getComponentList(){
        return componentList;
    }

    public void setComponentList(ArrayList<GraphNode> componentList){
        this.componentList = componentList;
    }

    public void setIsIncludedTrue(){
        this.isIncluded = true;
    }

    public boolean getIsIncluded(){
        return this.isIncluded;
    }

    public void setClosureVertex(int closureVertex){
        this.closureVertex = closureVertex;
    }

    public int getClosureVertex(){
        return this.closureVertex;
    }

    public void addFromEdge(final Edge<SCCNode> edge) {
        fromEdgeSet.add(edge);
        outDegree++;
    }

    public void addToEdge(final Edge<SCCNode> edge) {
        toEdgeSet.add(edge);
        inDegree++;
    }

    public Edge<SCCNode> get(final Integer to) {
        for(Edge<SCCNode> edge : fromEdgeSet) {
            Integer dest = edge.getTo().vertex;
            if(dest.equals(to)) {
                return edge;
            }
        }
        return null;
    }
}

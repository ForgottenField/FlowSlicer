package FlowSlicer.GraphStructure;

import java.util.ArrayList;

public class Closure {
    private Integer vertex;
    private ArrayList<SCCNode> sccComponentList;
    private ArrayList<SCCNode> sccNodeList;
    private int nodeNum;

    public Closure(Integer vertex, ArrayList<SCCNode> componentList){
        this.vertex = vertex;
        this.sccComponentList = componentList;
        this.sccNodeList = new ArrayList<>();
        this.nodeNum = 0;
    }

    public Closure(Integer vertex){
        this.vertex = vertex;
        this.sccComponentList = new ArrayList<>();
        this.sccNodeList = new ArrayList<>();
        this.nodeNum = 0;
    }

    public Integer getVertex() {
        return vertex;
    }

    public void setVertex(Integer vertex){
        this.vertex = vertex;
    }

    public ArrayList<SCCNode> getSCCComponentList(){
        return sccComponentList;
    }

    public ArrayList<SCCNode> getSCCNodeList(){
        return sccNodeList;
    }

    public int getNodeNum() {
        return nodeNum;
    }

    public void addSCCNode(SCCNode node){
        sccNodeList.add(node);
        nodeNum++;
    }
}

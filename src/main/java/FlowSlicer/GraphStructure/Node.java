package FlowSlicer.GraphStructure;

public class Node {
    protected Integer vertex;
    protected int inDegree;
    protected int outDegree;

    public Node(Integer vertex){
        this.vertex = vertex;
        this.inDegree = 0;
        this.outDegree = 0;
    }

    public Integer getVertex() {
        return vertex;
    }

    public void setVertex(Integer vertex){
        this.vertex = vertex;
    }

    public int getInDegree() {
        return inDegree;
    }

    public void setInDegree(int inDegree){
        this.inDegree = inDegree;
    }

    public int getOutDegree() {
        return outDegree;
    }

    public void setOutDegree(int outDegree){
        this.outDegree = outDegree;
    }
}

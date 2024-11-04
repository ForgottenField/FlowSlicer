package FlowSlicer.GraphStructure;

import java.util.HashSet;

public class GraphNode extends Node {
    private String classname;   //each node stands for a class in the apk
    private String attribute;   //Component or Application Class
    protected HashSet<Edge<GraphNode>> fromEdgeSet;
    protected HashSet<Edge<GraphNode>> toEdgeSet;

    public GraphNode(Integer vertex, String classname, String attribute) {
        super(vertex);
        this.classname = classname;
        this.attribute = attribute;
        this.fromEdgeSet = new HashSet<>();
        this.toEdgeSet = new HashSet<>();
    }

    public String getClassName() {
        return classname;
    }

    public void setClassname(String classname){
        this.classname = classname;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute){
        this.attribute = attribute;
    }

    public HashSet<Edge<GraphNode>> getFromEdgeSet() {
        return fromEdgeSet;
    }

    public HashSet<Edge<GraphNode>> getToEdgeSet() {
        return toEdgeSet;
    }

    public boolean isComponentClass () {
        return !attribute.equals("Application Class");
    }

    public void addFromEdge(final Edge<GraphNode> edge) {
        fromEdgeSet.add(edge);
        outDegree++;
    }

    public void addToEdge(final Edge<GraphNode> edge) {
        toEdgeSet.add(edge);
        inDegree++;
    }

    public Edge<GraphNode> get(final Integer to) {
        for(Edge<GraphNode> edge : fromEdgeSet) {
            Integer dest = edge.getTo().vertex;
            if(dest.equals(to)) {
                return edge;
            }
        }
        return null;
    }
}

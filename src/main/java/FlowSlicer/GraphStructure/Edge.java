package FlowSlicer.GraphStructure;

import java.util.HashMap;

public class Edge<V> {
    private V from;
    private V to;
    private double weight;
    private HashMap<String, Boolean> relationTable;
    private HashMap<String, Integer> associationInfoTable;//记录association关系下引用类的数目
    private HashMap<String, String> invokeInfoTable;

    public Edge(V from, V to){
        this.from = from;
        this.to = to;
        this.weight = 0;
        this.relationTable = new HashMap<>();
        this.associationInfoTable = new HashMap<>();
        this.invokeInfoTable = new HashMap<>();
        initRelationTable(this.relationTable);
        initInvokeInfoTable(this.invokeInfoTable);
    }

    public Edge(V from, V to, double weight){
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.relationTable = new HashMap<>();
        this.associationInfoTable = new HashMap<>();
        this.invokeInfoTable = new HashMap<>();
        initRelationTable(this.relationTable);
        initInvokeInfoTable(this.invokeInfoTable);
    }

    public void initRelationTable(HashMap<String, Boolean> relationTable){
        relationTable.put("ExtendsRelation", false);
        relationTable.put("ImplementRelation", false);
        relationTable.put("InvokeRelation", false);
        relationTable.put("OuterClassRelation", false);
        relationTable.put("AssociationRelation", false);
        relationTable.put("FragmentLoadRelation", false);
    }

    public void initInvokeInfoTable(HashMap<String, String> invokeInfoTable){
        invokeInfoTable.put("InvokeType", null);
        invokeInfoTable.put("MethodName", null);
        invokeInfoTable.put("ReturnType", null);
        invokeInfoTable.put("Parameters", null);
        invokeInfoTable.put("InvokeTimes", String.valueOf(0));
    }

    public V getFrom() {
        return from;
    }

    public void setFrom(V from) {
        this.from = from;
    }

    public V getTo() {
        return to;
    }

    public void setTo(V to) {
        this.to = to;
    }

    public double getWeight() {
        return weight;
    }

    public void addWeight(double weight) {
        this.weight += weight;
    }

    public Boolean getRelationType(String type){
        return relationTable.get(type);
    }

    public void setRelationTypeTrue(String type){
        relationTable.put(type, true);//put会覆盖原有key对应的value
    }

    public HashMap<String, Integer> getAssociationInfoTable(){
        return associationInfoTable;
    }

    public void setAssociationInfoTable(String string,Integer num){
        associationInfoTable.put(string,num);
    }

    public HashMap<String, String> getInvokeInfoTable(){
        return invokeInfoTable;
    }

    public void getWeightFromRelationTable(){
        for(String key : relationTable.keySet()){
            if(relationTable.get(key)){
                this.weight += RDmap.getInstance().findWeightFromRelation(key);
            }
        }
    }

    public boolean isExtendsEdge() {
        return getRelationType("ExtendsRelation").equals(true);
    }

    public boolean isImplementEdge() {
        return getRelationType("ImplementRelation").equals(true);
    }

    public boolean isOuterClassEdge() {
        return getRelationType("OuterClassRelation").equals(true);
    }

    public boolean isAssociationEdge() {
        return getRelationType("AssociationRelation").equals(true);
    }
    public boolean isInvokeEdge() {
        return getRelationType("InvokeRelation").equals(true);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from = " + ((Node)from).getVertex() +
                "(" + from.toString() +
                "),\n\tto = " + ((Node)to).getVertex() +
                "(" + to.toString() +
                "),\n\ttypeTable = " + relationTable +
                ",\n\tassociationInfo = " + associationInfoTable +
                ",\n\tinvokeInfo = " + invokeInfoTable +
                ",\n\tweight = " + weight +
                '}';
    }
}

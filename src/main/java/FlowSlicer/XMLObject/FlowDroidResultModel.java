package FlowSlicer.XMLObject;
import fj.P;
import lombok.Getter;

import java.util.*;

@Getter
public class FlowDroidResultModel {
    private List<Reference> sourceList;
    private List<Reference> sinkList;
    private Map<Reference, List<Reference>> sinkToSourcesMap;
    private int leakNum;

    public FlowDroidResultModel(){
        this.sinkList = new ArrayList<>();
        this.sourceList = new ArrayList<>();
        this.sinkToSourcesMap = new HashMap<>();
        this.leakNum = 0;
    }

    public void setSinkList(List<Reference> sinkList) {
        this.sinkList = sinkList;
    }

    public void setSourceList(List<Reference> sourceList) {
        this.sourceList = sourceList;
    }

    public void setSinkToSourcesMap(Map<Reference, List<Reference>> sinkToSourcesMap) {
        this.sinkToSourcesMap = sinkToSourcesMap;
    }
    public void setLeakNum (int num) { this.leakNum = num;}

    // 重写equals方法，判断两个对象是否相同
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowDroidResultModel result = (FlowDroidResultModel) o;

        if (!Objects.equals(result.getSourceList(), sourceList)) {
            return false;
        }
        if (!Objects.equals(result.getSinkList(), sinkList)) {
            return false;
        }

        if (result.getSinkToSourcesMap() == null) {
            return false;
        }

        for (Map.Entry<Reference, List<Reference>> entry : sinkToSourcesMap.entrySet()) {
            if (!result.getSinkToSourcesMap().containsKey(entry.getKey())) {
                return false;
            }
            if (!Objects.equals(entry.getValue(), result.getSinkToSourcesMap().get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17; // 选择一个非零常数作为初始值
        result = 31 * result + sourceList.hashCode(); // 使用素数31乘以当前结果，然后加上域的哈希码
        result = 31 * result + sinkList.hashCode();
        result = 31 * result + sinkToSourcesMap.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Source List: \n");
        for (Reference source : sourceList) {
            str.append(source.toString());
        }
        str.append("Sink List: \n");
        for (Reference sink : sinkList) {
            str.append(sink.toString());
        }
        str.append("Sink to Source Map: \n");
        str.append("{\n");
        for (Map.Entry<Reference, List<Reference>> entry : sinkToSourcesMap.entrySet()) {
            str.append("  ").append(entry.getKey()).append(": ");
            str.append(entry.getValue().toString()).append("\n");
        }
        str.append("}");
        return str.toString();
    }
}

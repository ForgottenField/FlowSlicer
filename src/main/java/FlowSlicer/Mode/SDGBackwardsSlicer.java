package FlowSlicer.Mode;

import lombok.extern.slf4j.Slf4j;
import soot.SootField;
import soot.Unit;

import java.util.*;
@Slf4j
public class SDGBackwardsSlicer extends SDGSlicer{
    public SDGBackwardsSlicer(DependenceGraph dg, Set<Unit> target) {
        super(dg, target, null);
    }

    public SDGBackwardsSlicer(DependenceGraph dg, Unit target) {
        super(dg, target, null);
    }

    /**
     * @param ignoreAlways
     *            true: ignore if the ignoreEdgeType is contained, false: only ignore if the ignoreEdgeType is the only assigned type
     */
    @Override
    protected void visit(Unit start, Set<Unit> visited, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways) {
        if (this.globalVisit != null && this.globalVisit.getVisited().contains(start)) {
            return;
        }

        visited.add(start);
        if (this.dg.getPredsOf(start) != null) {
            for (final Unit pred : this.dg.getPredsOf(start)) {
                Edge temp = new Edge(pred, start);
                final Set<Integer> tempTypes = this.dg.getEdgeTypes(temp);
                // Check ignored edge type
                if (!visited.contains(pred) && !ignoreEdge(tempTypes, ignoreEdgeTypes, ignoreAlways)) {
                    if (tempTypes.contains(DependenceGraph.EDGE_TYPE_FIELD_DATA)
                            || tempTypes.contains(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA)) {
                        // Re-init context-sensitivity due to (static-)field edge
                        init(pred);
                    }
                    try {
                        if (!this.worklist.contains(pred)) {
                            this.worklist.add(pred);
                        }
                    } catch (final StackOverflowError e) {
                        log.error("A StackOverflowError was thrown while slicing through: " + pred + ". To avoid please increase the JavaVitualMachine's heap size (launch parameter \"-Xss\" - e.g.: \"-Xss2m\")");
                        return;
                    }
                }
                temp = null;
            }
        }
    }

    @Override
    protected void init(Unit start) {
        final Set<Unit> replacedCalls = super.findMethodCalls(start);
        super.getContextSensitivelyAllowed().addAll(replacedCalls);
    }
}

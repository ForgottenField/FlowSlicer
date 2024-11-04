package FlowSlicer.Mode;

import lombok.extern.slf4j.Slf4j;
import soot.SootField;
import soot.Unit;
import soot.jimple.NopStmt;

import java.util.List;
import java.util.Set;
@Slf4j
public class SDGForwardSlicer extends SDGSlicer{
    public SDGForwardSlicer(DependenceGraph dg, Set<Unit> target) {
        super(dg, target, null);
    }

    public SDGForwardSlicer(DependenceGraph dg, Unit target) {
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
        if (this.dg.getSuccsOf(start) != null) {
            for (final Unit succ : this.dg.getSuccsOf(start)) {
                Edge temp = new Edge(start, succ);
                final Set<Integer> tempTypes = this.dg.getEdgeTypes(temp);
                if (tempTypes.contains(DependenceGraph.EDGE_TYPE_SUMMARY)) {
                    // Add context-sensitivity due to summary edge
                    super.getContextSensitivelyAllowed().add(succ);
                }
                // Check ignored edge type
                if (!visited.contains(succ) && !ignoreEdge(tempTypes, ignoreEdgeTypes, ignoreAlways)) {
                    // Check if field-label is not assigned OR field-label is considered OR anonymous class edge
                    if (this.fieldsToConsider == null || Packs.getInstance().getFieldEdgeLabel(temp) == null
                            || this.fieldsToConsider.contains(Packs.getInstance().getFieldEdgeLabel(temp))
                            || Packs.getInstance().isAnonymousClassEdge(temp))
                    {
                        if (tempTypes.contains(DependenceGraph.EDGE_TYPE_FIELD_DATA)
                                || tempTypes.contains(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA)) {
                            // Re-init context-sensitivity due to (static-)field edge
                            init(succ);
                        }
                        try {
                            if (!this.worklist.contains(succ)) {
                                this.worklist.add(succ);
                            }
                        } catch (final StackOverflowError e) {
                            log.error("A StackOverflowError was thrown while slicing through: " + succ
                                    + ". To avoid please increase the JavaVitualMachine's heap size (launch parameter \"-Xss\" - e.g.: \"-Xss2m\")");
                            return;
                        }
                    }
                }
                temp = null;
            }
        }
    }

    @Override
    protected void init(Unit start) {
        if (!(start instanceof NopStmt)) {
            final Set<Unit> replacedCalls = super.findMethodCalls(start);
            for (final Unit replacedCall : replacedCalls) {
                final Unit originalCall;
                originalCall = Packs.getInstance().getReplacedNodesReplacementToOriginal().getOrDefault(replacedCall, replacedCall);
                if (Packs.getInstance().getCallToReturnNodes().containsKey(originalCall)) {
                    super.getContextSensitivelyAllowed()
                            .add(Packs.getInstance().getCallToReturnNodes().get(originalCall));
                }
            }
        }
    }
}

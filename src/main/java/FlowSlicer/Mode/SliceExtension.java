package FlowSlicer.Mode;

import heros.solver.CountingThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.options.Options;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class SliceExtension {
    private DependenceGraph sdg;
    private boolean isBackward;
    private Set<Unit> toKeep;
    private Set<Unit> toSliceExtra;
    public SliceExtension(DependenceGraph sdg, boolean isBackward, Set<Unit> toKeep, Set<Unit> toSliceExtra) {
        this.sdg = sdg;
        this.isBackward = isBackward;
        this.toKeep = toKeep;
        this.toSliceExtra = toSliceExtra;
    }

//    public void slice() {
//        // Prepare
//        final List<SDGBackwardsSlicer> extraSlicers = new LinkedList<>();
//        for (final Unit unit : this.toSliceExtra) {
//            SDGBackwardsSlicer slicer = new SDGBackwardsSlicer(this.sdg, unit);
//            extraSlicers.add(slicer);
//        }
//
//        // Sort
//        extraSlicers.sort(SDGSlicerComparator.getInstance());
//
//        // Slice
//        for (final SDGBackwardsSlicer slicer : extraSlicers) {
//            boolean subset = false;
//            for (final SDGBackwardsSlicer slicerBefore : extraSlicers) {
//                if (slicerBefore == slicer) {
//                    break;
//                }
//                if (slicerBefore.getGlobalVisit().getContextSensitivity()
//                        .containsAll(slicer.getGlobalVisit().getContextSensitivity())) {
//                    slicer.getGlobalVisit().getVisited().addAll(slicerBefore.getGlobalVisit().getVisited());
//                    if (slicerBefore.getGlobalVisit().getVisited().containsAll(slicer.getStartUnitList())) {
//                        subset = true;
//                        break;
//                    }
//                }
//            }
//            if (!subset) {
//                final Set<Unit> extraSlice = slicer.slice();
//                this.toKeep.addAll(extraSlice);
//            }
//        }
//    }

    public void sliceIncorparatedly () {
        SDGBackwardsSlicer slicer = new SDGBackwardsSlicer(this.sdg, this.toSliceExtra);
        Set<Unit> extraSlice = slicer.slice();
        this.toKeep.addAll(extraSlice);
    }

    public void sliceConcurrently () {
        final List<SDGBackwardsSlicer> extraSlicers = new LinkedList<>();
        for (final Unit unit : this.toSliceExtra) {
            SDGBackwardsSlicer slicer = new SDGBackwardsSlicer(this.sdg, unit);
            extraSlicers.add(slicer);
        }

        ConcurrentMap<Unit, Boolean> toKeepMap = new ConcurrentHashMap<>();
        this.toKeep.forEach(unit -> toKeepMap.put(unit, Boolean.TRUE));
        // Create a list of CompletableFutures for the slicing tasks
        List<CompletableFuture<Set<Unit>>> futures = extraSlicers.stream()
                .map(slicer -> CompletableFuture.supplyAsync(slicer::slice))
                .collect(Collectors.toList());

        // Combine the results of all futures
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> {
            // Merge the results into the toKeep set
            futures.forEach(future -> {
                try {
                    Set<Unit> extraSlice = future.get();
                    extraSlice.forEach(unit -> toKeepMap.put(unit, Boolean.TRUE));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }).join(); // Wait for all tasks to complete

        // Update toKeep with the merged results
        this.toKeep.clear();
        this.toKeep.addAll(toKeepMap.keySet());
    }

    public static Set<Unit> contextSensitiveRefinement(DependenceGraph sdgSliced, Set<Unit> targetListTo, Set<Unit> fromSlice) {
        // Find junctions
        Set<Unit> toRemove = new HashSet<>();
        for (final Unit unit : sdgSliced) {
            if (unit instanceof NopStmt) {
                final SootMethod method = Packs.getInstance().getEntryMethod(unit);
                if (method != null) {
                    final Set<Unit> candidates = new HashSet<>();
                    for (final Unit pred : sdgSliced.getPredsOf(unit)) {
                        if (pred instanceof Stmt) {
                            final Stmt castedPred = (Stmt) pred;
                            if (castedPred.containsInvokeExpr()) {
                                if (method == castedPred.getInvokeExpr().getMethod()) {
                                    candidates.add(pred);
                                }
                            }
                        }
                    }
                    //当调用该函数的边多于一个时，根据上下文敏感信息判断具体那条边是真正的调用关系
                    //判断方式为选择fromSlice中包含的调用语句作为真正的调用边
                    if (candidates.size() >= 2) {
                        if (candidates.removeAll(fromSlice) && !candidates.isEmpty()) {
                            toRemove.addAll(candidates);
                        }
                    }
                }
            }
        }

        // Refine
        sdgSliced.removeNodes(toRemove);
        toRemove = findDeadUnits(sdgSliced, targetListTo);
        sdgSliced.removeNodes(toRemove);
        return toRemove;
    }

    private static Set<Unit> findDeadUnits(DependenceGraph sdgSliced, Set<Unit> targetListTo) {
        final Set<Unit> toRemove = new HashSet<>(sdgSliced.getAllNodes());
        final Set<Unit> visited = new HashSet<>();
        for (Unit targetTo : targetListTo) {
            findDeadUnitsIterative(sdgSliced, targetTo, visited);
        }
        toRemove.removeAll(visited);
        return toRemove;
    }

    private static void findDeadUnitsIterative(DependenceGraph sdgSliced, Unit startUnit, Set<Unit> visited) {
        Stack<Unit> stack = new Stack<>();
        stack.push(startUnit);

        while (!stack.isEmpty()) {
            Unit unit = stack.pop();

            if (!visited.contains(unit)) {
                visited.add(unit);

                if (sdgSliced.getPredsOf(unit) != null) {
                    for (final Unit pred : sdgSliced.getPredsOf(unit)) {
                        if (pred != null && !visited.contains(pred)) {
                            stack.push(pred);
                        }
                    }
                }
            }
        }
    }
}

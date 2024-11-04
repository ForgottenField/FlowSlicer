package FlowSlicer.Mode;

import FlowSlicer.Config;
import FlowSlicer.Global;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

import java.util.*;
@Slf4j
public class CFGSlicer {
    private static final String[] DEBUG_DO_NOT_SLICE = {};
    private DependenceGraph sdgSliced;
    private DependenceGraph sdg;
    private Set<SootMethod> calledMethodsInSDG;
    private Set<SootMethod> calledMethodsInSDGSliced;
    private Set<SootField> occurringFields;

    public CFGSlicer(DependenceGraph sdgSliced, DependenceGraph sdg) {
        this.sdgSliced = sdgSliced;
        this.sdg = sdg;
        this.calledMethodsInSDG = new HashSet<>();
        this.calledMethodsInSDGSliced = new HashSet<>();
        this.occurringFields = new HashSet<>();
    }

    public CFGSlicer() {
        this.sdgSliced = null;
        this.sdg = null;
        this.calledMethodsInSDG = new HashSet<>();
        this.calledMethodsInSDGSliced = new HashSet<>();
        this.occurringFields = new HashSet<>();
    }

    public void sliceout(SootClass sc) {
        final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
        for (final SootMethod sm : snapshotIterator) {
            if (sm.isConcrete()) {
                final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
                if (body != null) {
                    for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
                        Unit unit = i.next();
                        final Unit removeUnit = unit;
                        final Unit replacement = Packs.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
                        if (!this.sdgSliced.getAllNodes().contains(unit)
                                && (replacement != null && this.sdgSliced.getAllNodes().contains(replacement))) {
                            unit = replacement;
                        }
                        if (this.sdgSliced.getAllNodes().contains(unit)) {
                            final Collection<Unit> succs = this.sdg.getSuccsOf(unit);
                            final Collection<Unit> succsSliced = this.sdgSliced.getSuccsOf(unit);
                            if (succs != null) {
                                succs.removeAll(Packs.getInstance().getActualParameterNodes());
                            }
                            if (succsSliced != null) {
                                succsSliced.removeAll(Packs.getInstance().getActualParameterNodes());
                            }

                            if ((succs == null && succsSliced == null) || (succs == null && succsSliced.size() == 0)
                                    || (succsSliced == null && succs.size() == 0)
                                    || (succs != null && succsSliced != null && succs.size() == succsSliced.size())) {
                                System.out.println("Slicing Unit: " + sc.getName() + " -> " + sm.getName() + " -> "
                                        + removeUnit.toString());
                                body.getUnits().remove(removeUnit);
                            }
                        }
                    }
                    cleanUpTraps(body);
                    cleanUpReturns(body);
                }
            }
        }
    }

    public void slice() {
        // Find called methods
        for (Unit unit : this.sdg.getAllNodes()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                final SootMethod calledMethod = stmt.getInvokeExpr().getMethod();
                this.calledMethodsInSDG.add(calledMethod);
            }
        }

        for (Unit unit : this.sdgSliced.getAllNodes()) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                final SootMethod calledMethod = stmt.getInvokeExpr().getMethod();
                this.calledMethodsInSDGSliced.add(calledMethod);
            }
        }

        // Collect occurring fields
        for (final Unit unit : this.sdgSliced.getAllNodes()) {
            for (final ValueBox box : unit.getUseAndDefBoxes()) {
                if (box.getValue() instanceof FieldRef) {
                    occurringFields.add(((FieldRef) box.getValue()).getField());
                }
            }
        }

        List<SootClass> classesToRemove = new ArrayList<>();
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.isConcrete() && !SootHelper.isOrdinaryLibraryClass(sc)) {
                if (Config.getInstance().isPartialSDGConstruction() &&
                        !Global.v().getAppModel().getLocalSootClassSet().contains(sc.getName())) {
                    classesToRemove.add(sc);
                } else {
                    // Slice Fields
                    sliceFields(sc);

                    // Slice Methods
                    sliceMethods(sc);

                    // check empty class
                    boolean delete = true;
                    final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
                    for (final SootMethod sm : snapshotIterator) {
                        if (!sm.isConcrete()) {
                            delete = false;
                            break;
                        }
                        final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
                        if (body == null)
                            continue;
                        if (calledMethodsInSDGSliced.contains(sm) || !isEmpty(body, false)) {
                            delete = false;
                            break;
                        }
                    }

                    if (delete) {
                        classesToRemove.add(sc);
                    }
                }
            }
        }

        // remove class after iteration
        for (SootClass sc : classesToRemove) {
            Scene.v().removeClass(sc);
        }

        // count left sootmethods and statements
        int methodNum = 0;
        int stmtNum = 0;
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.isConcrete() && !SootHelper.isOrdinaryLibraryClass(sc)) {
                methodNum += sc.getMethods().size();
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isConcrete()) {
                        Body body = sm.retrieveActiveBody();
                        if (body != null) {
                            stmtNum += body.getUnits().size();
                        }
                    }
                }
            }
        }
//        Global.v().getAppModel().setMethodsAfterSlicing(methodNum);
        Global.v().getAppModel().setNodesAfterSlicing(stmtNum);
    }

    public void sliceWithoutSDG() {
        List<SootClass> classesToRemove = new ArrayList<>();
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.isConcrete() && !SootHelper.isOrdinaryLibraryClass(sc)) {
                if (Config.getInstance().isPartialSDGConstruction() &&
                        !Global.v().getAppModel().getLocalSootClassSet().contains(sc.getName()))
                {
                    classesToRemove.add(sc);
                }
            }
        }

        // remove class after iteration
        for (SootClass sc : classesToRemove) {
            Scene.v().removeClass(sc);
        }

        // count left sootmethods and statements
        int methodNum = 0;
        int stmtNum = 0;
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.isConcrete() && !SootHelper.isOrdinaryLibraryClass(sc) && !classesToRemove.contains(sc)) {
                methodNum += sc.getMethods().size();
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isConcrete()) {
                        Body body = sm.retrieveActiveBody();
                        if (body != null) {
                            stmtNum += body.getUnits().size();
                        }
                    }
                }
            }
        }
//        Global.v().getAppModel().setMethodsAfterSlicing(methodNum);
        Global.v().getAppModel().setNodesAfterSlicing(stmtNum);
    }

    private void sliceFields(SootClass sc) {
        // Remove fields
        final Collection<SootField> toRemoveFields = new ArrayList<>();
        final Chain<SootField> fields = sc.getFields();
        if (fields != null && !fields.isEmpty()) {
            for (final SootField sf : fields) {
                if (!occurringFields.contains(sf)) {
                    toRemoveFields.add(sf);
                }
            }
            for (final SootField sf : toRemoveFields) {
                fields.remove(sf);
            }
        }
    }

    private void sliceMethods(SootClass sc) {
        // Collect methods
        final Set<SootMethod> toRemoveMethods = new HashSet<>();
        final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
        for (final SootMethod sm : snapshotIterator) {
            if (!sm.isConcrete()) {
                continue;
            }

            // skip <init> and <clinit> methods
//            if ((sm.isConstructor() || sm.isStaticInitializer()) && calledMethodsInSDG.contains(sm))
//                continue;

            final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
            boolean checkNeeded = false;
            for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
                Unit unit = i.next();
                if (ignoreUnit(unit, true, true) || ignoreAnonymousClassSuperConstructorCall(sc, sm, unit)) {
                    continue;
                }

                // Take care of any other units
                final Unit removeUnit = unit;
                final Unit replacement = Packs.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
                if (!this.sdgSliced.getAllNodes().contains(unit)
                        && (replacement != null && this.sdgSliced.getAllNodes().contains(replacement))) {
                    unit = replacement;
                }
                if (!this.sdgSliced.getAllNodes().contains(unit)) {
                    checkNeeded = true;
                    body.getUnits().remove(removeUnit);
                }
            }

            cleanUpTraps(body);

            if (checkNeeded) {
                if (isEmpty(body, false)) {
                    toRemoveMethods.add(sm);
                }
            }
        }

        // Remove methods
        for (final SootMethod sm : toRemoveMethods) {
            if (!sm.isConstructor() && !sm.isStaticInitializer() && sm.isConcrete()) {
                if (!this.calledMethodsInSDGSliced.contains(sm)) {
                    sc.removeMethod(sm);
                } else {
                    // Stumping method, since removing it leads to an ConcurrentModificationException
                    stumpMethod(sm);
                }
            }
        }
    }

    private Set<Unit> getConnectedUnitChain(Unit unit, UnitGraph cfg) {
        final Set<Unit> chain = new HashSet<>();
        chain.add(unit);
        for (final Unit next : cfg.getSuccsOf(unit)) {
            if (cfg.getPredsOf(next).size() == 1 && cfg.getPredsOf(next).iterator().next() == unit) {
                chain.addAll(getConnectedUnitChain(next, cfg));
            }
        }
        return chain;
    }

    private void stumpMethod(SootMethod sm) {
        final Body b = sm.getActiveBody();
        Chain<Unit> units = b.getUnits();
        Unit newUnit = null;
        for (Unit unit : units) {
            // 如果是 Return 语句
            if (unit instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt) unit;
                Value returnValue = returnStmt.getOp(); // 获取 return 语句返回的变量

                // 判断是否是局部变量并且是否已定义
                if (returnValue instanceof Local) {
                    Local returnLocal = (Local) returnValue;

                    // 检查局部变量是否在前文中有定义
                    if (!isLocalDefined(returnLocal, units)) {
                        // 如果未定义，插入一个 new 语句来创建对象
                        newUnit = insertNewInstance(returnLocal, sm);
                    }
                }
            }
        }
        // 将该语句插入到方法的开头，或者可以插入到 return 前
        if (newUnit != null) {
            units.insertBefore(newUnit, units.getFirst());
        }
    }

    private static boolean isLocalDefined(Local local, Chain<Unit> units) {
        // 遍历方法中的语句，检查变量是否有定义
        for (Unit unit : units) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                // 检查变量是否被赋值
                if (assignStmt.getLeftOp().equals(local)) {
                    return true; // 已定义
                }
            }
        }
        return false; // 未定义
    }

    private static Unit insertNewInstance(Local local, SootMethod method) {
        Body body = method.getActiveBody();
        Chain<Unit> units = body.getUnits();

        // 根据变量类型插入相应的 new 语句
        Type localType = local.getType();
        Unit newAssignStmt = null;

        // 如果是引用类型（如类、接口）
        if (localType instanceof RefType) {
            RefType refType = (RefType) localType;
            SootClass localClass = refType.getSootClass();

            // 创建 new 语句，例如 new SomeClass();
            NewExpr newExpr = Jimple.v().newNewExpr(refType);
            newAssignStmt = Jimple.v().newAssignStmt(local, newExpr);
        } else if (localType instanceof ArrayType) {
            // 如果是数组类型，创建数组实例
            ArrayType arrayType = (ArrayType) localType;
            NewArrayExpr newArrayExpr = Jimple.v().newNewArrayExpr(arrayType.getElementType(), IntConstant.v(1)); // 创建长度为1的数组
            newAssignStmt = Jimple.v().newAssignStmt(local, newArrayExpr);

        } else if (localType instanceof PrimType) {
            // 如果是基本类型，设置默认值
            if (localType instanceof IntType) {
                newAssignStmt = Jimple.v().newAssignStmt(local, IntConstant.v(0));  // 整型默认值0
            } else if (localType instanceof FloatType) {
                newAssignStmt = Jimple.v().newAssignStmt(local, FloatConstant.v(0.0f));  // 浮点型默认值0.0f
            } else if (localType instanceof DoubleType) {
                newAssignStmt = Jimple.v().newAssignStmt(local, DoubleConstant.v(0.0));  // 双精度浮点型默认值0.0
            } else if (localType instanceof BooleanType) {
                newAssignStmt = Jimple.v().newAssignStmt(local, IntConstant.v(0));  // 布尔型默认值false
            } // 可根据需要扩展其他基本类型
        }

        // 如果 newAssignStmt 不为空，说明已经创建了实例或赋值语句
        if (newAssignStmt != null) {
            return newAssignStmt;
        } else {
            return null;
        }
    }

    private void cleanUpTraps(Body body) {
        if (body != null && !body.getUnits().isEmpty()) {
            // CleanUp Catch Clauses
            final ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(body);
            final Set<Unit> toRemove = new HashSet<>();
            for (final Unit unit : cfg) {
                if (ignoreCatches(unit) && cfg.getExceptionalPredsOf(unit).isEmpty()) {
                    final Set<Unit> chain = getConnectedUnitChain(unit, cfg);
                    toRemove.addAll(chain);
                }
            }
            for (final Unit cfgUnit : toRemove) {
                body.getUnits().remove(cfgUnit);
            }

            // CleanUp Traps (which do not have any effect)
            for (final Iterator<Trap> i = body.getTraps().snapshotIterator(); i.hasNext();) {
                final Trap t = i.next();
                if (t.getBeginUnit() == t.getEndUnit() && t.getEndUnit() == t.getHandlerUnit()) {
                    body.getTraps().remove(t);
                    if (t.getHandlerUnit() instanceof DefinitionStmt) {
                        if (((DefinitionStmt) t.getHandlerUnit()).getRightOp() instanceof CaughtExceptionRef) {
                            body.getUnits().remove(t.getHandlerUnit());
                        }
                    }
                }
            }
        }
    }

    private void cleanUpReturns(Body body) {
        // CleanUp methods which basically only contain return statements
        boolean noBranchingPossible = true;
        Unit firstReturn = null;
        for (final Unit u : body.getUnits()) {
            if (u instanceof IfStmt || u instanceof soot.jimple.GotoStmt) {
                noBranchingPossible = false;
                break;
            } else if (firstReturn == null && (u instanceof ReturnStmt || u instanceof ReturnVoidStmt)) {
                firstReturn = u;
                break;
            }
        }
        if (noBranchingPossible) {
            boolean returnFound = false;
            for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
                final Unit u = i.next();
                if (returnFound) {
                    body.getUnits().remove(u);
                } else if (u instanceof ReturnStmt) {
                    final Type type = ((ReturnStmt) u).getOp().getType();
                    Value returnValue;
                    if (type instanceof PrimType) {
                        if (type instanceof IntegerType) {
                            returnValue = IntConstant.v(1);
                        } else if (type instanceof DoubleType) {
                            returnValue = DoubleConstant.v(1);
                        } else if (type instanceof FloatType) {
                            returnValue = FloatConstant.v(1);
                        } else if (type instanceof LongType) {
                            returnValue = LongConstant.v(1);
                        } else {
                            returnValue = IntConstant.v(1);
                        }
                    } else {
                        returnValue = NullConstant.v();
                    }
                    body.getUnits().insertBefore(Jimple.v().newReturnStmt(returnValue), u);
                    body.getUnits().remove(u);
                    returnFound = true;
                }
            }
        }
    }

    private boolean isEmpty(Body body, boolean ignoreGotosAndCatches) {
        final UnitPatchingChain units = body.getUnits();
        if (units.isEmpty()) {
            return true;
        }
        for (final Unit unit : units) {
            if (!ignoreUnit(unit, false, ignoreGotosAndCatches)) {
                return false;
            }
        }
        return true;
    }


    private boolean ignoreUnit(Unit unit, boolean ignoreSetContentView, boolean ignoreGotosAndCatches) {
        // Skip this assignment
        if (unit instanceof DefinitionStmt) {
            if (((DefinitionStmt) unit).getRightOp() instanceof ThisRef) {
                return true;
            }
        }
        // Skip parameter assignments
        if (unit instanceof DefinitionStmt) {
            if (((DefinitionStmt) unit).getRightOp() instanceof ParameterRef) {
                return true;
            }
        }
        // Skip return
        if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
            return true;
        }
        // Skip Goto
        if (ignoreGotosAndCatches && ignoreGoto(unit)) {
            return true;
        }
        // Skip Catches
        if (ignoreGotosAndCatches && ignoreCatches(unit)) {
            return true;
        }
        // Skip setContentView method calls
//        if (ignoreSetContentView) {
//            if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
//                    && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
//                final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
//                if (invoke.getMethodRef().getName().equals("setContentView")) {
//                    if (SootHelper.hasActivitySuperClass(invoke.getMethodRef().getDeclaringClass())) {
//                        return true;
//                    }
//                }
//            }
//        }

        // Skip Activity constructor calls
//        if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
//                && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
//            final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
//            if (SootHelper.hasActivitySuperClass(invoke.getMethodRef().getDeclaringClass())) {
//                if (invoke.getMethodRef().getName().equals("<init>")
//                        || SootHelper.isCallBackMethod(invoke.getMethodRef())) {
//                    return true;
//                }
//            }
//        }

        // Skip every constructor call
//        if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
//                && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
//            final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
//            return invoke.getMethodRef().getName().equals("<init>")
//                    || SootHelper.isCallBackMethod(invoke.getMethodRef());
//        }

        return false;
    }

    private boolean ignoreGoto(Unit unit) {
        if (unit instanceof GotoStmt) {
            final Unit target = ((GotoStmt) unit).getTarget();
            final Unit replacedTarget = Packs.getInstance().getReplacedNodesOriginalToReplacement().get(target);
            if (target instanceof ReturnStmt || target instanceof ReturnVoidStmt) {
                return true;
            } else if (this.sdgSliced.getAllNodes().contains(target)
                    || this.sdgSliced.getAllNodes().contains(replacedTarget)) {
                return true;
            }
        }
        return false;
    }

    private boolean ignoreCatches(Unit unit) {
        if (unit instanceof DefinitionStmt) {
            if (((DefinitionStmt) unit).getRightOp() instanceof CaughtExceptionRef) {
                return true;
            }
        }
        return false;
    }

    private boolean ignoreAnonymousClassSuperConstructorCall(SootClass sc, SootMethod sm, Unit unit) {
        if (SootHelper.isAnonymousClass(sc) && sm.isConstructor()) {
            if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
                    && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
                final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
                if (invoke.getMethod().getName().equals("<init>")) {
                    if (SootHelper.getAllAccessibleClasses(sc).contains(invoke.getMethod().getDeclaringClass())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

package FlowSlicer.Mode;

import FlowSlicer.Config;
import FlowSlicer.Global;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;
@Slf4j
public class SootHelper {
    private static Set<String> defaultExcludes;
    private static Set<String> exceptionalIncludes;

    public static Set<SootClass> getAllOuterClasses(SootClass sc) {
        final Set<SootClass> allClasses = new HashSet<>();
        while (sc != null) {
            // extends
            allClasses.add(sc);

            // Continue with outerClass
            sc = sc.getOuterClassUnsafe();
        }
        return allClasses;
    }

    public static Set<SootClass> getAllAccessibleClasses(SootClass sc) {
        final Set<SootClass> allClasses = new HashSet<>();
        while (sc != null) {
            // extends
            allClasses.add(sc);

            // Continue with superClass
            sc = sc.getSuperclassUnsafe();
        }
        return allClasses;
    }

    public static Set<SootClass> getAllAccessibleInterfaces(SootClass sc) {
        return getAllAccessibleInterfaces(sc, new HashSet<>());
    }

    private static Set<SootClass> getAllAccessibleInterfaces(SootClass sc, Set<SootClass> visited) {
        final Set<SootClass> allInterfaces = new HashSet<>();
        if (!visited.contains(sc)) {
            while (sc != null) {
                // extends
                if (sc.isInterface()) {
                    allInterfaces.add(sc);
                }

                // implements
                for (final SootClass interfaceClass : sc.getInterfaces()) {
                    allInterfaces.add(interfaceClass);
                    allInterfaces.addAll(getAllAccessibleInterfaces(interfaceClass, allInterfaces));
                }

                // Continue with superClass
                sc = sc.getSuperclassUnsafe();
            }
        }
        return allInterfaces;
    }

    public static Set<SootClass> getAllAccessibleClassesAndInterfaces(SootClass sc) {
        final Set<SootClass> allClasses = new HashSet<>();
        while (sc != null) {
            // extends
            allClasses.add(sc);

            // implements
            for (final SootClass interfaceClass : sc.getInterfaces()) {
                allClasses.add(interfaceClass);
                allClasses.addAll(getAllAccessibleInterfaces(interfaceClass));
            }

            // Continue with superClass
            sc = sc.getSuperclassUnsafe();
        }
        return allClasses;
    }

    public static Set<SootMethod> getAllAccessibleMethods(Type type) {
        return getAllAccessibleMethods(Scene.v().getSootClass(type.toString()));
    }

    public static Set<SootMethod> getAllAccessibleMethods(SootClass sc) {
        final Set<SootMethod> allMethods = new HashSet<>();
        while (sc != null) {
            // extends
            allMethods.addAll(sc.getMethods());

            // implements
            for (final SootClass interfaceClass : sc.getInterfaces()) {
                allMethods.addAll(interfaceClass.getMethods());
            }

            // Continue with superClass(es)
            sc = sc.getSuperclassUnsafe();
        }
        return allMethods;
    }

    public static boolean isCallBackClass(Value useValue) {
        final String type = useValue.getType().toString();
        final SootClass sc = Scene.v().getSootClassUnsafe(type);
        return isCallBackClass(sc);
    }

    public static boolean isCallBackClass(SootClass sc) {
        while (sc != null) {
            // extends
            if (Packs.getInstance().getCallBackClasses().contains(sc.getName())) {
                return true;
            }
            // implements
            for (final SootClass interfaceClass : sc.getInterfaces()) {
                if (Packs.getInstance().getCallBackClasses().contains(interfaceClass.getName())) {
                    return true;
                }
            }

            // Continue with superClass(es)
            sc = sc.getSuperclassUnsafe();
        }
        return false;
    }

    public static boolean isCallBackMethod(SootMethod sm) {
        // TODO: (Future work) Make this more accurate!
        if (sm.getName().startsWith("on")) {
            // Android Callback
            return true;
        }
        return false;
    }

    public static boolean isCallBackMethod(SootMethodRef sm) {
        // TODO: (Future work) Make this more accurate!
        if (sm.getName().startsWith("on")) {
            // Android Callback
            return true;
        }
        return false;
    }

    public static boolean isAnonymousClass(SootClass sc) {
        if (sc.isInnerClass()) {
            final String innerName = sc.getName().substring(sc.getName().lastIndexOf('$') + 1);
            if (innerName.matches("[0-9]+")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTemporaryField(SootField field) {
        final String innerName = field.getName().substring(field.getName().lastIndexOf('$') + 1);
        if (innerName.matches("[0-9]+")) {
            return true;
        }
        return false;
    }

    public static SootMethod getMethod(Unit unit) {
        if (Packs.getInstance().getEntryMethod(unit) != null) {
            return Packs.getInstance().getEntryMethod(unit);
        } else {
            for (final SootClass sc : Scene.v().getApplicationClasses()) {
                for (final SootMethod sm : sc.getMethods()) {
                    if (sm.isConcrete()) {
                        final Body body = sm.retrieveActiveBody();
                        for (final Unit needle : body.getUnits()) {
                            if (needle == unit
                                    || Packs.getInstance().getReplacedNodesOriginalToReplacement().get(needle) == unit) {
                                return sm;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    public static Body getActiveBodyIfMethodExists(SootMethod sm) {
        try {
            return sm.retrieveActiveBody();
        } catch (final RuntimeException e) {
            return null;
        }
    }

    public static Set<String> getDefaultExcludes() {
        if (defaultExcludes == null) {
            defaultExcludes = new HashSet<>();
            defaultExcludes.addAll(Config.getInstance().getDefaultExcludes());
        }
        return defaultExcludes;
    }

    public static List<String> getExcludeList() {
        final List<String> excludeList = new ArrayList<>();
        String pkgName = Global.v().getAppModel().getPackageName();
        excludeList.addAll(Arrays.asList(new String[] { pkgName + ".BuildConfig", pkgName + ".R", pkgName + ".R$*" }));
        excludeList.addAll(SootHelper.getDefaultExcludes());
        log.info("Excluding: " + excludeList.toString());
        return excludeList;
    }

    public static boolean isOrdinaryLibraryClass(SootClass sootClass) {
        final String classname = sootClass.getName();
        for (final String exclude : getDefaultExcludes()) {
            String strippedExclude = exclude.endsWith("*") ? exclude.substring(0, exclude.length() - 1) : exclude;
            if (classname.startsWith(strippedExclude)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOrdinaryLibraryClass(String sootClassName) {
        for (final String exclude : getDefaultExcludes()) {
            String strippedExclude = exclude.endsWith("*") ? exclude.substring(0, exclude.length() - 1) : exclude;
            if (sootClassName.startsWith(strippedExclude)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOrdinaryLibraryMethod(SootMethod sootMethod) {
        return isOrdinaryLibraryClass(sootMethod.getDeclaringClass());
    }

    public static void reset() {
        exceptionalIncludes = null;
    }

    public static Set<SootMethod> findMethods(String name, int i) {
        final Set<SootMethod> methods = new HashSet<>();
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            for (final SootMethod sm : sc.getMethods()) {
                if (sm.getName().equals(name) && sm.getParameterCount() == i) {
                    methods.add(sm);
                }
            }
        }
        return methods;
    }

    public static Set<SootMethod> findMethodsBySubSig(String subSig) {
        final Set<SootMethod> methods = new HashSet<>();
        for (final SootClass sc : Scene.v().getClasses()) {
            for (final SootMethod sm : sc.getMethods()) {
                if (sm.getSignature().contains(subSig)) {
                    methods.add(sm);
                }
            }
        }
        return methods;
    }

    public static void findCallBackMethods(DependenceGraph sdg) {
        Set<SootMethod> sootMethodSet = new HashSet<>();
        for (Unit unit : sdg) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr().getMethod().getSignature().contains("<android.database.Cursor: java.lang.String getString(int)>")) {
                SootMethod sm = SootHelper.getMethod(unit);
                sootMethodSet.add(sm);
            }
        }
        Stack<SootMethod> stack = new Stack<>();
        Set<SootMethod> visited = new HashSet<>();
        stack.addAll(sootMethodSet);
        while (!stack.isEmpty()) {
            SootMethod sm = stack.pop();
            visited.add(sm);
            for (Edge edge : Global.v().getAppModel().getLifeCycleHandler().getCg()) {
                if (edge.getSrc() != null && edge.getTgt().method().equals(sm) && !visited.contains(edge.getSrc().method())) {
                    stack.push(edge.getSrc().method());
                }
            }
        }
        for (SootMethod sm : visited) {
            if (SootHelper.isCallBackMethod(sm) && SootHelper.isCallBackClass(sm.getDeclaringClass())) {
                System.out.println(sm);
            }
        }
    }

    public static boolean hasActivitySuperClass(SootClass sc) {
        if (sc.getName().equals("android.app.Activity")) {
            return true;
        } else if (sc.getSuperclassUnsafe() == null) {
            return false;
        } else {
            return hasActivitySuperClass(sc.getSuperclass());
        }
    }
}

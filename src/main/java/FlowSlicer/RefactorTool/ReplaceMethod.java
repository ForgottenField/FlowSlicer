package FlowSlicer.RefactorTool;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;

import java.util.*;

public class ReplaceMethod {

    ReplaceMethod() {
    }

    public void replace_method(String classname, String methodname, String rep_method, String add_method) {
        String rep_callback = rep_method.substring(rep_method.indexOf("set") + 3, rep_method.indexOf("Listener"));
        rep_callback = rep_callback.replace("On", "on");
        String add_callback = add_method.substring(add_method.indexOf("set") + 3, add_method.indexOf("Listener"));
        add_callback = add_callback.replace("On", "on");

        for (SootClass sootclass : Scene.v().getApplicationClasses()) {
            if (!sootclass.getName().contains(classname)) continue;

            if (sootclass.getName().contains("$")) {
                String interfacename = add_method.substring(add_method.indexOf("set") + 3);
                Iterator<SootClass> sootclasses = sootclass.getInterfaces().snapshotIterator();
                SootClass interfaceclass = sootclasses.next();
                sootclass.removeInterface(interfaceclass);
                interfaceclass = Scene.v().getSootClass(interfacename);
                sootclass.addInterface(interfaceclass);
            }

            for (SootMethod sootmethod : sootclass.getMethods()) {
                if (sootmethod.getName().equals(rep_callback)) {
                    sootmethod.setName(add_callback);
                    continue;
                }
                if (!sootmethod.getName().contains(methodname)) continue;
                if (sootmethod.isConcrete()) {
                    Body body = sootmethod.retrieveActiveBody();
                    Iterator<Unit> units = body.getUnits().snapshotIterator();
                    while (units.hasNext()) {
                        Unit unit = units.next();
//                        log.info(unit);
                        if (unit instanceof InvokeStmt) {
                            if (!unit.toString().contains(rep_method)) continue;
//                            log.info(unit);
                            InvokeStmt invokestmt = (InvokeStmt) unit;
                            InvokeExpr invokeexpr = invokestmt.getInvokeExpr();
                            SootClass sc = invokeexpr.getMethod().getDeclaringClass();
                            SootMethod sm = sc.getMethodByName(add_method);
                            invokeexpr.setMethodRef(sm.makeRef());
//                            log.info(unit);
                        }
                    }
                }
            }
        }
    }

    public void replace_method(String classname, String methodname, String rep_method) {
        String rep_interfacename = rep_method.substring(rep_method.indexOf("set") + 3);
        for (SootClass sootclass : Scene.v().getApplicationClasses()) {
            if (!sootclass.getName().contains(classname)) continue;
            for (SootMethod sootmethod : sootclass.getMethods()) {
                if (!sootmethod.getName().contains(methodname)) continue;
                if (sootmethod.isConcrete()) {
                    Body body = sootmethod.retrieveActiveBody();
                    Iterator<Local> locals = body.getLocals().snapshotIterator();
                    List<Local> del_locals = new ArrayList<Local>();
                    while (locals.hasNext()) {
                        Local local = locals.next();
                        Type type = local.getType();

//                        if (type.toString().contains("$")) {
//                            del_locals.add(local);
//                        }

                        SootClass classoflocal = Scene.v().getSootClass(type.toString());
                        if (classoflocal.getInterfaceCount() > 0) {
                            Iterator<SootClass> sootclasses = classoflocal.getInterfaces().snapshotIterator();
                            while (sootclasses.hasNext()) {
                                SootClass interfaceclass = sootclasses.next();
                                if (interfaceclass.getName().contains(rep_interfacename)) {
                                    del_locals.add(local);
                                    break;
                                }
                            }
                        }
                    }

                    for (Local del_local : del_locals) {
                        body.getLocals().remove(del_local);
                    }

                    Iterator<Unit> units = body.getUnits().snapshotIterator();
                    List<Unit> del_units = new ArrayList<Unit>();
                    while (units.hasNext()) {
                        Unit unit = units.next();
//                        log.info(unit);
                        if (unit instanceof InvokeStmt) {
                            if (unit.toString().contains(rep_method)) del_units.add(unit);
                            else {
                                InvokeStmt invokestmt = (InvokeStmt) unit;
                                InvokeExpr invokeexpr = invokestmt.getInvokeExpr();
                                SootClass sc = invokeexpr.getMethodRef().getDeclaringClass();
                                if (sc.getInterfaceCount() > 0) {
                                    SootClass interfaceclass = sc.getInterfaces().snapshotIterator().next();
                                    if (interfaceclass.getName().contains(rep_interfacename)) {
                                        del_units.add(unit);
                                    }
                                }
                            }
                        }
                    }

                    for (Unit del_unit : del_units) {
                        body.getUnits().remove(del_unit);
                    }

                    add_timer(body);
                }
            }
        }
    }

    public void add_timer(Body body) {
        SootClass timerclass = Scene.v().getSootClass("java.util.Timer");
        Type type = timerclass.getType();
        Local local = new JimpleLocal("test", type);
        body.getLocals().add(local);

//        for (SootMethod sootmethod : timerclass.getMethods()) {
//            log.info(sootmethod.getSubSignature());
//        }

        Value left = local;
        Value right = Jimple.v().newNewExpr(RefType.v(timerclass));
        AssignStmt assignstmt = Jimple.v().newAssignStmt(left, right);

        SootMethod sootmethod = timerclass.getMethod("void <init>()");
        SootMethodRef sootmethodref = sootmethod.makeRef();
        SpecialInvokeExpr invokeexpr = Jimple.v().newSpecialInvokeExpr(local, sootmethodref);
        InvokeStmt invokestmt = Jimple.v().newInvokeStmt(invokeexpr);

        Iterator<Unit> units = body.getUnits().snapshotIterator();
        Unit unit = null;
        while (units.hasNext()) unit = units.next();

        body.getUnits().insertBefore(assignstmt, unit);
        body.getUnits().insertBefore(invokestmt, unit);
    }
}

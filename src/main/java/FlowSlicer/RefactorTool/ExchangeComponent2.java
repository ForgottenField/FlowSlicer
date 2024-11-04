package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import java.util.*;
@Slf4j
public class ExchangeComponent2 {
	private Map<String, SootClass> classmap;
	private Map<Local, Local> replacemap;
	
	ExchangeComponent2() {
		classmap = new HashMap<String, SootClass>();
		replacemap = new HashMap<Local, Local>();
	}
	public void init_classmap() {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			classmap.put(classname, sootclass);
		}
		log.info("Total: " + classmap.size() + " classes.");
	}
	
	public void exchange_component(String classA, String classB) {
		exchange_component_local(classA, classB);
		exchange_component_unit(classA, classB);
	}
	
	public void exchange_component_local(String classA, String classB) {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String sootclassname = sootclass.getName();
			if (sootclassname.equals(classA) || sootclassname.equals(classB)) continue;
			for (SootMethod sootmethod : sootclass.getMethods()) {
				if (sootmethod.isConcrete()) {
					Body body = sootmethod.retrieveActiveBody();
					Iterator<Local> locals = body.getLocals().snapshotIterator();
					
					Vector<Local> del_locals = new Vector<Local>();
					Vector<Local> add_locals = new Vector<Local>();
					
					while(locals.hasNext()) {
						Local local = locals.next();
						String type = local.getType().toString();
						if (type.equals(classA) || type.equals(classB)) {
//							log.info(sootclass.getName());
							Local add_local = modify_local(local, classA, classB);
							del_locals.addElement(local);
							add_locals.addElement(add_local);
							replacemap.put(local, add_local);
//							log.info(local.getName() + " " + local.getType() + " " + local.getNumber());
//							log.info(add_local.getName() + " " + add_local.getType() + " " + add_local.getNumber());
						}
					}
					
					for(int i = 0; i < del_locals.size(); i++) {
						Local del_local = del_locals.elementAt(i);
						Local add_local = add_locals.elementAt(i);
						body.getLocals().swapWith(del_local, add_local);
					}
					
				}
			}
		}
	}
	
	public void exchange_component_unit(String classA, String classB) {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String sootclassname = sootclass.getName();
			if (sootclassname.equals(classA) || sootclassname.equals(classB)) continue;
			for (SootMethod sootmethod : sootclass.getMethods()) {
				if (sootmethod.isConcrete()) {
					Body body = sootmethod.retrieveActiveBody();
					Iterator<Unit> units = body.getUnits().snapshotIterator();
					
					Vector<Unit> del_units = new Vector<Unit>();
					Vector<Unit> add_units = new Vector<Unit>();
					
					while(units.hasNext()) {
						Unit unit = units.next();
						String strunit = unit.toString();
						
						if (strunit.indexOf(classA) != -1 || strunit.indexOf(classB) != -1) {
//							log.info(sootclass.getName());
//							log.info(unit);
							Unit add_unit = modify_unit((Stmt) unit, classA, classB);
							del_units.addElement(unit);
							add_units.addElement(add_unit);
						}
						
//						for (UnitBox b : unit.getUnitBoxes()) {
//							log.info(b.getUnit());
//						}
						
					}
					
					for(int i = 0; i < del_units.size(); i++) {
						Unit del_unit = del_units.elementAt(i);
						Unit add_unit = add_units.elementAt(i);
						body.getUnits().swapWith(del_unit, add_unit);
					}
					
				}
			}
		}
	}
	
	public Local modify_local(Local origin, String classA, String classB) {
		String localname = origin.getName();
		String type = origin.getType().toString();
		if (type.equals(classA)) {
			SootClass sootclass = classmap.get(classB);
			Local local = Jimple.v().newLocal(localname, sootclass.getType());
//			Local local = Jimple.v().newLocal("abc", RefType.v(sootclass));
			return local;
		}
		else {
			SootClass sootclass = classmap.get(classA);
			Local local = Jimple.v().newLocal(localname, sootclass.getType());
//			Local local = Jimple.v().newLocal("abc", RefType.v(sootclass));
			return local;
		}
	}
	
	public Stmt modify_unit(Stmt stmt, String classA, String classB) {
		if (stmt instanceof AssignStmt) {
			log.info("AssignStmt");
			AssignStmt origin_assignstmt = (AssignStmt) stmt.clone();
			String strstmt = origin_assignstmt.toString();
			
			if (strstmt.indexOf(classA) != -1) {
//				log.info(origin_assignstmt);
				Value left = replacemap.get(origin_assignstmt.getLeftOp());
				Value right = Jimple.v().newNewExpr(RefType.v(classB));
				AssignStmt assignstmt = Jimple.v().newAssignStmt(left, right);
//				log.info(assignstmt);
				return assignstmt;
			}
			else {
//				log.info(origin_assignstmt);
				Value left = replacemap.get(origin_assignstmt.getLeftOp());
				Value right = Jimple.v().newNewExpr(RefType.v(classA));
				AssignStmt assignstmt = Jimple.v().newAssignStmt(left, right);
//				log.info(assignstmt);
				return assignstmt;
			}
		}
		else if (stmt instanceof InvokeStmt) {
			log.info("InvokeStmt");
			InvokeStmt origin_invokestmt = (InvokeStmt) stmt.clone();
			InvokeExpr origin_invokeexpr = origin_invokestmt.getInvokeExpr();
			String strstmt = origin_invokestmt.toString();
			String methodname = origin_invokeexpr.getMethod().getName();
			
			if (strstmt.indexOf(classA) != -1) {
//				log.info(origin_invokestmt);
				Local local = replacemap.get(origin_invokeexpr.getUseBoxes().get(0).getValue());
				SootMethod sootmethod = classmap.get(classB).getMethodByName(methodname);
				SootMethodRef sootmethodref = sootmethod.makeRef();
				SpecialInvokeExpr invokeexpr = Jimple.v().newSpecialInvokeExpr(local, sootmethodref);
				InvokeStmt invokestmt = Jimple.v().newInvokeStmt(invokeexpr);
//				log.info(invokestmt);
				return invokestmt;
			}
			else {
//				log.info(origin_invokestmt);
				Local local = replacemap.get(origin_invokeexpr.getUseBoxes().get(0).getValue());
				SootMethod sootmethod = classmap.get(classA).getMethodByName(methodname);
				SootMethodRef sootmethodref = sootmethod.makeRef();
				SpecialInvokeExpr invokeexpr = Jimple.v().newSpecialInvokeExpr(local, sootmethodref);
				InvokeStmt invokestmt = Jimple.v().newInvokeStmt(invokeexpr);
//				log.info(invokestmt);
				return invokestmt;
			}
		}
		return null;
	}
}

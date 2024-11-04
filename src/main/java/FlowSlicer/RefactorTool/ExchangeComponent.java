package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import java.util.*;
@Slf4j
public class ExchangeComponent {
	private Map<String, SootClass> classmap;
	
	ExchangeComponent() {
		classmap = new HashMap<String, SootClass>();
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
					while(locals.hasNext()) {
						Local local = locals.next();
						String type = local.getType().toString();
						if (type.equals(classA) || type.equals(classB)) {
//							log.info(sootclass.getName());
							modify_local(local, classA, classB);
						}
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
					while (units.hasNext()) {
						Unit unit = units.next();
						String strunit = unit.toString();
						
						if (strunit.contains(classA) || strunit.contains(classB)) {
//							log.info(sootclass.getName());
//							log.info(unit);
							modify_unit((Stmt) unit, classA, classB);
						}
					}
				}
			}
		}
	}
	
	public void modify_local(Local local, String classA, String classB) {
		String type = local.getType().toString();
		if (type.equals(classA)) {
			SootClass sootclass = classmap.get(classB);
			local.setType(sootclass.getType());
		}
		else {
			SootClass sootclass = classmap.get(classA);
			local.setType(sootclass.getType());
		}
	}
	
	public void modify_unit(Stmt stmt, String classA, String classB) {
		if (stmt instanceof AssignStmt) {
			log.info("AssignStmt");
			AssignStmt assignstmt = (AssignStmt) stmt;
			String strstmt = assignstmt.toString();
			if (strstmt.contains(classA)) {
				assignstmt.setRightOp(Jimple.v().newNewExpr(RefType.v(classB)));
			}
			else {
				assignstmt.setRightOp(Jimple.v().newNewExpr(RefType.v(classA)));
			}
			
//			log.info(assignstmt);
		}
		else if (stmt instanceof InvokeStmt) {
			log.info("InvokeStmt");
			InvokeStmt invokestmt = (InvokeStmt) stmt;
			InvokeExpr invokeexpr = invokestmt.getInvokeExpr();
			String strstmt = invokestmt.toString();
			String methodname = invokeexpr.getMethod().getName();
			
			if (strstmt.contains(classA)) {
				SootMethod sootmethod = classmap.get(classB).getMethodByName(methodname);
				invokeexpr.setMethodRef(sootmethod.makeRef());
			}
			else {
				SootMethod sootmethod = classmap.get(classA).getMethodByName(methodname);
				invokeexpr.setMethodRef(sootmethod.makeRef());
			}
			
//			log.info(invokestmt);
		}
	}
}

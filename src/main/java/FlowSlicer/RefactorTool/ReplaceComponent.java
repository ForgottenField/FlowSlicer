package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import java.util.*;
@Slf4j
public class ReplaceComponent {
	private Map<String, SootClass> classmap;
	
	ReplaceComponent() {
		classmap = new HashMap<String, SootClass>();
	}
	
	public void init_classmap() {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			classmap.put(classname, sootclass);
		}
		log.info("Total: " + classmap.size() + " classes.");
	}
	
	public void replace_component(String baseclass, String rep_class, String add_class) {
		replace_component_local(baseclass, rep_class, add_class);
		replace_component_unit(baseclass, rep_class, add_class);
	}

	private void replace_component_local(String baseclass, String rep_class, String add_class) {
		SootClass basesootclass = classmap.get(baseclass);
		
		for (SootMethod sootmethod : basesootclass.getMethods()) {
			if (sootmethod.isConcrete()) {
				Body body = sootmethod.retrieveActiveBody();
				Iterator<Local> locals = body.getLocals().snapshotIterator();
				while(locals.hasNext()) {
					Local local = locals.next();
					String type = local.getType().toString();
					if (type.equals(rep_class)) {
//						log.info(sootclass.getName());
						modify_local(local, add_class);
					}
				}
			}
		}
	}
	
	private void replace_component_unit(String baseclass, String rep_class, String add_class) {
		SootClass basesootclass = classmap.get(baseclass);
		
		for (SootMethod sootmethod : basesootclass.getMethods()) {
			if (sootmethod.isConcrete()) {
				Body body = sootmethod.retrieveActiveBody();
				Iterator<Unit> units = body.getUnits().snapshotIterator();
				while(units.hasNext()) {
					Unit unit = units.next();
					String strunit = unit.toString();
					
					if (strunit.indexOf(rep_class) != -1) {
//						log.info(sootclass.getName());
//						log.info(unit);
						modify_unit((Stmt) unit, add_class);
					}
				}
			}
		}
	}
	
	public void modify_local(Local local, String classname) {
		SootClass sootclass = classmap.get(classname);
		local.setType(sootclass.getType());
	}
	
	public void modify_unit(Stmt stmt, String classname) {
		if (stmt instanceof AssignStmt) {
			log.info("AssignStmt");
			AssignStmt assignstmt = (AssignStmt) stmt;
			assignstmt.setRightOp(Jimple.v().newNewExpr(RefType.v(classname)));
			log.info("assignstmt:"+assignstmt);
		}
		else if (stmt instanceof InvokeStmt) {
			log.info("InvokeStmt");
			InvokeStmt invokestmt = (InvokeStmt) stmt;
			InvokeExpr invokeexpr = invokestmt.getInvokeExpr();
			String strstmt = invokestmt.toString();
			String methodname = invokeexpr.getMethod().getName();
			SootMethod sootmethod = classmap.get(classname).getMethodByName(methodname);
			invokeexpr.setMethodRef(sootmethod.makeRef());
			log.info("invokestmt:"+invokestmt);
		}
	}
}

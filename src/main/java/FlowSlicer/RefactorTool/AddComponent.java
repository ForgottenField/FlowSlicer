package FlowSlicer.RefactorTool;

import soot.*;
import soot.jimple.*;

import java.util.*;

public class AddComponent {
	public void add_component(String add_classname, String prenode) {
		SootClass sootclass;
		sootclass = new SootClass(add_classname, Modifier.PUBLIC);
		sootclass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		sootclass.setApplicationClass();
		
		add_init_method(sootclass);
		add_main_method(sootclass);
        
        add_to_class(sootclass, prenode);
	}
	
	public void add_init_method(SootClass sootclass) {
		SootMethod sootmethod = new SootMethod("<init>", // ������
				Collections.<Type>emptyList(), // �����б�
                VoidType.v(), // ����ֵ����
                Modifier.PUBLIC); // ��������
		
		sootclass.addMethod(sootmethod);
        
        JimpleBody jimplebody = Jimple.v().newBody(sootmethod);
        sootmethod.setActiveBody(jimplebody);
        
        Local local = Jimple.v().newLocal("r0", RefType.v(sootclass));
        jimplebody.getLocals().add(local);
        
        IdentityStmt indentitystmt = Jimple.v().newIdentityStmt(local, Jimple.v().newThisRef(RefType.v(sootclass)));
        jimplebody.getUnits().add(indentitystmt);
        
        SootClass object = Scene.v().getSootClass("java.lang.Object");
		SootMethod initmethod = object.getMethodByName("<init>");
		SootMethodRef initmethodref = initmethod.makeRef();
		SpecialInvokeExpr invokeexpr = Jimple.v().newSpecialInvokeExpr(local, initmethodref);
		InvokeStmt invokestmt = Jimple.v().newInvokeStmt(invokeexpr);
		jimplebody.getUnits().add(invokestmt);
        
        jimplebody.getUnits().add(Jimple.v().newReturnVoidStmt());
	}
	
	public void add_main_method(SootClass sootclass) {
		SootMethod sootmethod = new SootMethod("main", // ������
				Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}), // �����б�
                VoidType.v(), // ����ֵ����
                Modifier.PUBLIC); // ������
        
        sootclass.addMethod(sootmethod);
        
        JimpleBody jimplebody = Jimple.v().newBody(sootmethod);
        jimplebody.insertIdentityStmts();
        sootmethod.setActiveBody(jimplebody);
        
        jimplebody.getUnits().add(Jimple.v().newReturnVoidStmt());
	}
	
	public void add_to_class(SootClass add_class, String prenode) {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			if (!classname.equals(prenode)) continue;
			for (SootMethod sootmethod : sootclass.getMethods()) {
				String methodname = sootmethod.getName();
				if (!methodname.equals("onCreate")) continue;
				
				Body body = sootmethod.retrieveActiveBody();
				
				Local local = Jimple.v().newLocal("test", RefType.v(sootclass));
				body.getLocals().add(local);

				Value left = local;
				Value right = Jimple.v().newNewExpr(RefType.v(sootclass));
				AssignStmt assignstmt = Jimple.v().newAssignStmt(left, right);
//				body.getUnits().add(assignstmt);
				
				SootMethod initmethod = add_class.getMethodByName("<init>");
				SootMethodRef initmethodref = initmethod.makeRef();
				SpecialInvokeExpr invokeexpr = Jimple.v().newSpecialInvokeExpr(local, initmethodref);
				InvokeStmt invokestmt = Jimple.v().newInvokeStmt(invokeexpr);
//				body.getUnits().add(invokestmt);

				Iterator<Unit> units = body.getUnits().snapshotIterator();
				Unit unit = null;
				while (units.hasNext()) unit = units.next();

				body.getUnits().insertBefore(assignstmt, unit);
				body.getUnits().insertBefore(invokestmt, unit);
				
				return;
			}
		}
	}
}

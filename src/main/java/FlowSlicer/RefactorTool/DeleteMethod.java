package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import java.util.*;
@Slf4j
public class DeleteMethod {
	private Map<String, HashSet<String>> map;
	private Map<String, SootMethod> methodmap;
	private Map<SootMethod, SootClass> methodtoclass;
	
	DeleteMethod() {
		map = new HashMap<String, HashSet<String>>();
		methodmap = new HashMap<String, SootMethod>();
		methodtoclass = new HashMap<SootMethod, SootClass>();
	}
	
	public void init_methodmap() {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			for (SootMethod sootmethod : sootclass.getMethods()) {
				String returntype = sootmethod.getReturnType().toString();
				String methodname = sootmethod.getName();
				String key = classname + ": " + returntype + " " + methodname;
				methodmap.put(key, sootmethod);
//				log.info(key);
			}
		}
		log.info("Total: " + methodmap.size() + " methods.");
	}
	
	public void init_graph(String packageName) {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			for (SootMethod sootmethod : sootclass.getMethods()) {
				String returntype = sootmethod.getReturnType().toString();
				String methodname = sootmethod.getName();
				String key = classname + ": " + returntype + " " + methodname;
				map.put(key, new HashSet<String>());
				methodtoclass.put(sootmethod, sootclass);
			}
		}
		
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			for (SootMethod sootmethod : sootclass.getMethods()) {
				if (sootmethod.isConcrete()) {
					Body body = sootmethod.retrieveActiveBody();
					Iterator<Unit> units = body.getUnits().snapshotIterator();
					while(units.hasNext()) {
						Unit unit = units.next();
						if (unit instanceof InvokeStmt) {
							InvokeStmt invokestmt = (InvokeStmt) unit;
							String str = invokestmt.toString();
							if (str.indexOf(packageName) == -1) continue;
							String end = "";
							int idx = str.indexOf("<") + 1;
							while (idx < str.length() && str.charAt(idx) != '(') {
								end += str.charAt(idx);
								idx++;
							}
//							log.info(key);
							
							String returntype = sootmethod.getReturnType().toString();
							String methodname = sootmethod.getName();
							String start = classname + ": " + returntype + " " + methodname;
							if (map.get(end) == null) {
								continue;
							}
							HashSet<String> startset = map.get(end);
							startset.add(start);
						}
					}
				}
			}
		}
	}
	
	public void delete_method(String del_methodname) {
		HashSet<String> set = new HashSet<String>();
		Vector<String> del = new Vector<String>();
		Queue<String> que = new LinkedList<String>();
		set.add(del_methodname);
		del.add(del_methodname);
		que.add(del_methodname);
		while(que.size() > 0) {
			String t = que.remove();
//			log.info(t);
			for (String start : map.get(t)) {
				if (!set.contains(start)) {
					set.add(start);
					del.add(start);
					que.add(start);
				}
			}
		}
		
		int size = del.size();
		for (int i = 0; i < size / 2; i++) {
			String t = del.elementAt(i);
			del.set(i, del.elementAt(size - i - 1));
			del.set(size - i - 1, t);
		}
		
		for (String methodname : del) {
			if (methodmap.get(methodname) == null) {
				System.out.print("ERROR! " + methodname);
			}
			
			SootMethod delmethod = methodmap.get(methodname);
			SootClass sootclass = methodtoclass.get(delmethod);
			log.info(sootclass.getName() + ": " + delmethod.getName());
			sootclass.removeMethod(delmethod);
		}
	}

	public void empty_method(String del_methodname) {
		SootMethod delmethod = methodmap.get(del_methodname);
		if (delmethod.isConcrete()) {
			Body body = delmethod.retrieveActiveBody();
			Iterator<Unit> units = body.getUnits().snapshotIterator();
			while (units.hasNext()) {
				Unit unit = units.next();
				body.getUnits().remove(unit);
			}

			Iterator<Local> locals = body.getLocals().snapshotIterator();
			while (locals.hasNext()) {
				Local local = locals.next();
				body.getLocals().remove(local);
			}
		}

		delmethod.getActiveBody().getUnits().add(Jimple.v().newReturnVoidStmt());

//		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
//			if (!sootclass.getName().contains(classname)) continue;
//			for (SootMethod sootmethod : sootclass.getMethods()) {
//				String methodname = sootmethod.getName();
//				if (!methodname.equals(del_methodname)) continue;
//
//				if (sootmethod.isConcrete()) {
//					Body body = sootmethod.retrieveActiveBody();
//					Iterator<Unit> units = body.getUnits().snapshotIterator();
//					while (units.hasNext()) {
//						Unit unit = units.next();
//						body.getUnits().remove(unit);
//					}
//
//					Iterator<Local> locals = body.getLocals().snapshotIterator();
//					while (locals.hasNext()) {
//						Local local = locals.next();
//						body.getLocals().remove(local);
//					}
//
//					body.add(Jimple.v().newReturnVoidStmt())
//				}
//			}
//		}
	}
}

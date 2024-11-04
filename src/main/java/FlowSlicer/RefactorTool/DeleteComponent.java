package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import java.util.*;
@Slf4j
public class DeleteComponent {
	private Map<String, HashSet<String>> callmap;
	private Map<String, SootClass> sootclassmap;
	
	DeleteComponent() {
		callmap = new HashMap<String, HashSet<String>>();
		sootclassmap = new HashMap<String, SootClass>();
	}
	
	public boolean check_end(char c) {
		if (c >= '0' && c <= '9') return false;
		if (c >= 'a' && c <= 'z') return false;
		if (c >= 'A' && c <= 'Z') return false;
		if (c == '.' || c == '$') return false;
		return true;
	}
	
	public void init_classmap() {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			sootclassmap.put(classname, sootclass);
		}
		log.info("Total: " + sootclassmap.size() + " classes.");
	}

	/**
	 *
	 * question: startset is not used?
	 * @param packageName
	 */
	public void init_graph(String packageName) {
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String classname = sootclass.getName();
			callmap.put(classname, new HashSet<String>());
		}
		
		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
			String callerclass = sootclass.getName();
			for (SootMethod sootmethod : sootclass.getMethods()) {
				if (sootmethod.isConcrete()) {
					Body body = sootmethod.retrieveActiveBody();
//					Body body = sootmethod.getActiveBody();
					Iterator<Unit> units = body.getUnits().snapshotIterator();
					while(units.hasNext()) {
						Unit unit = units.next();
						String strunit = unit.toString();
						if (strunit.contains(packageName)) {
							String calleeclass = packageName;
							int idx = strunit.indexOf(packageName) + packageName.length();
							while (idx < strunit.length() && !check_end(strunit.charAt(idx))) {
								calleeclass += strunit.charAt(idx);
								idx ++;
							}
//							log.info(start + "   ->   " + end);
							HashSet<String> dependencyset = callmap.get(calleeclass);
							if (dependencyset != null) dependencyset.add(callerclass);
						}

//						log.info(unit);
//						for (UnitBox b : unit.getUnitBoxes()) {
//							log.info(b.getUnit());
//						}

					}
				}
			}
		}
	}
	
	public void delete_component(String del_classname) {
		Vector<String> del = new Vector<String>();
		Queue<String> que = new LinkedList<String>();
		del.add(del_classname);
		que.add(del_classname);
		while(que.size() > 0) {
			String t = que.remove();
//			log.info(t);
			if (callmap.get(t) == null) {
				log.info(del_classname + " has been deleted");
				return;
			}
			for (String caller_class : callmap.get(t)) {
				if (!del.contains(caller_class)) {
					del.add(caller_class);
					que.add(caller_class);
				}
			}
		}

		//把即将删除的类集合中元素倒转
		int size = del.size();
		for (int i = 0; i < size / 2; i++) {
			String t = del.elementAt(i);
			del.set(i, del.elementAt(size - i - 1));
			del.set(size - i - 1, t);
		}
		
		for (String classname : del) {
			if (sootclassmap.get(classname) == null) {
				System.out.print("this class has been deleted");
				System.out.print("ERROR! " + classname);
			}
			
			SootClass delclass = sootclassmap.get(classname);
			if(!delclass.isInScene())
				continue;
			try{
				Scene.v().removeClass(delclass);
				log.info(delclass.getName());
			}catch(Exception e){
				log.error(delclass.getName());
			}
		}
	}

	public void delete_component(String packageName, String del_classname) {
		try {
//			SootClass delclass = classmap.get(del_classname);
//			SootClass delclass = Scene.v().getSootClass(del_classname);
//			Scene.v().removeClass(delclass);

			List<SootClass> sootclasses = new ArrayList<SootClass>();
			for (SootClass sootclass : Scene.v().getApplicationClasses()) {
				String classname = sootclass.getName();
				if (classname.contains(del_classname)) {
					sootclasses.add(sootclass);
				}
			}
			for (SootClass delclass : sootclasses) {
				log.info(delclass.getName());
				Scene.v().removeClass(delclass);
			}

//			for (SootClass sootclass : Scene.v().getApplicationClasses()) {
//				for (SootMethod sootmethod : sootclass.getMethods()) {
//					if (sootmethod.isConcrete()) {
//						Body body = sootmethod.retrieveActiveBody();
//						Iterator<Unit> units = body.getUnits().snapshotIterator();
//						while (units.hasNext()) {
//							Unit unit = units.next();
//							String strunit = unit.toString();
//							if (strunit.indexOf(packageName) != -1) {
//								String end = packageName;
//								int idx = strunit.indexOf(packageName) + packageName.length();
//								while (idx < strunit.length() && !check_end(strunit.charAt(idx))) {
//									end += strunit.charAt(idx);
//									idx++;
//								}
//
//								if (end.equals(del_classname)) {
////									log.info(unit);
//									body.getUnits().remove(unit);
//								}
//							}
//
////						log.info(unit);
////						for (UnitBox b : unit.getUnitBoxes()) {
////							log.info(b.getUnit());
////						}
//
//						}
//					}
//				}
//			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void empty_component(String del_classname) {
		SootClass delclass = Scene.v().getSootClass(del_classname);
		for (SootMethod delmethod : delclass.getMethods()) {
			if (delmethod.isConcrete()) {
				Body body = delmethod.retrieveActiveBody();

				Iterator<Unit> units = body.getUnits().snapshotIterator();
				while (units.hasNext()) {
					Unit unit = units.next();
					if (unit instanceof IdentityStmt) continue;
					body.getUnits().remove(unit);
				}

				Iterator<Local> locals = body.getLocals().snapshotIterator();
				while (locals.hasNext()) {
					Local local = locals.next();
					body.getLocals().remove(local);
				}
			}

			try {
				Body body = delmethod.retrieveActiveBody();
				if (body != null) body.getUnits().add(Jimple.v().newReturnVoidStmt());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
//	public void delete_unit(String del_classname) {
//		for (SootClass sootclass : Scene.v().getApplicationClasses()) {
//			List<SootMethod> sootmethods = sootclass.getMethods();
//			for (SootMethod sootmethod : sootmethods) {
//				if (sootmethod.isConcrete()) {
//					Body body = sootmethod.retrieveActiveBody();
//					Iterator<Unit> units = body.getUnits().snapshotIterator();
//					while(units.hasNext()) {
//						Unit unit = units.next();
//						log.info(unit);
//						for (UnitBox b : unit.getUnitBoxes()) {
//							log.info(b.getUnit());
//						}
//						if (classname.indexOf(del_classname) != -1) {
//							log.info(unit);
//						}
//					}
//				}
//			}
//		}
//	}
}

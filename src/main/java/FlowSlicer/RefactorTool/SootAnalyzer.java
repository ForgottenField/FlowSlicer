package FlowSlicer.RefactorTool;

import FlowSlicer.Config;
import FlowSlicer.Global;
import FlowSlicer.Mode.SootHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.draw.geom.GuideIf;
import soot.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.options.*;

import java.io.*;
import java.util.*;

import org.xmlpull.v1.XmlPullParserException;
@Slf4j
public class SootAnalyzer {
	private String apkName;
	private String apkPath;
	
	public SootAnalyzer(){
		this.apkName = Config.getInstance().getAppName();
		this.apkPath = Config.getInstance().getAppPath();
	}
	
	public void sootInit() {
		soot.G.reset();

		Options.v().set_verbose(false);
		Options.v().set_android_jars(Config.getInstance().getAndroidJar());
		Options.v().set_process_dir(Collections.singletonList(apkPath));
		Options.v().set_verbose(false);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().allow_phantom_refs();
		if (Config.getInstance().getOutputFormat().equals("APK")) {
			Options.v().set_output_format(Options.output_format_dex);
		} else if (Config.getInstance().getOutputFormat().equals("JIMPLE")) {
			Options.v().set_output_format(Options.output_format_jimple);
		}

//        String out = "myOutput/FlowSlicer.SplitApkByProgramSlice/" + apkName + "/Apk";
		Options.v().set_output_dir(Config.getInstance().getResultFolder() + Config.getInstance().getAppName() + File.separator + "apk" + File.separator);
		Options.v().set_force_overwrite(true);
		Options.v().set_whole_program(true);
//		Options.v().set_app(true);
//		Options.v().set_keep_line_number(true);
//		Options.v().set_ignore_resolution_errors(true);
//		Options.v().set_no_writeout_body_releasing(true);
		Options.v().set_exclude(SootHelper.getExcludeList());
		Options.v().set_no_bodies_for_excluded(true);

//		Options.v().set_android_api_version(19);
		Options.v().set_process_multiple_dex(true);
		soot.Main.v().autoSetOptions();

//		Options.v().setPhaseOption("jb.cp", "enabled:false");

		Scene.v().loadNecessaryClasses();
		Scene.v().loadBasicClasses();
//		sootRunPack();
//		PackManager.v().getPack("wjpp").apply();
	}

	public void sootRunPack() {
		PackManager.v().runPacks();
	}

	public void sootWriteOutput() {
		PackManager.v().writeOutput();
	}
	public void Delete_Component(String classname) {
		String packageName = Global.v().getAppModel().getPackageName();
		DeleteComponent dc = new DeleteComponent();
		dc.init_classmap();
		dc.init_graph(packageName);
		dc.delete_component(classname);
//		dc.delete_component(packageName, classname);
//		dc.empty_component(classname);
//		dc.delete_unit(classname);
	}
	
	public void Add_Component(String classname, String prenode) {
		String packageName = Global.v().getAppModel().getPackageName();
		AddComponent ac = new AddComponent();
		ac.add_component(packageName + "." + classname, prenode);
	}
	
	public void Exchange_Component(String classA, String classB) {
		ExchangeComponent ec = new ExchangeComponent();
		ec.init_classmap();
		ec.exchange_component(classA, classB);
	}
	
	public void Replace_Component(String baseclass, String rep_class, String add_class) {
		ReplaceComponent rc = new ReplaceComponent();
		rc.init_classmap();
		rc.replace_component(baseclass, rep_class, add_class);
	}
	
	public void Delete_Method(String classname, String methodname, String returntype) {
		String packageName = Global.v().getAppModel().getPackageName();
		String key = classname + ": " + returntype + " " + methodname;
		DeleteMethod dm = new DeleteMethod();
		dm.init_methodmap();
		dm.init_graph(packageName);
//		dm.delete_method(key);
		dm.empty_method(key);
	}

	public void Replace_Method(String classname, String methodname, String rep_method, String add_method) {
		ReplaceMethod rm = new ReplaceMethod();
		rm.replace_method(classname, methodname, rep_method, add_method);
	}

	public void Replace_Method(String classname, String methodname, String rep_method) {
		ReplaceMethod rm = new ReplaceMethod();
		rm.replace_method(classname, methodname, rep_method);
	}
}

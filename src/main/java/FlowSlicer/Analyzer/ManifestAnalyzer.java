package FlowSlicer.Analyzer;

import FlowSlicer.AppModel;
import lombok.extern.slf4j.Slf4j;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.IOException;
@Slf4j
public class ManifestAnalyzer extends Analyzer {
    private ProcessManifest manifest;

    public ManifestAnalyzer() {
        super();
    }

    @Override
    public void analyze() {
        try {
            manifest = new ProcessManifest(appModel.getAppPath());
            appModel.setManifestString(manifest.getAXml().toString());
        } catch (IOException e) {
            log.error(e + ": error reading manifest file!");
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }

        if (manifest == null)
            return;

        appModel.setPackageName(manifest.getPackageName());
        appModel.getPermissionSet().addAll(manifest.getPermissions());

        for (AXmlNode activity : manifest.getAXml().getNodesWithTag("activity")) {
            if (activity.hasAttribute("name")) {
                String name = activity.getAttribute("name").getValue().toString();
                appModel.getActivities().add(name);
                appModel.getComponents().add(name);
            }
        }

        for (AXmlNode provider : manifest.getAXml().getNodesWithTag("provider")) {
            if (provider.hasAttribute("name")) {
                String name = provider.getAttribute("name").getValue().toString();
                appModel.getProviders().add(name);
                appModel.getComponents().add(name);
            }
        }

        for (AXmlNode service : manifest.getAXml().getNodesWithTag("service")) {
            if (service.hasAttribute("name")) {
                String name = service.getAttribute("name").getValue().toString();
                appModel.getServices().add(name);
                appModel.getComponents().add(name);
            }
        }

        for (AXmlNode receiver : manifest.getAXml().getNodesWithTag("receiver")) {
            if (receiver.hasAttribute("name")) {
                String name = receiver.getAttribute("name").getValue().toString();
                appModel.getReceivers().add(name);
                appModel.getComponents().add(name);
            }
        }
    }
}

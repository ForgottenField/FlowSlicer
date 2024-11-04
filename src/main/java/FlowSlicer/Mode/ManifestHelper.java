package FlowSlicer.Mode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.dongliu.apk.parser.ApkFile;

public class ManifestHelper {
    private final Map<String, ApkFile> data;

    private static ManifestHelper instance = new ManifestHelper();

    private ManifestHelper() {
        this.data = new HashMap<>();
    }

    public static ManifestHelper getInstance() {
        return instance;
    }

    synchronized public String getManifestRAW(File apkFile) {
        ApkFile apkParserFile = null;
        try {
            if (apkFile.exists()) {
                final String hash = HashHelper.sha256Hash(apkFile);
                apkParserFile = this.data.get(hash);
                if (apkParserFile == null) {
                    // Instantiate ApkFile from ApkParser
                    try {
                        apkParserFile = new ApkFile(apkFile);
                        this.data.put(hash, apkParserFile);
                    } catch (final Exception e) {
                        System.out.println("apk-parser failed to parse: \"" + apkFile.getAbsolutePath() + "\"");
                        return null;
                    }
                }
                try {
                    return apkParserFile.getManifestXml();
                } catch (final IOException e) {
                    System.out.println("No valid manifest could be found in: \"" + apkFile.getAbsolutePath() + "\"");
                    return null;
                }
            } else {
                System.out.println("Apk file does not exist: \"" + apkFile.getAbsolutePath() + "\"");
                return null;
            }
        } finally {
            if (apkParserFile != null) {
                try {
                    apkParserFile.close();
                } catch (final IOException e) {
                    System.out.println("Read APK information from \"" + apkFile.getAbsolutePath()
                            + "\" but could not close access!");
                }
            }
        }
    }

    public ApkFile getApkParserFile(File apkFile) {
        final String hash = HashHelper.sha256Hash(apkFile);
        if (!this.data.containsKey(hash)) {
            getManifestRAW(apkFile);
        }
        return this.data.get(hash);
    }

//    public ManifestInfo getManifest(File apkFile) {
//        return new ManifestInfo(getManifestRAW(apkFile));
//    }
}

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ConfigPreferences {

    ConfigPreferences(String StatsVersion) {
        if (StatsVersion != null && StatsVersion.length() > 0) {
            m_RLocation += StatsVersion;
        }
    }

    void setRInvokeLibName(String value) {
        setRInfo(m_RInvokeLibNameKey, value);
    }

    void setRPath(String value) {
        setRInfo(m_RPathKey, value);
    }

    void setRInvokeLibPath(String value) {
        setRInfo(m_RInvokeLibPathKey, value);
    }

    void setRPackageInstallPath(String value) {
        setRInfo(m_RPackageInstallPathKey, value);
    }

    String getRPath() {
        return getRInfo(m_RPathKey);
    }

    String getRInvokeLibPath() {
        return getRInfo(m_RInvokeLibPathKey);
    }

    String getRInvokeLibName() {
        return getRInfo(m_RInvokeLibNameKey);
    }

    String getRPackageInstallPath() {
        return getRInfo(m_RPackageInstallPathKey);
    }

    public void removeOneConfigPref(String key) {
        String[] keys = {key};
        removeConfigPrefs(keys, false);
    }

    public void removeAllConfigPrefs() {
        String[] keys = {m_RPathKey, m_RInvokeLibNameKey, m_RInvokeLibPathKey,
                m_RPackageInstallPathKey};
        removeConfigPrefs(keys, true);
    }

    ///////////////////private method///////////////////////////
    private static void removeConfigPrefs(String[] keys, boolean isRemoveNode) {
        try {
            if (Preferences.userRoot().nodeExists(m_RLocation)) {
                Preferences pref = Preferences.userRoot().node(m_RLocation);
                for ( String key : keys ) {
                    pref.remove(key);
                }
                Preferences.userRoot().sync();
                if (isRemoveNode) {
                    pref.removeNode();
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private static void setRInfo(String key, String value) {
        Preferences pref = Preferences.userRoot().node(m_RLocation);
        pref.put(key, value);
    }

    private static String getRInfo(String key) {
        String value = "";
        try {
            if (Preferences.userRoot().nodeExists(m_RLocation)) {
                Preferences pref = Preferences.userRoot().node(m_RLocation);
                value = pref.get(key, value);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        return value;
    }
    ///////////////////data member///////////////////////////
    private static String m_RLocation = "/com/ibm/SPSS/externalr/";
    private static String m_RPathKey = "r_path";
    private static String m_RInvokeLibNameKey = "lib_name";
    private static String m_RInvokeLibPathKey = "lib_path";
    private static String m_RPackageInstallPathKey = "package_path";
}

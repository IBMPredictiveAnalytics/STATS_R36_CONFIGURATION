import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

class ParseJSON {

    ParseJSON(String filePath, HandlePlugin.HandleType type) {
        FileInputStream jsonFile = null;
        try {
            jsonFile = new FileInputStream(new File(filePath));
            this.getJsonParseMethod(jsonFile, type);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (jsonFile != null) {
                    jsonFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    ParseJSON(String filePath) {
        this(filePath, HandlePlugin.HandleType.INSTALL);
    }

    String getRVersion() {
        return m_RVersion;
    }

    String getStatsPkgName() {
        return m_PkgName;
    }

    String getRInvokeLibName() {
        return m_RInvokeLibName;
    }


    ///////////////////private method///////////////////////////
    private void getJsonParseMethod(FileInputStream inputStream, HandlePlugin.HandleType type) throws Exception {
        StringBuilder jarPath = new StringBuilder(ConfigUtil.getStatisticsPath());
        if (!jarPath.toString().endsWith(File.separator)) {
            jarPath.append(File.separator);
        }
        OSInfo osInfo = ConfigUtil.getOSType();
        switch (osInfo) {
            case Linux:
            case MacOSX:
                jarPath.append("bin");
                jarPath.append(File.separator);
                break;
        }
        jarPath.append(m_JsonLibName);

        File jarFile = new File(jarPath.toString());
        URL url = jarFile.toURI().toURL();
        ClassLoader loader = new URLClassLoader(new URL[]{url});
        Class<?> cls = loader.loadClass("com.ibm.json.java.JSONObject");
        Method method = cls.getMethod("parse", InputStream.class);
        Object obj = method.invoke(null, inputStream);

        method = obj.getClass().getSuperclass().getDeclaredMethod("get", Object.class);
        m_RVersion = method.invoke(obj, m_RVersionKey).toString();

        m_PkgName =  method.invoke(obj, m_PkgNameKey).toString();
        m_RInvokeLibName = method.invoke(obj, m_RInvokeLibNameKey).toString();

    }

    ///////////////////data member///////////////////////////
    private String m_PkgName = null;
    private String m_RVersion = null;
    private String m_RInvokeLibName = null;

    private final String m_PkgNameKey = "name";
    private final String m_RVersionKey = "rVersion";
    private final String m_RInvokeLibNameKey = "invokeLibName";

    private final String m_JsonLibName = "json4j-1.0.jar";
}

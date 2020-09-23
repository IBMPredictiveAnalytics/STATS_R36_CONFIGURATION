import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Properties;

import com.ibm.statistics.plugin.StatsException;
import com.ibm.statistics.plugin.StatsUtil;

class ConfigUtil {

    static OSInfo getOSType() throws ConfigException {

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.startsWith("win")) {
            String osArch = System.getProperty("os.arch");
            if (osArch.equalsIgnoreCase("amd64")) {
                return OSInfo.Win64;
            } else {
                return OSInfo.Win32;
            }
        } else if (osName.contains("linux")) {
            return OSInfo.Linux;
        } else if (osName.contains("mac os")) {
            return OSInfo.MacOSX;
        } else {
            throw new ConfigException(getConfigResPropertiesValue("PLATFORM_ERROR"));
        }
    }

    /*
    static boolean checkInternet() throws Exception{

        boolean result = false;
        try {
            final URL url = new URL(m_extURL);
            final URLConnection conn = url.openConnection();
            conn.connect();
            result = true;
        } catch (Exception e) {
            //Can not access the internet.
            throw new ConfigException(getConfigPropertiesValue("INTERNET_ERROR"));
        }

        return result;
    }

    static void downloadFile(String fileURL, String fileName) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            URL url = new URL(fileURL);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();

            File file = new File(fileName);
            if (file.isFile() && file.exists()) {
                file.delete();
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(fileName));

            final int BUFFER_SIZE = 1024;
            byte[] buf = new byte[BUFFER_SIZE];
            int len = 0;

            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static String getExtensionIndexURL() {
        return m_extURL;
    }
    */

    static String getIndexFile() {
        return m_IndexFile;
    }

    static String getSourceName() {
        return m_sourceName;
    }

    static void unZip(String filePath) throws IOException{
        File zipFile = new File(filePath);
        ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry = zipInput.getNextEntry();
        while (entry != null) {
            File target = new File(zipFile.getParent(), entry.getName());
            if ( entry.isDirectory()) {
                target.mkdir();
            } else {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
                byte[] buffer = new byte[4096];
                int read = 0;
                while ((read = zipInput.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                bos.close();
            }
            zipInput.closeEntry();
            entry = zipInput.getNextEntry();
        }
        zipInput.close();
    }

    static String getStatisticsPath() {
        return StatsUtil.getStatisticsPath();
    }

    static String getFileNameWithoutExtension(String fileName) {
        String result = null;
        if (fileName != null && fileName.length() > 0) {
            int pos = fileName.lastIndexOf(".");
            if (pos == -1) {
                result = fileName;
            } else {
                result = fileName.substring(0, pos);
            }
        }
        return result;
    }

    static String getUserHomeFolderWithSep() {
        String homeFolder = System.getProperty("user.home");
        StringBuilder path = new StringBuilder(homeFolder);
        if (!homeFolder.endsWith(File.separator)) {
            path.append(File.separator);
        }

        return path.toString();
    }

    static String getConfigResPropertiesValue(String key) {
        String result = null;
        try {
            Properties prop = new Properties();
            String statsOutPutLang = StatsUtil.getSetting("OLANG");
            StringBuilder resFile = new StringBuilder("ConfigResBundle");
            switch (statsOutPutLang) {
                case "French":
                    resFile.append("_fr");
                    break;
                case "German":
                    resFile.append("_de");
                    break;
                case "Italian":
                    resFile.append("_it");
                    break;
                case "Japanese":
                    resFile.append("_ja");
                    break;
                case "Korean":
                    resFile.append("_ko");
                    break;
                case "Polish":
                    resFile.append("_pl");
                    break;
                case "Russian":
                    resFile.append("_ru");
                    break;
                case "SChinese":
                    resFile.append("_zh_CN");
                    break;
                case "Spanish":
                    resFile.append("_es");
                    break;
                case "TChinese":
                    resFile.append("_zh_TW");
                    break;
                case "BPortugu":
                    resFile.append("_pt_BR");
                    break;
                default:
                    break;
            }
            resFile.append(".properties");
            InputStream is = ClassLoader.getSystemResourceAsStream(resFile.toString());
            prop.load(is);

            result = prop.getProperty(key, "Unknown error.");
        } catch (IOException | StatsException e ) {
            e.printStackTrace();
        }

        return result;
    }

    static String runRProcessWithFirstLineOutput(String[] commands) throws Exception{
        ArrayList<String> result = runRProcess(commands);

        if (result.size() > 0) {
            return result.get(0);
        } else {
            return "";
        }
    }

    static ArrayList<String> runRProcess(String[] commands) throws Exception {
        ArrayList<String> result = new ArrayList<>();
        result.clear();

        Runtime run = Runtime.getRuntime();
        Process process = run.exec(commands);

        // Get inputStream
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = br.readLine();
        while (line != null) {
            result.add(line);
            line = br.readLine();
        }
        br.close();

        // Get errorStream
        br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        line = br.readLine();
        while (line != null) {
            result.add(line);
            line = br.readLine();
        }
        br.close();

        return result;
    }

    static String getConigJarPathWithSep() {
        URL url = ConfigUtil.class.getProtectionDomain().getCodeSource().getLocation();
        String path = null;
        try {
            path = URLDecoder.decode(url.getPath(), "utf-8");
            path = path.substring(0, path.lastIndexOf("/") + 1);
            File file = new File(path);
            path = file.getAbsolutePath();
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    static String getInvokeLibName(String base) throws Exception {
        String name = base;
        OSInfo osInfo = getOSType();
        switch (osInfo) {
            case Win64:
            case Win32:
                name += ".dll";
                break;
            case MacOSX:
                name = "lib" + name;
                name += ".dylib";
                break;
            default:
                name = "lib" + name;
                name += ".so";
        }
        return name;
    }

    static String getStatisticsVersion() {
        StringBuilder version = new StringBuilder();
        if ( isSubscriptionVersion() ) {
            version.append("Subscription");
            version.append(".");
            version.append(DEFAULT_MINOR_VERSION);
        } else {
            version.append(StatsUtil.getStatisticsVersion());
        }

        return version.toString();
    }

    static boolean isSubscriptionVersion() {
        String statsType = System.getProperty("versioninfo.apptype", "");
        boolean result = false;
        if ( statsType.equalsIgnoreCase("Subscription") ) {
            result = true;
        }
        return result;
    }

    static boolean isWritable(String filePath) {
        boolean result = false;
        File file = new File(filePath);
        if (file.exists() && Files.isWritable(file.toPath())) {
            result = true;
        }

        return result;
    }


    ///////////////////data member///////////////////////////
    private final static String m_IndexFile = "index.json";
    private final static String m_sourceName = "Rplugin.zip";
    private final static String DEFAULT_MINOR_VERSION = "0";
}

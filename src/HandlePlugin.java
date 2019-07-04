import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;

public class HandlePlugin {
    static void doInstall(String r_home, String extFolderName) throws Exception {
        String pluginSrcBasePath = getSourcePath(extFolderName);
        String jsonPath = pluginSrcBasePath + ConfigUtil.getIndexFile();

        OSInfo osInfo = ConfigUtil.getOSType();
        String osInfoString = osInfo.toString().toLowerCase();

        String invokeLibDir = m_extensionFolder;
        String scriptDir = m_extensionFolder;

        ParseJSON indexJSON = new ParseJSON(jsonPath, HandleType.INSTALL);
        File rHomeDir = new File(r_home);
        String rHomePath = rHomeDir.getAbsolutePath();
        CheckREnv rEnv = new CheckREnv(rHomePath, indexJSON.getRVersion());
        if (prepareInstallFolder(rEnv.getRscriptExeFilePath(), pluginSrcBasePath)) {
            String pkgName = indexJSON.getStatsPkgName();
            String baseInvokeLibName = indexJSON.getRInvokeLibName();
            String invokeLibName = ConfigUtil.getInvokeLibName(baseInvokeLibName);
            String srcPath = pluginSrcBasePath + osInfoString + File.separator;

            boolean storeToIni = false;
            if (isStoreToDXConfigFile(rHomePath)) {
                scriptDir = getDXConfigDir();

                if (osInfo == OSInfo.Win64 || osInfo == OSInfo.Win32) {
                    invokeLibDir = scriptDir;
                } else {
                    File binFolder = new File(scriptDir);
                    invokeLibDir = binFolder.getParentFile().getAbsolutePath();
                    if (!invokeLibDir.endsWith(File.separator)) {
                        invokeLibDir += File.separator;
                    }
                    invokeLibDir += "lib";
                }

                storeToIni = true;
            }
            copyFile(srcPath, invokeLibDir, invokeLibName);
            String getpkgScript = "getpkgs.R";
            String checkpkgScript = "checkpkgs.R";
            copyFile(pluginSrcBasePath, scriptDir, getpkgScript);
            copyFile(pluginSrcBasePath, scriptDir, checkpkgScript);

            installRPackage(rEnv.getRscriptExeFilePath(), pluginSrcBasePath, pkgName, osInfoString);
            installRPackage(rEnv.getRscriptExeFilePath(), pluginSrcBasePath, m_commonPkgName, osInfoString);

            changeXDConfigFile(pkgName, ConfigUtil.isSubscriptionVersion());

            if (storeToIni) {
                changeDXConfigFile(rHomePath, baseInvokeLibName);
            } else {
                ConfigPreferences pref = new ConfigPreferences(ConfigUtil.getStatisticsVersion());
                pref.setRPackageInstallPath(m_rLibsPath);
                pref.setRInvokeLibName(baseInvokeLibName);
                pref.setRPath(rHomePath);
                pref.setRInvokeLibPath(invokeLibDir);
            }

        } else {
            throw new ConfigException(MessageFormat.format(
                    ConfigUtil.getConfigResPropertiesValue("INSTALL_DIR_ERROR"), m_rLibsPath));
        }
        deleteDir(pluginSrcBasePath);
    }

    static void doUninstall(String extFolderName) throws Exception {
        String pluginSrcBasePath = getSourcePath(extFolderName);
        String jsonPath = pluginSrcBasePath + ConfigUtil.getIndexFile();

        ParseJSON indexJSON = new ParseJSON(jsonPath, HandleType.UNINSTALL);

        String statsPkgName = indexJSON.getStatsPkgName();
        String rInvokeLibName = indexJSON.getRInvokeLibName();

        //Get Invoke Lib name
        String invokeLibName = ConfigUtil.getInvokeLibName(rInvokeLibName);

        if (dxConfigFileIsWritable()) {
            String rHome = getRHomeFromDXConfigFile();
            changeDXConfigFile(null, null, false);

            if (rHome != null && rHome.length() > 0 ) {
                if (!rHome.endsWith(File.separator)) {
                    rHome += File.separator;
                }

                StringBuilder pkgPath = new StringBuilder(rHome);
                pkgPath.append("library");
                pkgPath.append(File.separator);
                pkgPath.append(statsPkgName);

                deleteDir(pkgPath.toString());
            }

            String statsPath = ConfigUtil.getStatisticsPath();
            OSInfo osInfo = ConfigUtil.getOSType();
            StringBuilder invokeLibPath = new StringBuilder(statsPath);
            if (!statsPath.endsWith(File.separator)) {
                invokeLibPath.append(File.separator);
            }

            if (osInfo != OSInfo.Win64 && osInfo != OSInfo.Win32) {
                invokeLibPath.append("lib");
                invokeLibPath.append(File.separator);
            }
            invokeLibPath.append(invokeLibName);

            deleteDir(invokeLibPath.toString());

        } else {
            ConfigPreferences pref = new ConfigPreferences(ConfigUtil.getStatisticsVersion());
            String statsPkg = pref.getRPackageInstallPath();
            if (!statsPkg.endsWith(File.separator)) {
                statsPkg += File.separator;
            }
            statsPkg += statsPkgName;
            //delete Stats package from R library path.
            deleteDir(statsPkg);

            String extensionFolder = m_extensionFolder;
            if (!extensionFolder.endsWith(File.separator)) {
                extensionFolder += File.separator;
            }
            //Delete Invoke Lib
            deleteDir(extensionFolder + invokeLibName);

            //Delete unzip folder
            deleteDir(pluginSrcBasePath);

            pref.removeAllConfigPrefs();
        }
    }

    enum HandleType {
        INSTALL,
        UNINSTALL
    }

    ///////////////////private method///////////////////////////
    private static boolean prepareInstallFolder(String rscript, String path) throws Exception{
        String[] commands ={rscript, "--vanilla", path + m_installRScript, "libraryPath"};
        String rLibsPath = ConfigUtil.runRProcessWithFirstLineOutput(commands);
        rLibsPath = rLibsPath.substring(rLibsPath.indexOf("\"") + 1);

        if (rLibsPath.endsWith("\"")) {
            rLibsPath = rLibsPath.substring(0, rLibsPath.lastIndexOf("\""));
        }

        if (rLibsPath.startsWith("~")) {
            rLibsPath = rLibsPath.replaceFirst("~", System.getProperty("user.home"));
        }
        File rLibsDir = new File(rLibsPath);
        if (rLibsDir.exists() || rLibsDir.mkdirs()) {
            m_rLibsPath = rLibsDir.getAbsolutePath();
            return true;
        } else {
            return false;
        }
    }

    private static void installRPackage(String rscriptExePath, String pkgPath, String spssPkgName, String osName) throws Exception{
        ArrayList<String> commands = new ArrayList<>();
        commands.clear();

        commands.add(rscriptExePath);
        commands.add("--vanilla");
        commands.add(pkgPath + m_installRScript);
        StringBuilder pkgFullName = new StringBuilder(pkgPath);
        pkgFullName.append(osName);
        pkgFullName.append(File.separator);
        pkgFullName.append(spssPkgName);

        OSInfo osInfo = ConfigUtil.getOSType();
        switch (osInfo) {
            case Win64:
            case Win32:
                pkgFullName.append(".zip");
                break;
            case MacOSX:
                pkgFullName.append(".tgz");
                break;
            default:
                pkgFullName.append(".tar.gz");
        }
        commands.add(pkgFullName.toString());

        ArrayList<String> result = ConfigUtil.runRProcess(commands.toArray(commands.toArray(new String[0])));
        for (String line : result) {
            System.out.println(line);
        }
    }

    private static void changeXDConfigFile(String pkgName, boolean isSubscription) throws IOException {
        String statsPath = ConfigUtil.getStatisticsPath();
        String path = m_rLibsPath;
        StringBuilder pathBuilder = new StringBuilder(path);
        if (!path.endsWith(File.separator)) {
            pathBuilder.append(File.separator);
        }

        pathBuilder.append(pkgName).append(File.separator);
        pathBuilder.append(m_xdConfig);
        IniHandler xdCfgIni = IniHandler.loadIniFile(pathBuilder.toString());
        xdCfgIni.writePrivateProfileString("path", m_xdPath, statsPath);

        if (isSubscription) {
            xdCfgIni.addNewSection("version");
            xdCfgIni.writePrivateProfileString("version", "SpssxdVersionType", "Subscription");
        }
    }

    private static String getExtensionInstallFolder(String extFolderName) {
        if (m_extensionFolder == null) {
            String path = ConfigUtil.getConigJarPathWithSep();
            path = path + extFolderName;

            m_extensionFolder = path;
            return path;
        } else {
            return m_extensionFolder;
        }
    }

    private static void deleteDir(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    deleteDir(f.getAbsolutePath());
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    private static void copyFile(String srcFilePath, String destFilePath, String fileName) {
        String srcFileName = new String(srcFilePath);
        if ( !srcFilePath.endsWith(File.separator) ) {
            srcFileName += File.separator;
        }
        String destFileName = new String(destFilePath);
        if ( !destFilePath.endsWith(File.separator) ) {
            destFileName += File.separator;
        }
        srcFileName += fileName;
        destFileName += fileName;
        File srcFile = new File(srcFileName);
        File destFile = new File(destFileName);

        FileChannel inChannel = null;
        FileChannel outChannel = null;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(srcFile);
            outputStream = new FileOutputStream(destFile);
            inChannel = inputStream.getChannel();
            outChannel = outputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (inChannel.read(buffer) != -1) {
                buffer.flip();
                outChannel.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getSourcePath(String extFolderName) throws Exception {
        String dir = getExtensionInstallFolder(extFolderName);
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        String sourceName = ConfigUtil.getSourceName();
        ConfigUtil.unZip(dir + sourceName);
        String pluginFolder = ConfigUtil.getFileNameWithoutExtension(sourceName);
        String pluginSrcPath = dir + pluginFolder + File.separator;

        return pluginSrcPath;
    }

    private static String getDXConfigDir() throws Exception{
        String statsPath = ConfigUtil.getStatisticsPath();
        if (!statsPath.endsWith(File.separator)) {
            statsPath += File.separator;
        }
        StringBuilder dxIniDir = new StringBuilder(statsPath);
        OSInfo osInfo = ConfigUtil.getOSType();
        switch (osInfo) {
            case Linux:
            case MacOSX:
                dxIniDir.append("bin");
                dxIniDir.append(File.separator);
                break;
        }
        return dxIniDir.toString();
    }

    private static boolean isStoreToDXConfigFile(String rHome) throws Exception{
        boolean result = false;

        String rDefaultLibDir = rHome;
        if (!rDefaultLibDir.endsWith(File.separator)) {
            rDefaultLibDir += File.separator;
        }
        rDefaultLibDir += "library";
        if (dxConfigFileIsWritable() && ConfigUtil.isWritable(rDefaultLibDir)) {
            result = true;
        }

        return result;
    }

    private static boolean dxConfigFileIsWritable() throws Exception {
        boolean result = false;

        String dxIniDir = getDXConfigDir();
        if (ConfigUtil.isWritable(dxIniDir)) {
            String dxIniPath = dxIniDir + m_dxConfig;
            if (ConfigUtil.isWritable(dxIniPath)) {
                result = true;
            }
        }

        return result;
    }

    private static void changeDXConfigFile(String rHome, String libName, boolean isInstall) throws Exception {
        String dxIniDir = getDXConfigDir();

        if (ConfigUtil.isWritable(dxIniDir)) {
            String dxIniPath = dxIniDir + m_dxConfig;

            if (ConfigUtil.isWritable(dxIniPath)) {
                IniHandler dxCfgIni = IniHandler.loadIniFile(dxIniPath);
                if (isInstall) {
                    dxCfgIni.writePrivateProfileString("R", m_dxHomeKeyName, rHome);
                    dxCfgIni.writePrivateProfileString("R", m_dxLibKeyName, libName);
                } else {
                    dxCfgIni.writePrivateProfileString("R", null, null);
                }
            }
        }
    }

    private static void changeDXConfigFile(String rHome, String libName) throws Exception {
        changeDXConfigFile(rHome, libName, true);
    }

    private static String getRHomeFromDXConfigFile() throws Exception {
        String dxIniDir = getDXConfigDir();
        String dxIniPath = dxIniDir + m_dxConfig;
        IniHandler dxCfgIni = IniHandler.loadIniFile(dxIniPath);
        LinkedList<String> homeList = dxCfgIni.getPrivateProfileString("R", "HOME");
        if (homeList != null && homeList.size() > 0) {
            return homeList.getFirst();
        } else {
            return "";
        }
    }


    ///////////////////data member///////////////////////////
    private final static String m_installRScript = "installPkg.R";
    private final static String m_xdConfig = "spssxdcfg.ini";
    private final static String m_dxConfig = "spssdxcfg.ini";
    private final static String m_xdPath = "spssxd_path";
    private final static String m_commonPkgName = "spssstatistics";
    private final static String m_dxHomeKeyName = "HOME";
    private final static String m_dxLibKeyName = "LIB_NAME";
    private static String m_rLibsPath = null;

    private static String m_extensionFolder = null;
}

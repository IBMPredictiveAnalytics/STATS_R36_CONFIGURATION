import java.io.File;
import java.text.MessageFormat;

class CheckREnv {

    CheckREnv(String home, String expcRMajorVer) throws Exception{

        boolean validHome = isValidHome(home);
        if (validHome) {
            m_RVersionString = getRVersion();
            m_RMajorString = getRMajor();
            m_RMinorString = getRMinor();

            if (m_RMajorString != null && m_RMajorString.equalsIgnoreCase(expcRMajorVer)) {
                m_isRVersionMatched = true;
            }
        }
        if (!validHome || !m_isRVersionMatched) {
            throw new ConfigException(MessageFormat.format(
                    ConfigUtil.getConfigResPropertiesValue("R_PATH_ERROR"), expcRMajorVer, home));
        }
    }

    String getRExeFilePath() {
        return m_RExeFile;
    }

    String getRscriptExeFilePath() {
        return m_RscriptExeFile;
    }

    String getRVersionString() {
        return m_RVersionString;
    }

    String getRMajorString() {
        return m_RMajorString;
    }

    String getRMinorString() {
        return m_RMinorString;
    }

    boolean isMatched() {
        return m_isRVersionMatched;
    }

    ///////////////////private method///////////////////////////
    private boolean isValidHome(String home) throws Exception{
        boolean result = false;

        File dirName = new File(home);
        if (dirName.isDirectory()) {
            OSInfo osInfo = ConfigUtil.getOSType();

            StringBuilder RFileName = new StringBuilder(home);
            if (!home.endsWith(File.separator)) {
                RFileName.append(File.separator);
            }

            RFileName.append("bin").append(File.separator);

            RFileName.append("R");

            if ( osInfo == OSInfo.Win64 || osInfo == OSInfo.Win32 ) {
                RFileName.append(".exe");
            }

            File RFile = new File(RFileName.toString());
            if (RFile.isFile() && RFile.exists() && RFile.canExecute()) {
                m_RExeFile = RFile.getAbsolutePath();
                result = true;
            }

            if (result) {
                String RscriptFilePath = RFile.getParentFile().getAbsolutePath();
                if (!RscriptFilePath.endsWith(File.separator)) {
                    RscriptFilePath += File.separator;
                }

                RscriptFilePath += "Rscript";
                if ( osInfo == OSInfo.Win64 || osInfo == OSInfo.Win32 ) {
                    RscriptFilePath += ".exe";
                }
                File RscriptFile = new File(RscriptFilePath);
                if (!RscriptFile.isFile() || !RscriptFile.exists() || !RscriptFile.canExecute()) {
                    result = false;
                } else {
                    m_RscriptExeFile = RscriptFilePath;
                }
            }
        }

        return result;
    }

    private String getRVersion() throws Exception {

        String[] commands = {m_RExeFile, "--version"};
        String verString = ConfigUtil.runRProcessWithFirstLineOutput(commands);
        verString = verString.substring(10);
        verString = verString.substring(0, verString.indexOf(" "));

        return verString;
    }

    private String getRMajor() {
        return m_RVersionString.substring(0, m_RVersionString.lastIndexOf('.'));
    }

    private String getRMinor() {
        return m_RVersionString.substring(m_RVersionString.lastIndexOf('.') + 1);
    }

    ///////////////////data member///////////////////////////
    private String m_RExeFile = null;
    private String m_RscriptExeFile = null;
    private String m_RVersionString = null;
    private String m_RMajorString = null;
    private String m_RMinorString = null;
    private boolean m_isRVersionMatched = false;
}

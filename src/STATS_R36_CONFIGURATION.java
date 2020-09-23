import com.ibm.statistics.plugin.*;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class STATS_R36_CONFIGURATION {

    public static void Run(Hashtable args) throws Exception {

        try {
            List<Object> valList = Arrays.asList(
                    (Object)"install",
                    "uninstall"
            );
            List<TemplateClass> templateList = Arrays.asList(
                    //Extension.Template("TYPE", "", "str", Arrays.asList((Object)"install", (Object)"uninstall")),
                    Extension.Template("R_HOME", "", "literal"),
                    Extension.Template("ACTION", "", "str", valList),
                    Extension.Template("HELP", "", "bool"));

            SyntaxClass oobj = Extension.Syntax(templateList);
            if (args.containsKey("HELP")) {
                System.out.println(HELP_TEXT);
            } else {
                Extension.processcmd(oobj, args, "exec", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exec(@ParamName("r_home") String r_home, @ParamName("action") String action ) throws Exception  {

        String extensionName = this.getClass().getName();
        if (action == null || action.equalsIgnoreCase(sInstallAction)) {
            try {
                HandlePlugin.doInstall(r_home, extensionName);
            } catch (ConfigException e) {
                Object[][] data = {{e.getMessage()}};
                Object[] rowLabels = {""};
                Object[] colLabels = {""};
                StatsUtil.startProcedure(this.getClass().getName().replace("_", " "));
                PivotTable warningsTable = new PivotTable(data, rowLabels, colLabels);
                warningsTable.setTitle("Warnings ");
                warningsTable.setHideColDimLabel(true);
                warningsTable.setHideRowDimLabel(true);
                warningsTable.setHideRowDimTitle(true);
                warningsTable.setHideColDimTitle(true);
                warningsTable.createSimplePivotTable();
                StatsUtil.endProcedure();
            }
        } else if (action.equalsIgnoreCase(sUninstallAction)) {
            HandlePlugin.doUninstall(extensionName);
        }
    }

    ///////////////////data member///////////////////////////
    private static String HELP_TEXT = "This Extension will download and install R Essentials on your machine";
    private final static String sInstallAction = "install";
    private final static String sUninstallAction = "uninstall";
}
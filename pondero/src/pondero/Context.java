package pondero;

import static pondero.Logger.critical;
import static pondero.Logger.debug;
import static pondero.Logger.info;
import static pondero.Logger.trace;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import pondero.data.Workbook;
import pondero.data.WorkbookFactory;
import pondero.tests.Test;
import pondero.tests.management.Artifact;
import pondero.ui.exceptions.ExceptionReporting;
import pondero.util.OsUtil;
import pondero.util.StringUtil;

public final class Context {

    public static Collection<Artifact> getArtifacts() {
        return ARTIFACTS.values();
    }

    public static Workbook getDefaultWorkbook() throws Exception {
        return WorkbookFactory.openWorkbook(Context.getDefaultWorkbookFile());
    }

    public static File getDefaultWorkbookFile() {
        return new File(getFolderResults(), DEFAULT_WORKBOOK_NAME);
    }

    public static File getFolderBin() {
        return getFolder("bin");
    }

    public static File getFolderCfg() {
        return getFolder("cfg");
    }

    public static File getFolderHome() {
        return homeFolder;
    }

    public static File getFolderLog() {
        return getFolder("log");
    }

    public static File getFolderResults() {
        return getFolder("results");
    }

    public static File getFolderResultsTemp() {
        return getFolder("results/temp");
    }

    public static File getFolderTests() {
        return getFolder("tests");
    }

    public static int getFrameRate() {
        return FRAME_RATE;
    }

    public static File getLastWorkbookFile() {
        if (lastWorkbookFile != null) { return lastWorkbookFile; }
        return getDefaultWorkbookFile();
    }

    public static Locale getLocale() {
        return new Locale(uiLocaleString);
    }

    public static Artifact getPonderoArtifact() {
        return ARTIFACTS.get("PONDERO");
    }

    public static Test getTest(final String testName) {
        for (final Test test : REGISTERED_TESTS) {
            if (test.getDescriptor().getId().equals(testName)) { return test; }
        }
        return null;
    }

    public static String getThemeString() {
        return uiThemeString;
    }

    public static double getUiFontScaleFactor() {
        return uiFontScaleFactor;
    }

    public static synchronized void init(String homeFolderName) throws Exception {
        if (homeFolder == null) {
            if (StringUtil.isNullOrBlank(homeFolderName)) {
                runningFromIde = true;
                homeFolderName = getDefaultHomeFolderName();
            } else {
                runningFromIde = false;
            }
            homeFolder = new File(homeFolderName);
            if (!homeFolder.exists()) {
                homeFolder.mkdirs();
            }
            propertiesFile = new File(getFolderCfg(), PROPERTIES_FILE_NAME);
            if (propertiesFile.exists()) {
                final Reader propertiesReader = new FileReader(propertiesFile);
                final Properties properties = new Properties();
                properties.load(propertiesReader);
                propertiesReader.close();
                String foo = null;
                foo = properties.getProperty(CONSOLE_LOG_LEVEL_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    Logger.maxConsoleLevel = Integer.parseInt(foo.trim());
                }
                foo = properties.getProperty(FILE_LOG_LEVEL_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    Logger.maxFileLevel = Integer.parseInt(foo.trim());
                }
                foo = properties.getProperty(UI_LOCALE_STRING_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    uiLocaleString = foo;
                }
                foo = properties.getProperty(UI_THEME_STRING_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    uiThemeString = foo.trim();
                }
                foo = properties.getProperty(UI_SCALE_FACTOR_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    uiFontScaleFactor = Double.parseDouble(foo);
                }
                foo = properties.getProperty(UPDATE_ON_STARTUP_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    updateOnStartup = Boolean.parseBoolean(foo.trim().toLowerCase());
                    updateOnStartup = false;
                }
                foo = properties.getProperty(LAST_WORKBOOK_FILE_KEY);
                if (!StringUtil.isNullOrBlank(foo)) {
                    lastWorkbookFile = new File(foo);
                    if (!lastWorkbookFile.exists()) {
                        lastWorkbookFile = null;
                    }
                }
            } else {
                savePreferences();
            }

            L10n.init(getLocale());

            if (OsUtil.isMacOSX()) {
                try {
                    System.setProperty("com.apple.mrj.application.apple.menu.about.name", L10n.getString("lbl.pondero"));
                    System.setProperty("com.apple.macos.useScreenMenuBar", "true");
                    System.setProperty("apple.laf.useScreenMenuBar", "true"); // for older versions of Java
                } catch (final SecurityException e) {
                    /* probably running via webstart, do nothing */
                }
            }
            if (OsUtil.isWindows()) {
                // System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
            }

            info("home folder: ", homeFolder.getCanonicalPath());
            debug("properties file: ", propertiesFile.getCanonicalPath());
            trace("property: ", CONSOLE_LOG_LEVEL_KEY, "=", Logger.maxConsoleLevel);
            trace("property: ", FILE_LOG_LEVEL_KEY, "=", Logger.maxFileLevel);
            trace("property: ", UI_LOCALE_STRING_KEY, "=", uiLocaleString);
            trace("property: ", UI_THEME_STRING_KEY, "=", uiThemeString);
            trace("property: ", UI_SCALE_FACTOR_KEY, "=", uiFontScaleFactor);
            trace("property: ", UPDATE_ON_STARTUP_KEY, "=", updateOnStartup);
            registerArtifact(Artifact.fromJarFile(new File(getFolderBin(), "pondero.jar")));
            registerArtifact(Artifact.fromJarFile(new File(getFolderBin(), "pondero-install.jar")));
        }
    }

    public static synchronized void initForTesting() throws Exception {
        init(null);
    }

    public static boolean isBackupWorkbookOnOpen() {
        return true;
    }

    public static boolean isParticipantOptional() {
        return isRunningFromIde() && false;
    }

    public static boolean isRunningFromIde() {
        return runningFromIde;
    }

    public static boolean isUpdateOnStartup() {
        return updateOnStartup;
    }

    public static void registerArtifact(final Artifact artifact) {
        if (artifact != null && !ARTIFACTS.containsKey(artifact.getId())) {
            ARTIFACTS.put(artifact.getId(), artifact);
            critical("registering artifact: " + artifact.getCodeName());
        }
    }

    public static void savePreferences() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(CONSOLE_LOG_LEVEL_KEY, String.valueOf(Logger.maxConsoleLevel));
        properties.setProperty(FILE_LOG_LEVEL_KEY, String.valueOf(Logger.maxFileLevel));
        properties.setProperty(UI_LOCALE_STRING_KEY, String.valueOf(uiLocaleString));
        properties.setProperty(UI_THEME_STRING_KEY, String.valueOf(uiThemeString));
        properties.setProperty(UI_SCALE_FACTOR_KEY, String.valueOf(uiFontScaleFactor));
        properties.setProperty(UPDATE_ON_STARTUP_KEY, String.valueOf(updateOnStartup));
        if (lastWorkbookFile != null) {
            properties.setProperty(LAST_WORKBOOK_FILE_KEY, lastWorkbookFile.getCanonicalPath());
        }
        final Writer propertiesWriter = new FileWriter(propertiesFile);
        properties.store(propertiesWriter, null);
        propertiesWriter.close();
    }

    public static void setLastWorkbookFile(final File file) {
        lastWorkbookFile = file;
        try {
            savePreferences();
        } catch (final Exception e) {
            ExceptionReporting.showExceptionMessage(null, e);
        }
    }

    public static void setLocale(final Locale value) {
        uiLocaleString = value.toString();
    }

    public static void setUiFontScaleFactor(final Double value) {
        uiFontScaleFactor = value;
    }

    public static void setUiThemeString(final String value) {
        uiThemeString = value;
    }

    private static String getDefaultHomeFolderName() {
        if (OsUtil.isWindows()) { return DEFAULT_HOME_FOLDER_NAME; }
        if (OsUtil.isMacOSX()) { return System.getProperties().getProperty("user.home") + DEFAULT_HOME_FOLDER_NAME; }
        return null;
    }

    private static File getFolder(final String name) {
        try {
            final File folder = new File(homeFolder.getCanonicalPath(), name);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return folder;
        } catch (final IOException e) {
            ExceptionReporting.showExceptionMessage(null, e);
            return null;
        }
    }

    private static final String                CONSOLE_LOG_LEVEL_KEY    = "consoleLogLevel";
    private static final String                FILE_LOG_LEVEL_KEY       = "fileLogLevel";
    private static final String                LAST_WORKBOOK_FILE_KEY   = "lastWorkbookFile";
    private static final String                UI_LOCALE_STRING_KEY     = "uilocaleString";
    private static final String                UI_THEME_STRING_KEY      = "uiThemeString";
    private static final String                UI_SCALE_FACTOR_KEY      = "uiFontScaleFactor";
    private static final String                UPDATE_ON_STARTUP_KEY    = "updateOnStartup";
    private static final String                PROPERTIES_FILE_NAME     = "pondero.properties";

    private static final String                DEFAULT_HOME_FOLDER_NAME = "/Pondero";
    private static final String                DEFAULT_WORKBOOK_NAME    = "default.xlsx";

    private static final int                   FRAME_RATE               = 15;

    private static final Map<String, Artifact> ARTIFACTS                = new LinkedHashMap<String, Artifact>();

    private static boolean                     runningFromIde;
    private static File                        homeFolder;
    private static File                        propertiesFile;
    private static File                        lastWorkbookFile;

    private static String                      uiLocaleString           = "ro";
    private static String                      uiThemeString            = "Nimbus";
    private static boolean                     updateOnStartup          = false;
    private static double                      uiFontScaleFactor        = 1.25;

    public static final List<Test>             REGISTERED_TESTS         = new ArrayList<Test>();

    private Context() {
    }

}

/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.script.support;

import org.apache.commons.cli.CommandLine;
import org.sikuli.basics.*;
import org.sikuli.natives.WinUtil;
import org.sikuli.script.*;
import org.sikuli.script.runners.ProcessRunner;
import org.sikuli.script.support.IScriptRunner.EffectiveRunner;
import org.sikuli.util.CommandArgs;
import org.sikuli.util.CommandArgsEnum;
import org.sikuli.util.Highlight;
import org.sikuli.vnc.VNCScreen;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * INTERNAL USE --- NOT official API<br>
 */
public class RunTime {

  //<editor-fold desc="00 static">
  private RunTime() {
    RunTime.logCallStack(2, "RunTime: instantiation");
  }

  private static Class runTimeClass = RunTime.class;

  static final long started = new Date().getTime();
  static final long obsolete = started - 2 * 24 * 60 * 60 * 1000;

  public static int javaVersion = 0;
  public static int javaArch = 32;
  public static String javaShow = "not-set";
  public static String SXJavaVersion;

  public static Properties sysProps = System.getProperties();
  public static final String sysPropJavaHome = System.getProperty("java.home");
  public static final String sysPropJRTVersion = System.getProperty("java.runtime.version");
  public static final String sysPropJSpecVersion = System.getProperty("java.specification.version");
  public static final String sysPropJVmVersion = System.getProperty("java.vm.version");
  public static final String sysPropJClassVersion = System.getProperty("java.class.version");
  public static final String sysPropSunArchDataModel = System.getProperty("sun.arch.data.model");
  public static final String sysPropJIoTmpdir = System.getProperty("java.io.tmpdir");
  public static final String sysPropJClassPath = System.getProperty("java.class.path");

  public static final String sysPropUserHome = System.getProperty("user.home");
  public static File fUserHome = new File(sysPropUserHome);
  public static final String sysPropUserDir = System.getProperty("user.dir");
  public static File fWorkDir = new File(sysPropUserDir);
  public static final String sysPropUserName = System.getProperty("user.name");

  public static final String sysPropOsName = System.getProperty("os.name");
  public static final String sysPropOsVersion = System.getProperty("os.version");
  public static final String sysPropOsArch = System.getProperty("os.arch");
  public static String SXSystemVersion;

  public static final String sysPropLineSep = System.getProperty("line.separator");
  public static final String sysPropPathSep = System.getProperty("path.separator");

  public static String SXVersion = "";
  public static int SikuliVersionMajor;
  public static int SikuliVersionMinor;
  public static int SikuliVersionSub;
  public static String SXBuild = "";
  public static String SXBuildNumber = "";
  public static String SXBuildStamp = "";
  public static String SXVersionIDE;
  public static String SXVersionAPI;
  public static String SXVersionLong;
  public static String SXVersionShort;

  public static String osName = "NotKnown";
  public static String sysName = "NotKnown";
  public static String osVersion = "";
  private static final String osNameShort = sysPropOsName.substring(0, 1).toLowerCase();
  public static boolean runningWindows = false;
  public static boolean runningMac = false;
  public static boolean runningLinux = false;
  public static String linuxDistro = "???LINUX???";

  private static String sxSandbox = System.getProperty("sikuli.Sandbox");
  private static File sxAppDataFolder = null;
  public static String appDataMsg = "";
  public static File fSikulixExtensions = null;
  public static File fSikulixLib = null;
  public static File fSikulixStore;
  public static File fTempPath = null;
  public static File fBaseTempPath = null;
  public static String fpBaseTempPath = "";

  public static String fpJarLibs = "/sikulixlibs/";
  public static File fLibsFolder = null;

  private static String fpContent = "sikulixcontent";
  public static File fSxBaseJar = null;

  private static File sxRunningJar = null;
  private static boolean runningAsIDE = true;
  private static String runType = "INIT";
  private static File isRunning = null;
  private static FileOutputStream isRunningFile = null;
  private static String isRunningFilename = "s_i_k_u_l_i-ide-isrunning";

  private static List<URL> classPathActual = new ArrayList<>();
  private static List<String> classPathList = new ArrayList<>();

  private static boolean optTesting = false;
  private static boolean testing = false;

  private static String runningAs = "OTHER";
  private static boolean isTerminating = false;

  public static final String libOpenCV = "opencv_java"; //Core.NATIVE_LIBRARY_NAME;

  public static void logCallStack(int level, String message) {
    if (RunTime.isDevelop()) {
      System.out.println("--------------------------------CallStack---------------------------------> " + message);
      StackTraceElement[] elements = new Exception().getStackTrace();
      int count = 0;
      for (StackTraceElement element : elements) {
        count++;
        if (count > level)
          System.out.println(element.toString());
      }
      System.out.println("--------------------------------CallStack--------------------------------->");
    }
  }

  private static void evalSikulixVersion() {
    // from from jar/Settings/sikulixversion.txt
    Properties prop = new Properties();
    String versionFile = "Settings/sikulixversion.txt";
//    sikulixvmaj=2
//    sikulixvmin=0
//    sikulixvsub=0
//    sikulixbuild=2019-10-17_09:58
//    sikulixbuildnumber=${env.TRAVIS_BUILD_NUMBER}
//    sikulixvused=2.0.0
//    sikulixvproject=2.0.0
//    sikulixvjython=2.7.1
//    sikulixvjruby=9.2.0.0
    try {
      InputStream is;
      is = RunTime.class.getClassLoader().getResourceAsStream(versionFile);
      if (is == null) {
        String msg = String.format("fatal: " + "initSikulixVersion: not found on classpath: %s", versionFile);
        Debug.error(msg);
        throw new SikuliXception(msg);
      }
      prop.load(is);
      is.close();
//    sikulixvproject=2.0.0  or 2.1.0-SNAPSHOT
      SXVersion = prop.getProperty("sikulixvproject");
      String[] version = SXVersion.replace("-SNAPSHOT", "").split("\\.");
      if (version.length != 3) {
        throw new SikuliXception(String.format("Settings: wrong version format: %s", SXVersion));
      }
      SikuliVersionMajor = Integer.decode(version[0]);
      SikuliVersionMinor = Integer.decode(version[1]);
      SikuliVersionSub = Integer.decode(version[2]);
//    sikulixbuild=2019-10-17_09:58
      SXBuild = prop.getProperty("sikulixbuild");
//    sikulixbuildnumber= BE-AWARE: only real in deployed artefacts (TravisCI)
//    in development context undefined:
      SXBuildNumber = prop.getProperty("sikulixbuildnumber");
      if (SXBuildNumber.contains("TRAVIS_BUILD_NUMBER")) {
        SXBuildNumber = "";
      }
      SXBuildStamp = SXBuild.replace("_", "").replace("-", "").replace(":", "").substring(0, 12);
    } catch (Exception e) {
      String msg = String.format("Settings: load version file %s did not work: %s", versionFile, e.getMessage());
      Debug.error(msg);
      throw new SikuliXception(msg);
    }
    SXVersionIDE = "SikulixIDE-" + SXVersion;
    SXVersionAPI = "SikulixAPI " + SXVersion;
    if (SXBuildNumber.isEmpty()) {
      SXVersionLong = SXVersion + String.format("-%s", SXBuild);
    } else {
      SXVersionLong = SXVersion + String.format("-#%s-%s", SXBuildNumber, SXBuild);
    }
    SXVersionShort = SXVersion.replace("-SNAPSHOT", "");
  }

  private static void evalJavaVersion() {
    String vJava = sysPropJSpecVersion;
    String vVM = sysPropJVmVersion;
    String vClass = sysPropJClassVersion;
    String vSysArch = sysPropSunArchDataModel;
    if (vSysArch != null) {
      if (vSysArch.contains("64")) {
        javaArch = 64;
      } else {
        vSysArch = null;
      }
    }

    try {
      if (vJava.startsWith("1.")) {
        javaVersion = Integer.parseInt(vJava.substring(2, 3));
      } else {
        String[] parts = vJava.split("\\.");
        javaVersion = Integer.parseInt(parts[0]);
      }
      javaShow = String.format("java %d version %s vm %s class %s arch %s",
          javaVersion, vJava, vVM, vClass, vSysArch);
    } catch (Exception ex) {
    }

    if (javaVersion < 8) {
      throw new SikuliXception(String.format("fatal: " + "Java version must at least be 8 (%s)", javaShow));
    }

    if (null == vSysArch) {
      throw new SikuliXception(String.format("fatal: " + "Java arch must be 64 Bit (%s)", javaShow));
    }
    SXJavaVersion = "Java" + javaVersion + "(" + javaArch + ")" + sysPropJRTVersion;
  }

  private static void evalSystemVersion() {
    osVersion = sysPropOsVersion;
    String os = sysPropOsName.toLowerCase();
    if (os.startsWith("windows")) {
      sysName = "windows";
      osName = "Windows";
      runningWindows = true;
      NL = "\r\n";
    } else if (os.startsWith("mac")) {
      sysName = "mac";
      osName = "Mac OSX";
      runningMac = true;
    } else if (os.startsWith("linux")) {
      sysName = "linux";
      osName = "Linux";
      runningLinux = true;
    } else {
      // Presume Unix -- pretend to be Linux
      sysName = os;
      osName = sysPropOsName;
      runningLinux = true;
      linuxDistro = sysPropOsName;
    }
    String osNAME = "UnKnown";
    String osname = sysPropOsName.toLowerCase();
    if (osname.startsWith("mac")) {
      osNAME = "Mac";
    } else if (osname.startsWith("windows")) {
      osNAME = "Windows";
    } else if (osname.startsWith("linux")) {
      osNAME = "Linux";
    }
    SXSystemVersion = osNAME + sysPropOsVersion;
  }

  private static void evalSikulixFolder() {
    if (sysPropUserHome == null || sysPropUserHome.isEmpty() || !(fUserHome = new File(sysPropUserHome)).exists()) {
      throw new SikuliXception(String.format("fatal: " + "JavaSystemProperty::user.home not valid"));
    }
    if (sysPropUserDir == null || sysPropUserDir.isEmpty() || !(fWorkDir = new File(sysPropUserDir)).exists()) {
      throw new SikuliXception(String.format("fatal: " + "JavaSystemProperty::user.dir not valid"));
    }
  }

  private static List<String> evalArgsStart(String[] args) {
    List<String> finalArgs = new ArrayList<>();
    for (String arg : args) {
      if ("-v".equals(arg)) {
        setVerbose();
      } else if ("-sandbox".equals(arg)) {
        setSandbox();
      } else if ("-q".equals(arg)) {
        setQuiet();
      } else if ("-r".equals(arg)) {
        shouldRunScript = true;
      } else if ("-s".equals(arg)) {
        asServer = true;
      } else if ("-p".equals(arg)) {
        asPyServer = true;
      }
      finalArgs.add(arg);
    }
    return finalArgs;
  }

  public static int getJVersion() {
    String version = sysPropJSpecVersion;
    int versionInt = -1;
    if (version.contains(".")) {
      version = version.split("\\.")[1];
    }
    try {
      versionInt = Integer.parseInt(version);
    } catch (NumberFormatException e) {
      System.out.println(String.format("[ERROR] RunTime: System property java.spec.version not valid: %s", sysPropJSpecVersion));
      System.exit(-1);
    }
    return versionInt;
  }

  public static boolean onWindows() {
    return sysPropOsName.toLowerCase().startsWith("w");
  }

  public static boolean onMac() {
    return sysPropOsName.toLowerCase().startsWith("m");
  }

  public static File getAppDataFolder() {
    if (null != sxAppDataFolder) {
      return sxAppDataFolder;
    }
    File fUserDir;
    File sandbox = null;
    if (sxSandbox != null) {
      if (sxSandbox.isEmpty()) {
        sandbox = sxRunningJar.getParentFile();
      } else {
        try {
          sandbox = new File(sxSandbox).getCanonicalFile();
          if (!sandbox.exists()) {
            sandbox = null;
          }
        } catch (IOException e) {
        }
      }
    }
    if (sandbox != null && sandbox.exists()) {
      //startLog(3, "Running Sandbox in: %s", sandbox);
      sxAppDataFolder = new File(sandbox, "Sikulix");
    } else {
      sxSandbox = null;
      String userHome = sysPropUserHome;
      if (userHome == null || userHome.isEmpty() || !(fUserDir = new File(userHome)).exists()) {
        System.out.println(String.format("[ERROR] RunTime: System property user.home not valid: %s", userHome));
        System.exit(-1);
      } else {
        if ("w".equals(osNameShort)) {
          String appPath = System.getenv("APPDATA");
          if (appPath != null && !appPath.isEmpty()) {
            sxAppDataFolder = new File(new File(appPath), "Sikulix");
          }
        } else if ("m".equals(osNameShort)) {
          sxAppDataFolder = new File(new File(fUserDir, "Library/Application Support"), "Sikulix");
        } else {
          sxAppDataFolder = new File(fUserDir, ".Sikulix");
        }
      }
    }
    if (!sxAppDataFolder.exists()) {
      sxAppDataFolder.mkdirs();
    }
    if (!sxAppDataFolder.exists()) {
      System.out.println(String.format("[ERROR] RunTime: SikuliX AppData folder not exists: %s", sxAppDataFolder));
      System.exit(-1);
    }
    return sxAppDataFolder;
  }

  public static String getAppDataPath() {
    return getAppDataFolder().getAbsolutePath();
  }

  private static File getRunningJar(String type) {
    if (sxRunningJar != null) {
      return sxRunningJar;
    }
    String jarName = "notKnown";
    CodeSource codeSrc = RunTime.class.getProtectionDomain().getCodeSource();
    if ("IDE".equals(type)) {
      try {
        Class cIDE = Class.forName("org.sikuli.ide.SikulixIDE");
        codeSrc = cIDE.getProtectionDomain().getCodeSource();
      } catch (ClassNotFoundException e) {
        dumpClassPath();
        terminate(999, String.format("RunTime::getRunningJar: no valid Java context (%s)", cIDE));
      }
    }
    if (codeSrc != null && codeSrc.getLocation() != null) {
      try {
        jarName = codeSrc.getLocation().getPath();
        jarName = URLDecoder.decode(jarName, "utf8");
      } catch (UnsupportedEncodingException e) {
        dumpClassPath();
        terminate(999, String.format("RunTime::getRunningJar: URLDecoder: not possible: %s", jarName));
      }
      sxRunningJar = new File(jarName);
    }
    return sxRunningJar;
  }

  public static File getRunningJar() {
    return getRunningJar(runType);
  }

  static {
    logCallStack(1, "RunTime <init>");
    evalSystemVersion();
    evalJavaVersion();
    evalSikulixVersion();
    evalSikulixFolder();
  }

  private static void init(String type) {
    log(3, "global init: entering");
    runType = type;

    //<editor-fold desc="01 add shutdown hook">
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        runShutdownHook();
      }
    });
    //</editor-fold>

    //<editor-fold desc="02 APPDATA folder, TempFolder">
    File appDataFolder = getAppDataFolder();
    if (!isSandbox()) {
      String tmpdir = sysPropJIoTmpdir;
      if (tmpdir != null && !tmpdir.isEmpty()) {
        fTempPath = new File(tmpdir);
      } else {
        throw new SikuliXception("init: java.io.tmpdir not valid (null or empty");
      }
    } else {
      File fSandbox = getAppDataFolder().getParentFile();
      fTempPath = new File(fSandbox, "SikulixTemp");
      fTempPath.mkdir();
      if (fTempPath.exists()) {
        startLog(3, "Sandbox Temp: %s", fTempPath);
      } else {
        throw new SikuliXception(String.format("init: Sandbox Temp not possible: %s", fTempPath));
      }
    }
    fBaseTempPath = new File(fTempPath, String.format("Sikulix_%d", FileManager.getRandomInt()));
    fpBaseTempPath = fBaseTempPath.getAbsolutePath();
    fBaseTempPath.mkdirs();
    log(3, " trying temp folder: %s", fpBaseTempPath);
    try {
      File tempTest = new File(fBaseTempPath, "tempTest.txt");
      FileManager.writeStringToFile("temp test", tempTest);
      boolean success = true;
      if (tempTest.exists()) {
        tempTest.delete();
        if (tempTest.exists()) {
          success = false;
        }
      } else {
        success = false;
      }
      if (!success) {
        throw new SikuliXception("init: temp folder not useable");
      }
    } catch (Exception e) {
      throw new SikuliXception("init: temp folder not writable");
    }

    for (String aFile : fTempPath.list()) {
      if ((aFile.startsWith("Sikulix") && (new File(aFile).isFile()))
              || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
        FileManager.deleteFileOrFolder(new File(fTempPath, aFile));
      }
    }

    if (!appDataFolder.exists()) {
      appDataFolder.mkdirs();
    }
    if (!appDataFolder.exists()) {
      terminate(999, String.format(appDataMsg, appDataFolder));
    }
    fSikulixExtensions = new File(appDataFolder, "Extensions");
    if (!fSikulixExtensions.exists()) {
      fSikulixExtensions.mkdir();
    }
    fSikulixLib = new File(appDataFolder, "Lib");

    fLibsFolder = new File(getAppDataFolder(), "SikulixLibs");
    fpJarLibs += sysName + "/libs" + javaArch;

    fSikulixStore = new File(appDataFolder, "SikulixStore");
    fSikulixStore.mkdir();
    //</editor-fold>

    //<editor-fold desc="03 IDE/API init">
    if ("IDE".equals(type)) {
      if (!runningScripts() && !isAllowMultiple()) {
        isRunning = new File(fTempPath, isRunningFilename);
        boolean shouldTerminate = false;
        try {
          isRunning.createNewFile();
          isRunningFile = new FileOutputStream(isRunning);
          if (null == isRunningFile.getChannel().tryLock()) {
            Class<?> classIDE = Class.forName("org.sikuli.ide.SikulixIDE");
            Method stopSplash = classIDE.getMethod("stopSplash", new Class[0]);
            stopSplash.invoke(null, new Object[0]);
            Sikulix.popError("Terminating: IDE already running");
            shouldTerminate = true;
          }
        } catch (Exception ex) {
          Sikulix.popError("Terminating on FatalError: cannot access IDE lock for/n" + isRunning);
          shouldTerminate = true;
        }
        if (shouldTerminate) {
          System.exit(1);
        }
      }
      initIDEbefore();
      initAPI();
      initIDEafter();
    } else {
      initAPI();
    }
    //</editor-fold>

    if (Debug.getDebugLevel() == minLvl) {
      show();
    }
    log(3, "global init: leaving");
  }

  public static boolean isIDE() {
    return runningAsIDE;
  }

  static boolean isObsolete(long refTime) {
    if (refTime == 0) {
      return false;
    }
    return refTime < obsolete;
  }

  public static boolean isDevelop() {
    return System.getProperty("sikuli.Develop") != null;
  }

  public boolean isTesting() {
    return optTesting;
  }

  public static void setSandbox() {
    sxSandbox = "";
  }

  public static boolean isSandbox() {
    return sxSandbox != null;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="01 global init">
  private static RunTime runTime = null;

  public static synchronized RunTime get() {
    if (runTime == null) {
      return get("API");
    }
    return runTime;
  }

  public static synchronized RunTime get(String type) {
    if (runTime != null) {
      return runTime;
    }
    runTime = new RunTime();

    //<editor-fold desc="04 options">
    float forever = Settings.FOREVER; // to force Settings initialization
    sxOptions = Options.init(runTime);
    optTesting = sxOptions.isOption("testing", false);
    if (optTesting) {
      Debug.info("Options: testing = on");
    }

    int optDebugLevel = optTesting ? Debug.getDebugLevel() : sxOptions.getOptionInteger("Debug.level", -1);
    if (optDebugLevel > Debug.getDebugLevel()) {
      Debug.info("Options: Debug.level = %d", optDebugLevel);
      Debug.on(optDebugLevel);
    }
    runTime.initSikulixOptions();
    //</editor-fold>

    return runTime;
  }

  private static RobotDesktop cleanupRobot = null;

  private static void initAPI() {
    log(4, "initAPI: entering");
    try {
      cleanupRobot = new RobotDesktop();
    } catch (AWTException e) {
    }
    log(4, "initAPI: leaving");
  }

  public static boolean isRunningIDE = false;

  private static void initIDEbefore() {
    log(4, "initIDEbefore: entering");
    isRunningIDE = true;
    log(4, "initIDEbefore: leaving");
  }

  private static void initIDEafter() {
    log(4, "initIDEafter: entering");
    try {
      cIDE = Class.forName("org.sikuli.ide.SikulixIDE");
      mHide = cIDE.getMethod("hideIDE", new Class[0]);
      mShow = cIDE.getMethod("showIDE", new Class[0]);
    } catch (Exception ex) {
      log(-1, "SikulixIDE: reflection: %s", ex.getMessage());
    }
    log(4, "initIDEafter: leaving");
  }

  static Class<?> cIDE = null;
  static Method mHide = null;
  static Method mShow = null;

  public void hideIDE() {
    if (null != cIDE) {
      try {
        mHide.invoke(null, new Object[0]);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }

  public void showIDE() {
    if (null != cIDE) {
      try {
        mShow.invoke(null, new Object[0]);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="02 startup">
  public static void startIDE(String[] args) {
    start("IDE", args);
  }

  public static void startAPI(String[] args) {
    start("API", args);
  }

  public static void start(String type, String[] args) {

    Debug.init();

    if ("API".equals(type)) {
      runningAsIDE = false;
      if (args.length == 1 && "buildDate".equals(args[0])) {
        System.out.println(SXBuild);
        System.exit(0);
      }

      if (args.length == 1 && "createlibs".equals(args[0])) {
        Debug.off();
        CodeSource codeSource = Sikulix.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && codeSource.getLocation().toString().endsWith("classes/")) {
          File libsSource = new File(new File(codeSource.getLocation().getFile()).getParentFile().getParentFile(), "src/main/resources");
          for (String sys : new String[]{"mac", "windows", "linux"}) {
            Sikulix.print("******* %s", sys);
            String sxcontentFolder = String.format("sikulixlibs/%s/libs64", sys);
            List<String> sikulixlibs = RunTime.get().getResourceList(sxcontentFolder);
            String sxcontent = "";
            for (String lib : sikulixlibs) {
              if (lib.equals("sikulixcontent")) {
                continue;
              }
              sxcontent += lib + "\n";
            }
            Sikulix.print("%s", sxcontent);
            FileManager.writeStringToFile(sxcontent, new File(libsSource, sxcontentFolder + "/sikulixcontent"));
          }
        }
        System.exit(0);
      }
    }

    List<String> finalArgs = evalArgsStart(args);

    File runningJar = getRunningJar(type);
    String jarName = runningJar.getName();
    RunTime.startLog(1, "Running: %s", runningJar);

    RunTime.startLog(1, "AppData%s: %s",
        (sxSandbox != null ? " (Sandbox)" : ""),
        getAppDataPath());

    if (jarName.endsWith(".jar")) {
      String classPath = "";
      classPath = ExtensionManager.makeClassPath(runningJar);
      List<String> cmd = new ArrayList<>();
      if (onWindows()) {
        cmd.add(sysPropJavaHome + "\\bin\\java.exe");
      } else {
        cmd.add(sysPropJavaHome + "/bin/java");
      }
      if (getJVersion() > 8) {
/*
Suppress Java 9+ warnings
--add-opens
java.desktop/javax.swing.plaf.basic=ALL-UNNAMED
--add-opens
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens
java.base/java.io=ALL-UNNAMED
-------------------- only needed for monitor eval
--add-opens
java.desktop/sun.awt=ALL-UNNAMED
-------------------------
-Dnashorn.args=--no-deprecation-warning
*/
        cmd.add("--add-opens");
        cmd.add("java.desktop/javax.swing.plaf.basic=ALL-UNNAMED");
        cmd.add("--add-opens");
        cmd.add("java.base/sun.nio.ch=ALL-UNNAMED");
        cmd.add("--add-opens");
        cmd.add("java.base/java.io=ALL-UNNAMED");
        cmd.add("-Dnashorn.args=--no-deprecation-warning");
      }
      cmd.add("-Dfile.encoding=UTF-8");

      for (Object key : sysProps.keySet()) {
        if (key.toString().startsWith("sikuli")) {
          String dSetting = "-D" + key.toString();
          Object value = sysProps.get(key);
          if (!value.toString().isEmpty()) {
            dSetting += "=" + value.toString();
          }
          cmd.add(dSetting);
        }
      }
      if (runningAsIDE) {
        cmd.add("-Dsikuli.IDE_should_run");
      } else {
        cmd.add("-Dsikuli.API_should_run");
      }
      if (!classPath.isEmpty()) {
        cmd.add("-cp");
        cmd.add(classPath);
      }
      if (runningAsIDE) {
        cmd.add("org.sikuli.ide.SikulixIDE");
      } else {
        cmd.add("org.sikuli.script.support.SikulixAPI");
      }
      cmd.addAll(finalArgs);

      RunTime.startLog(3, "*********************** leaving start");
      if (shouldDetach()) {
        ProcessRunner.detach(cmd);
        System.exit(0);
      } else {
        int exitCode = ProcessRunner.runBlocking(cmd);
        System.exit(exitCode);
      }
    }
  }

  public static void afterStart(String type, String[] args) {
    String startType = "IDE";
    if ("IDE".equals(type)) {
      if (null == System.getProperty("sikuli.IDE_should_run")) {
        System.out.println("[ERROR] org.sikuli.ide.SikulixIDE: unauthorized use. Use: org.sikuli.ide.Sikulix");
        System.exit(1);
      }
    } else {
      if (null == System.getProperty("sikuli.API_should_run")) {
        System.out.println("[ERROR] org.sikuli.script.SikulixAPI: unauthorized use. Use: org.sikuli.script.Sikulix");
        System.exit(1);
      }
      startType = "API";
    }

    getRunningJar(type);
    evalArgsStart(args);
    init(type);
    Debug.log(3, "Sikulix: starting " + startType);
    if (isSandbox()) {
      String javaTemp = new File(getAppDataFolder().getParent(), "SikulixJavaTemp").getAbsolutePath();
      System.setProperty("java.io.tmpdir", javaTemp);
    }
    evalArgs(args);
    ExtensionManager.readExtensions(true);

    if (isQuiet()) {
      Debug.quietOn();
    } else if (isVerbose()) {
      Debug.setWithTimeElapsed(RunTime.getElapsedStart());
      Debug.setGlobalDebug(3);
      Debug.globalTraceOn();
      Debug.setStartWithTrace();
    }

    if (!getLogFile().isEmpty()) {
      Debug.setLogFile(getLogFile());
    }

    if (!getUserLogFile().isEmpty()) {
      Debug.setUserLogFile(getUserLogFile());
    }

    if (runningScripts()) {
      HotkeyManager.getInstance().addHotkey("Abort", new HotkeyListener() {
        @Override
        public void hotkeyPressed(HotkeyEvent e) {
          if (RunTime.get().runningScripts()) {
            Runner.abortAll();
            terminate(254, "AbortKey was pressed: aborting all running scripts");
          }
        }
      });
      int exitCode = Runner.runScripts(RunTime.getRunScripts(), userArgs, new IScriptRunner.Options());
      if (exitCode > 255) {
        exitCode = 254;
      }
      terminate(exitCode, "");
    }

    if (shouldRunPythonServer()) {
      get().installStopHotkeyPythonServer();
      if (Debug.getDebugLevel() == 3) {
      }
      startPythonServer();
    }

    if (shouldRunServer()) {
      SikulixServer.run();
      terminate();
    }
  }

  public static void evalArgs(String[] args) {

    CommandLine cmdLine;
    String cmdValue;

    CommandArgs cmdArgs = new CommandArgs();
    cmdLine = cmdArgs.getCommandLine(args);

    boolean cmdLineValid = true;
    if (cmdLine == null) {
      startLog(-1, "Did not find any valid option on command line!");
      cmdLineValid = false;
    } else {
      setArgs(cmdArgs.getUserArgs(), cmdArgs.getSXArgs());
    }

    if (cmdLineValid && cmdLine.hasOption("h")) {
      cmdArgs.printHelp();
      System.exit(0);
    }

    if (cmdLineValid && cmdLine.hasOption(CommandArgsEnum.DEBUG.shortname())) {
      cmdValue = cmdLine.getOptionValue(CommandArgsEnum.DEBUG.longname());
      if (cmdValue != null) {
        Debug.setDebugLevel(cmdValue);
      }
    }

    if (cmdLineValid && cmdLine.hasOption("g")) {
      if (cmdLine.hasOption("s")) {
        serverGroups = cmdLine.getOptionValue("g");
        startLog(3, "groups (-g): %s", serverGroups);
      } else {
        startLog(-1, "groups (-g): currently only accepted with -s");
      }
    }

    if (cmdLineValid && cmdLine.hasOption("x")) {
      if (cmdLine.hasOption("s")) {
        serverExtra = cmdLine.getOptionValue("x");
        startLog(3, "extra (-x): %s", serverExtra);
      } else {
        startLog(-1, "extra (-x): currently only accepted with -s");
      }
    }

    if (cmdLineValid && cmdLine.hasOption("s")) {
      serverOptions = cmdLine.getOptionValues("s");
    }

    if (cmdLineValid && cmdLine.hasOption("m")) {
      setAllowMultiple();
    }

    if (cmdLineValid && cmdLine.hasOption(CommandArgsEnum.LOGFILE.shortname())) {
      logFile = cmdLine.getOptionValue(CommandArgsEnum.LOGFILE.longname());
    }

    if (cmdLineValid && cmdLine.hasOption(CommandArgsEnum.USERLOGFILE.shortname())) {
      userLogFile = cmdLine.getOptionValue(CommandArgsEnum.USERLOGFILE.longname());
    }

    if (cmdLineValid && cmdLine.hasOption("c")) {
      System.setProperty("sikuli.console", "false");
    }

    if (cmdLineValid && cmdLine.hasOption(CommandArgsEnum.LOAD.shortname())) {
      loadScripts = cmdLine.getOptionValues(CommandArgsEnum.LOAD.longname());
    }

    if (cmdLineValid && cmdLine.hasOption(CommandArgsEnum.RUN.shortname())) {
      runScripts = resolveRelativeFiles(cmdLine.getOptionValues(CommandArgsEnum.RUN.longname()));
    }
  }

  private static String[] userArgs = new String[0];
  private static String[] sxArgs = new String[0];

  private static void setArgs(String[] args, String[] sargs) {
    userArgs = args;
    sxArgs = sargs;
  }

  public static String[] getSXArgs() {
    return sxArgs;
  }

  public static void setUserArgs(String[] args) {
    userArgs = new String[args.length];
    int n = 0;
    for (String arg : args) {
      userArgs[n] = arg;
      n++;
    }
  }

  public static String[] getUserArgs() {
    return userArgs;
  }

  public static void printArgs() {
    String[] xargs = getSXArgs();
    if (xargs.length > 0) {
      startLog(1, "--- Sikuli parameters ---");
      for (int i = 0; i < xargs.length; i++) {
        startLog(1, "%d: %s", i + 1, xargs[i]);
      }
    }
    xargs = getUserArgs();
    if (xargs.length > 0) {
      startLog(1, "--- User parameters ---");
      for (int i = 0; i < xargs.length; i++) {
        startLog(1, "%d: %s", i + 1, xargs[i]);
      }
    }
  }

  public void installStopHotkeyPythonServer() {
    HotkeyManager.getInstance().addHotkey("Abort", new HotkeyListener() {
      @Override
      public void hotkeyPressed(HotkeyEvent e) {
        Debug.log(3, "Stop HotKey was pressed");
        if (RunTime.get().shouldRunPythonServer()) {
          stopPythonServer();
          terminate();
        }
      }
    });
  }

  public static void startPythonServer() {
    if (!isRunningPyServer()) {
      try {
        Class.forName("py4j.GatewayServer");
        pythonServer = new py4j.GatewayServer();
      } catch (ClassNotFoundException e) {
        Debug.error("Python server: py4j not on classpath");
        terminate();
      }
      pythonServer.start(false);
    }
  }

  public static void stopPythonServer() {
    if (isRunningPyServer()) {
      Debug.logp("Python server: trying to stop");
      pythonServer.shutdown();
      pythonServer = null;
    }
  }

  public static boolean isRunningPyServer() {
    return null != pythonServer;
  }

  private static py4j.GatewayServer pythonServer = null;

  public static File asFolder(String option) {
    if (null == option) {
      return null;
    }
    File folder = new File(option);
    if (!folder.isAbsolute()) {
      folder = new File(get().fWorkDir, option);
    }
    if (folder.isDirectory() && folder.exists()) {
      return folder;
    }
    return null;
  }

  public static File asFile(String option) {
    if (null == option) {
      return null;
    }
    if (null == asFolder(option)) {
      File file = new File(option);
      if (!file.isAbsolute()) {
        file = new File(get().fWorkDir, option);
      }
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  public static String[] resolveRelativeFiles(String[] givenScripts) {
    String[] runScripts = new String[givenScripts.length];
    String baseDir = get().fWorkDir.getPath();
    for (int i = 0; i < runScripts.length; i++) {
      String givenScript = givenScripts[i];
      String file = resolveRelativeFile(givenScript, baseDir);
      if (file == null) {
        file = resolveRelativeFile(givenScript + ".sikuli", baseDir);
        if (file == null) {
          runScripts[i] = "?" + givenScript;
          continue;
        }
      } else {
        if (i == 0 && file.endsWith(".sikuli")) {
          baseDir = new File(file).getParent();
        }
      }
      EffectiveRunner runnerAndFile = Runner.getEffectiveRunner(file);
      IScriptRunner runner = runnerAndFile.getRunner();
      String fileToRun = runnerAndFile.getScript();
      File possibleDir = null;
      if (null == fileToRun) {
        for (String ending : new String[]{"", ".sikuli"}) {
          possibleDir = new File(file + ending);
          if (possibleDir.exists()) {
            break;
          } else {
            possibleDir = null;
          }
        }
        if (null == possibleDir) {
          runScripts[i] = "?" + givenScript;
          continue;
        }
        baseDir = possibleDir.getAbsolutePath();
        runnerAndFile = Runner.getEffectiveRunner(baseDir);
        fileToRun = runnerAndFile.getScript();
        if (fileToRun == null) {
          fileToRun = "!" + baseDir;
        } else {
          fileToRun = baseDir;
        }
      }
      runScripts[i] = fileToRun;
    }
    return runScripts;
  }

  /**
   * a relative path is checked for existence in the current base folder,
   * working folder and user home folder in this sequence.
   *
   * @param scriptName
   * @return absolute file or null if not found
   */
  public static String resolveRelativeFile(String scriptName, String baseDir) {
    if (get().runningWindows && (scriptName.startsWith("\\") || scriptName.startsWith("/"))) {
      scriptName = new File(scriptName).getAbsolutePath();
      return scriptName;
    }
    File file = new File(scriptName);
    if (!file.isAbsolute()) {
      File inBaseDir = new File(baseDir, scriptName);
      if (inBaseDir.exists()) {
        file = inBaseDir;
      } else {
        File inWorkDir = new File(get().fWorkDir, scriptName);
        if (inWorkDir.exists()) {
          file = inWorkDir;
        } else {
          File inUserDir = new File(get().fUserHome, scriptName);
          if (inUserDir.exists()) {
            file = inUserDir;
          } else {
            return null;
          }
        }
      }
    }
    return file.getAbsolutePath();
  }

  public static long getElapsedStart() {
    return elapsedStart;
  }

  private static long elapsedStart = new Date().getTime();

  public static String getLogFile() {
    return logFile;
  }

  private static String logFile = "";

  public static String getUserLogFile() {
    return userLogFile;
  }

  private static String userLogFile = "";

  public static String[] getLoadScripts() {
    return loadScripts;
  }

  private static String[] loadScripts = new String[0];

  public static String[] getRunScripts() {
    return runScripts;
  }

  private static boolean shouldRunScript = false;
  private static String[] runScripts = new String[0];

  public static boolean runningScripts() {
    return shouldRunScript;
  }

  public static boolean shouldRunServer() {
    return asServer;
  }

  private static boolean asServer = false;

  public static String[] getServerOptions() {
    return serverOptions;
  }

  private static String[] serverOptions = null;

  public static String getServerGroups() {
    return serverGroups;
  }

  private static String serverGroups = null;

  public static String getServerExtra() {
    return serverExtra;
  }

  private static String serverExtra = null;

  public static boolean shouldRunPythonServer() {
    return asPyServer;
  }

  private static boolean asPyServer = false;

  public static void setAllowMultiple() {
    allowMultiple = true;
  }

  public static boolean isAllowMultiple() {
    return allowMultiple;
  }

  private static boolean allowMultiple = false;

  public static boolean shouldDetach() {
    return !runningScripts() && !shouldRunServer() && !shouldRunPythonServer();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="05 logging">
  private static int lvl = 3;
  private static int minLvl = lvl;

  public static boolean isVerbose() {
    return verbose;
  }

  public static void setVerbose() {
    RunTime.verbose = true;
    Debug.setDebugLevel(3);
    Debug.setWithTimeElapsed(RunTime.getElapsedStart());
    Debug.setGlobalDebug(3);
    Debug.globalTraceOn();
    Debug.setStartWithTrace();
  }

  private static boolean verbose = false;

  public static boolean isQuiet() {
    return quiet;
  }

  public static void setQuiet() {
    RunTime.quiet = true;
  }

  private static boolean quiet = false;

  public static void startLog(int level, String msg, Object... args) {
    String typ = runningAsIDE ? "IDE" : "API";
    String msgShow = String.format("startUp: %s: ", typ);
    if (!isVerbose()) {
      return;
    }
    if (level < 0) {
      msgShow = "[ERROR]" + msgShow + msg;
      System.out.println(String.format(msgShow, args));
      return;
    }
    if (isQuiet()) {
      return;
    }
    if (isVerbose()) {
      if (level > 0) {
        msgShow = "[DEBUG]" + msgShow + msg;
      } else {
        msgShow = "[INFO]" + msgShow + msg;
      }
      System.out.println(String.format(msgShow, args));
    }
  }

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "RunTime:" + message, args);
  }

  private static void logp(String message, Object... args) {
    Debug.logx(-3, message, args);
  }

  private static void logp(int level, String message, Object... args) {
    if (level <= Debug.getDebugLevel()) {
      logp(message, args);
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="09 Sikulix options handling">
  void initSikulixOptions() {
  }

  public Options options() {
    return sxOptions;
  }
  private static Options sxOptions = null;
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="11 libs handling">
  boolean areLibsExported = false;
  private Map<String, Boolean> libsLoaded = new HashMap<String, Boolean>();

  private void addToWindowsSystemPath(File fLibsFolder) {
    for (File bridjFile : runTime.fTempPath.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name.contains("BridJExtractedLibraries")) {
          return true;
        }
        return false;
      }
    })) {
      runTime.log(4, "cleanTemp: " + bridjFile.getName());
      FileManager.deleteFileOrFolder(bridjFile);
    }
    //TODO String syspath = SysJNA.WinKernel32.getEnvironmentVariable("PATH");
    String syspath = WinUtil.getEnv("PATH");
    if (syspath == null) {
      throw new SikuliXception("addToWindowsSystemPath: cannot access system path");
    } else {
      String libsPath = fLibsFolder.getAbsolutePath();
      if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
        // TODO if (SysJNA.WinKernel32.setEnvironmentVariable("PATH", libsPath + ";" + syspath)) {
        if (null != (syspath = WinUtil.setEnv("PATH", libsPath + ";" + syspath))) {
          if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
            log(-1, "addToWindowsSystemPath: adding to system path did not work:\n%s", syspath);
            throw new SikuliXception("addToWindowsSystemPath: did not work - see error");
          }
        }
        log(lvl, "addToWindowsSystemPath: added to systempath:\n%s", libsPath);
      }
    }
  }

  private boolean checkJavaUsrPath(File fLibsFolder) {
    //TODO Java 9: Windows: Java Classloader::usr_paths needed for libs access?
    if (isJava9()) {
      return true;
    }
    String fpLibsFolder = fLibsFolder.getAbsolutePath();
    Field usrPathsField = null;
    boolean contained = false;
    try {
      usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    } catch (NoSuchFieldException ex) {
      log(-1, "checkJavaUsrPath: get\n%s", ex);
    } catch (SecurityException ex) {
      log(-1, "checkJavaUsrPath: get\n%s", ex);
    }
    if (usrPathsField != null) {
      usrPathsField.setAccessible(true);
      try {
        //get array of paths
        String[] javapaths = (String[]) usrPathsField.get(null);
        //check if the path to add is already present
        for (String p : javapaths) {
          if (new File(p).equals(fLibsFolder)) {
            contained = true;
            break;
          }
        }
        //add the new path
        if (!contained) {
          final String[] newPaths = Arrays.copyOf(javapaths, javapaths.length + 1);
          newPaths[newPaths.length - 1] = fpLibsFolder;
          usrPathsField.set(null, newPaths);
          log(lvl, "checkJavaUsrPath: added to ClassLoader.usrPaths");
          contained = true;
        }
      } catch (IllegalAccessException ex) {
        log(-1, "checkJavaUsrPath: set\n%s", ex);
      } catch (IllegalArgumentException ex) {
        log(-1, "checkJavaUsrPath: set\n%s", ex);
      }
      return contained;
    }
    return false;
  }

  /**
   * INTERNAL USE: load a native library from the libs folder
   *
   * @param libname name of library without prefix/suffix/ending
   */
  public static boolean loadLibrary(String libname) {
    if (isTerminating) {
      return false;
    }
    return RunTime.get().libsLoad(libname);
  }

  private boolean libsLoad(String libName) {
    log(lvl, "loadlib: trying %s", libName);
    String msg = "loadLib: %s";
    if (!areLibsExported) {
      libsExport();
    }
    if (!areLibsExported) {
      throw new SikuliXception("loadLib: deferred exporting of libs did not work");
    }
    File fLibsFolderUsed = fLibsFolder;
    if (runningWindows) {
      libName += ".dll";
    } else if (runningMac) {
      libName = "lib" + libName + ".dylib";
    } else if (runningLinux) {
      libName = "lib" + libName + ".so";
    }
    File fLib = new File(fLibsFolder, libName);
    int level = lvl;
    if (!runningLinux) {
      Boolean vLib = libsLoaded.get(libName);
      if (vLib == null || !fLib.exists()) {
        if (!fLib.exists()) {
          throw new SikuliXception(String.format("loadlib: %s not in any libs folder", libName));
        } else {
          vLib = false;
        }
      }
      if (vLib) {
        level++;
        msg += " already loaded";
        log(level, msg, libName);
        return true;
      }
    }
    try {
      if (runningLinux && libName.startsWith("libopen")) {
        libName = "opencv_java";
        System.loadLibrary(libName);
      } else {
        System.load(fLib.getAbsolutePath());
      }
    } catch (Exception e) {
      log(-1, "not usable: %s", e.getMessage());
      terminate(999, "problem with native library: " + libName);
    } catch (UnsatisfiedLinkError e) {
      log(-1, msg + " (failed) probably dependent libs missing:\n%s", libName, e.getMessage());
      String helpURL = "https://github.com/RaiMan/SikuliX1/wiki/macOS-Linux:-Support-Libraries-for-OpenCV-4";
      if (RunTime.isIDE()) {
        Debug.error("Save your work, correct the problem and restart the IDE!");
        try {
          Desktop.getDesktop().browse(new URI(helpURL));
        } catch (IOException ex) {
        } catch (URISyntaxException ex) {
        }
      }
      Debug.error("see: " + helpURL);
      terminate(999, "problem with native library: " + libName);
    }
    libsLoaded.put(libName, true);
    log(level, msg + " (success)", libName);
    return true;
  }

  private boolean didExport = false;

  public boolean shouldExport() {
    return didExport;
  }

  private void libsExport() {
/*
    remove obsolete libs folders in Temp
*/
    String[] fpList = fTempPath.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name.contains("SikulixLibs")) {
          return true;
        }
        return false;
      }
    });
    if (fpList.length > 0) {
      log(lvl, "libsExport: deleting obsolete libs folders in Temp");
      for (String entry : fpList) {
        if (entry.endsWith(SXBuildStamp)) {
          continue;
        }
        FileManager.deleteFileOrFolder(new File(fTempPath, entry));
      }
    }

/*
    remove libsfolder < 1.1.4
*/
    fpList = getAppDataFolder().list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name.contains("SikulixLibs_")) {
          return true;
        }
        return false;
      }
    });
    if (fpList.length > 0) {
      log(lvl, "libsExport: deleting obsolete libs folders in AppPath");
      for (String entry : fpList) {
        FileManager.deleteFileOrFolder(new File(getAppDataFolder(), entry));
      }
    }

/*
    export
*/
    String libMsg = "folder exists:";
    if (fLibsFolder.exists()) {
      String[] resourceList = fLibsFolder.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if (name.contains("_MadeForSikuliX")) return true;
          return false;
        }
      });
      String libVersion = "";
      String libStamp = "";
      if (resourceList.length > 0) {
        Matcher matcher = Pattern.compile("(.*?)_(.*?)_MadeForSikuliX.*?txt").matcher(resourceList[0]);
        if (matcher.find()) {
          libVersion = matcher.group(1);
          libStamp = matcher.group(2);
        }
      }
      if (libVersion.isEmpty() || !libVersion.equals(getVersionShort()) ||
          libStamp.length() != SXBuildStamp.length() || 0 != libStamp.compareTo(SXBuildStamp)) {
        FileManager.deleteFileOrFolder(fLibsFolder);
        log(lvl, "libsExport: folder has wrong content: %s (%s - %s)", fLibsFolder, libVersion, libStamp);
      }
    }

    if (!fLibsFolder.exists()) {
      fLibsFolder.mkdirs();
      if (!fLibsFolder.exists()) {
        throw new SikuliXception("libsExport: folder not available: " + fLibsFolder.toString());
      }
      String libToken = String.format("%s_%s_MadeForSikuliX64%s.txt",
          getVersionShort(), SXBuildStamp, runningMac ? "M" : (runningWindows ? "W" : "L"));
      FileManager.writeStringToFile("*** Do not delete this file ***\n", new File(fLibsFolder, libToken));
      libMsg = "folder created:";
      List<String> nativesList = getResourceList(fpJarLibs);
      for (String aFile : nativesList) {
        String copyMsg = "exported";
        String inFile = new File(fpJarLibs, aFile).getPath();
        if (runningWindows) {
          inFile = inFile.replace("\\", "/");
        }
        try (FileOutputStream outFile = new FileOutputStream(new File(fLibsFolder, aFile));
             InputStream inStream = runTimeClass.getResourceAsStream(inFile);) {
          copy(inStream, outFile);
          libsLoaded.put(aFile, false);
        } catch (Exception ex) {
          copyMsg = String.format("failed: %s", ex.getMessage());
        }
        copyMsg = String.format("libsExport: %s: %s", aFile, copyMsg);
        if (copyMsg.contains("failed")) {
          FileManager.deleteFileOrFolder(fLibsFolder);
          log(-1, copyMsg);
          break;
        } else {
          log(lvl + 1, copyMsg);
          didExport = true;
        }
      }
    }

    //TODO useLibsProvided
/*
      if (useLibsProvided) {
        log(lvl, "Linux: requested to use provided libs - copying");
        LinuxSupport.copyProvidedLibs(fLibsFolder);
      }
*/

    if (runningWindows) {
      addToWindowsSystemPath(fLibsFolder);
      //TODO: Windows: Java Classloader::usr_paths needed for libs access?
      if (!checkJavaUsrPath(fLibsFolder)) {
        log(-1, "Problems setting up on Windows - see errors - might not work and crash later");
      }
      String lib = "jawt.dll";
      File fJawtDll = new File(fLibsFolder, lib);
      FileManager.deleteFileOrFolder(fJawtDll);
      FileManager.xcopy(new File(sysPropJavaHome, "bin/" + lib), fJawtDll);
      if (!fJawtDll.exists()) {
        throw new SikuliXception("problem copying " + fJawtDll);
      }
    }
    log(lvl, "libsExport: " + libMsg + " %s (%s - %s)", fLibsFolder, getVersionShort(), SXBuildStamp);
    areLibsExported = true;
  }

  private static boolean isLibExported = false;

  public void exportLib() {
    if (isLibExported) {
      return;
    }
    if (!fSikulixLib.exists()
        || !new File(fSikulixLib, "sikuli").exists()) {
      fSikulixLib.mkdir();
      extractResourcesToFolder("Lib", fSikulixLib, null);
    } else {
      extractResourcesToFolder("Lib/sikuli", new File(fSikulixLib, "sikuli"), null);
    }
    // RFW support: module robot should no longer be here (2.1.0)
    File fLibRobot = new File(RunTime.get().fSikulixLib, "robot");
    if (fLibRobot.exists()) {
      FileManager.deleteFileOrFolder(fLibRobot);
    }
    isLibExported = true;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="12 classpath handling">
  private static void storeClassPath() {
    if (isJava9()) {
      String separator = File.pathSeparator;
      String cp = sysPropJClassPath;
      classPathList = Arrays.asList(cp.split(separator));
    } else {
      classPathActual.clear();
      URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      classPathActual = Arrays.asList(sysLoader.getURLs());
      classPathList.clear();
      for (URL urlPath : classPathActual) {
        classPathList.add(urlPath.toExternalForm());
      }
    }
  }

  /**
   * print the current classpath entries to sysout
   */
  public static void dumpClassPath() {
    dumpClassPath(null);
  }

  /**
   * print the current classpath entries to sysout whose path name contain the given string
   *
   * @param filter the fileter string
   */
  public static void dumpClassPath(String filter) {
    filter = filter == null ? "" : filter;
    logp("*** classpath dump %s", filter);
    storeClassPath();
    filter = filter.toUpperCase();
    int n = 0;
    for (String sEntry : classPathList) {
      if (!filter.isEmpty()) {
        if (!sEntry.toUpperCase().contains(filter)) {
          n++;
          continue;
        }
      }
      logp("%3d: %s", n, sEntry);
      n++;
    }
    logp("*** classpath dump end");
  }

  /**
   * check whether a classpath entry contains the given identifying string, stops on first match
   *
   * @param artefact the identifying string
   * @return the absolute path of the entry found - null if not found
   */
  private String isOnClasspath(String artefact, boolean isJar) {
    artefact = FileManager.slashify(artefact, false);
    String cpe = null;
    if (classPathList.isEmpty()) {
      storeClassPath();
    }
    for (String entry : classPathList) {
      String sEntry = FileManager.slashify(new File(entry).getPath(), false);
      if (sEntry.contains(artefact)) {
        if (isJar) {
          if (!sEntry.endsWith(".jar")) {
            continue;
          }
          if (!new File(sEntry).getName().contains(artefact)) {
            continue;
          }
          if (new File(sEntry).getName().contains("4" + artefact)) {
            continue;
          }
        }
        cpe = new File(entry).getPath();
        break;
      }
    }
    return cpe;
  }

  public String isJarOnClasspath(String artefact) {
    return isOnClasspath(artefact, true);
  }

  public String isOnClasspath(String artefact) {
    return isOnClasspath(artefact, false);
  }

  public URL fromClasspath(String artefact) {
    artefact = FileManager.slashify(artefact, false).toUpperCase();
    URL cpe = null;
    String scpe = null;
    if (classPathActual.isEmpty()) {
      storeClassPath();
    }
    for (String entry : classPathList) {
      String sEntry = FileManager.slashify(new File(entry).getPath(), false);
      if (sEntry.toUpperCase().contains(artefact)) {
        scpe = entry;
        break;
      }
    }
    if (null != scpe) {
      try {
        cpe = new URL(scpe);
      } catch (MalformedURLException e) {
      }
    }
    return cpe;
  }

  /**
   * check wether a the given URL is on classpath
   *
   * @param path URL to look for
   * @return true if found else otherwise
   */
  public boolean isOnClasspath(URL path) {
    if (classPathActual.isEmpty()) {
      storeClassPath();
    }
    for (String entry : classPathList) {
    }
    return false;
  }

  List<String> sxClasspath = new ArrayList<>();

  public boolean addToClasspath(String jarOrFolder) {
    return addToClasspath(jarOrFolder, "");
  }

  public boolean addToClasspath(String jarOrFolder, String caller) {
    if (null != isOnClasspath(jarOrFolder)) {
      return true;
    }
    if (isJava9("skipped: addToClasspath() - caller: " + caller)) {
      sxClasspath.add(jarOrFolder);
      return false;
    }
    if (!new File(jarOrFolder).exists()) {
      log(-1, "addToClasspath: does not exist - not added:\n%s", jarOrFolder);
      return false;
    }
    //TODO addToClasspath
//    log(lvl, "addToClasspath:\n%s", uJarOrFolder);
//    Method method;
//    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//    Class sysclass = URLClassLoader.class;
//    try {
//      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
//      method.setAccessible(true);
//      method.invoke(sysLoader, new Object[]{uJarOrFolder});
//    } catch (Exception ex) {
//      log(-1, "Did not work: %s", ex.getMessage());
//      return false;
//    }
//    storeClassPath();
    return false;
  }

  public File asExtension(String fpJar) {
    File fJarFound = new File(FileManager.normalizeAbsolute(fpJar));
    if (!fJarFound.exists()) {
      String fpCPEntry = runTime.isOnClasspath(fJarFound.getName());
      if (fpCPEntry == null) {
        fJarFound = new File(runTime.fSikulixExtensions, fpJar);
        if (!fJarFound.exists()) {
          fJarFound = new File(runTime.fSikulixLib, fpJar);
          if (!fJarFound.exists()) {
            fJarFound = null;
          }
        }
      } else {
        fJarFound = new File(fpCPEntry, fJarFound.getName());
      }
    } else {
      return null;
    }
    return fJarFound;
  }
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="15 handling resources from classpath">

  /**
   * export all resource files from the given subtree on classpath to the given folder retaining the subtree<br>
   * to export a specific file from classpath use extractResourceToFile or extractResourceToString
   *
   * @param fpRessources path of the subtree relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */

  public List<String> extractResourcesToFolder(String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content;
    content = resourceList(fpRessources, filter);
    if (content == null) {
      return null;
    }
    if (fFolder == null) {
      return content;
    }
    return doExtractToFolderWithList(fpRessources, fFolder, content);
  }

  public List<String> doExtractToFolderWithList(String fpRessources, File fFolder, List<String> content) {
    int count = 0;
    int ecount = 0;
    String subFolder = "";
    if (content != null && content.size() > 0) {
      for (String eFile : content) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        if (extractResourceToFile(fpRessources, eFile, fFolder)) {
          log(lvl + 1, "extractResourceToFile done: %s", eFile);
          count++;
        } else {
          ecount++;
        }
      }
    }
    if (ecount > 0) {
      log(lvl, "files exported: %d - skipped: %d from %s to:\n %s", count, ecount, fpRessources, fFolder);
    } else {
      log(lvl, "files exported: %d from: %s to:\n %s", count, fpRessources, fFolder);
    }
    return content;
  }

  /**
   * export all resource files from the given subtree in given jar to the given folder retaining the subtree
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param fpRessources path of the subtree or file relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public List<String> extractResourcesToFolderFromJar(String aJar, String fpRessources, File fFolder, FilenameFilter
      filter) {
    List<String> content = new ArrayList<String>();
    File faJar = new File(aJar);
    URL uaJar = null;
    fpRessources = FileManager.slashify(fpRessources, false);
    if (faJar.isAbsolute()) {
      if (!faJar.exists()) {
        log(-1, "extractResourcesToFolderFromJar: does not exist:\n%s", faJar);
        return null;
      }
      try {
        uaJar = new URL("jar", null, "file:" + aJar);
      } catch (MalformedURLException ex) {
        log(-1, "extractResourcesToFolderFromJar: bad URL for:\n%s", faJar);
        return null;
      }
    } else {
      uaJar = fromClasspath(aJar);
      if (uaJar == null) {
        log(-1, "extractResourcesToFolderFromJar: not on classpath: %s", aJar);
        return null;
      }
      try {
        String sJar = "file:" + uaJar.getPath() + "!/";
        uaJar = new URL("jar", null, sJar);
      } catch (MalformedURLException ex) {
        log(-1, "extractResourcesToFolderFromJar: bad URL for:\n%s", uaJar);
        return null;
      }
    }
    content = doResourceListJar(uaJar, fpRessources, content, filter);
    if (fFolder == null) {
      return content;
    }
    copyFromJarToFolderWithList(uaJar, fpRessources, content, fFolder);
    return content;
  }

  /**
   * store a resource found on classpath to a file in the given folder with same filename
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @return success
   */
  public boolean extractResourceToFile(String inPrefix, String inFile, File outDir) {
    return extractResourceToFile(inPrefix, inFile, outDir, "");
  }

  /**
   * store a resource found on classpath to a file in the given folder
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @param outFile  the filename for export
   * @return success
   */
  public boolean extractResourceToFile(String inPrefix, String inFile, File outDir, String outFile) {
    InputStream aIS;
    FileOutputStream aFileOS;
    String content = inPrefix + "/" + inFile;
    try {
      content = runningWindows ? content.replace("\\", "/") : content;
      if (!content.startsWith("/")) {
        content = "/" + content;
      }
      aIS = (InputStream) runTimeClass.getResourceAsStream(content);
      if (aIS == null) {
        File fInFile = new File(content);
        if (!fInFile.exists()) {
          throw new IOException(String.format("resource not accessible: %s", content));
        }
        aIS = new FileInputStream(fInFile);
      }
      File out = outFile.isEmpty() ? new File(outDir, inFile) : new File(outDir, outFile);
      if (!out.getParentFile().exists()) {
        out.getParentFile().mkdirs();
      }
      aFileOS = new FileOutputStream(out);
      copy(aIS, aFileOS);
      aIS.close();
      aFileOS.close();
    } catch (Exception ex) {
      log(-1, "extractResourceToFile: %s\n%s", content, ex);
      return false;
    }
    return true;
  }

  /**
   * store the content of a resource found on classpath in the returned string
   *
   * @param inPrefix a subtree from root found in classpath (leading /)
   * @param inFile   the filename combined with the prefix on classpath
   * @param encoding
   * @return file content
   */
  public String extractResourceToString(String inPrefix, String inFile, String encoding) {
    InputStream aIS = null;
    String out = null;
    String content = inPrefix + "/" + inFile;
    if (!content.startsWith("/")) {
      content = "/" + content;
    }
    try {
      content = runningWindows ? content.replace("\\", "/") : content;
      aIS = (InputStream) runTimeClass.getResourceAsStream(content);
      if (aIS == null) {
        throw new IOException("resource not accessible");
      }
      if (encoding == null) {
        encoding = "UTF-8";
        out = new String(copy(aIS));
      } else if (encoding.isEmpty()) {
        out = new String(copy(aIS), "UTF-8");
      } else {
        out = new String(copy(aIS), encoding);
      }
      aIS.close();
      aIS = null;
    } catch (Exception ex) {
      log(-1, "extractResourceToString as %s from:\n%s\n%s", encoding, content, ex);
    }
    try {
      if (aIS != null) {
        aIS.close();
      }
    } catch (Exception ex) {
    }
    return out;
  }

  public URL resourceLocation(String folderOrFile) {
    log(lvl, "resourceLocation: (%s) %s", runTimeClass, folderOrFile);
    if (!folderOrFile.startsWith("/")) {
      folderOrFile = "/" + folderOrFile;
    }
    return runTimeClass.getResource(folderOrFile);
  }

  private List<String> resourceList(String folder, FilenameFilter filter) {
    List<String> files = new ArrayList<String>();
    if (!folder.startsWith("/")) {
      folder = "/" + folder;
    }
    URL uFolder = resourceLocation(folder);
    File fFolder = null;
    if (uFolder == null) {
      fFolder = new File(folder);
      if (fFolder.exists()) {
        files = doResourceListFolder(fFolder, files, filter);
      } else {
        log(lvl, "resourceList: not found: %s", folder);
      }
      return files;
    }
    try {
      uFolder = new URL(uFolder.toExternalForm().replaceAll(" ", "%20"));
    } catch (Exception ex) {
    }
    URL uContentList = runTimeClass.getResource(folder + "/" + fpContent);
    if (uContentList != null) {
      return doResourceListWithList(folder, files, filter);
    }
    try {
      fFolder = new File(uFolder.toURI());
      log(lvl, "resourceList: having folder: %s", fFolder);
      files.add(fFolder.getPath());
      files = doResourceListFolder(fFolder, files, filter);
      files.remove(0);
      return files;
    } catch (Exception ex) {
      if (!"jar".equals(uFolder.getProtocol())) {
        log(lvl, "resourceList:\n%s", folder);
        log(-1, "resourceList: URL neither folder nor jar:\n%s", ex);
        return null;
      }
    }
    String[] parts = uFolder.getPath().split("!");
    if (parts.length < 2 || !parts[0].startsWith("file:")) {
      log(lvl, "resourceList:\n%s", folder);
      log(-1, "resourceList: not a valid jar URL: " + uFolder.getPath());
      return null;
    }
    String fpFolder = parts[1];
    log(lvl, "resourceList: having jar: %s", uFolder);
    return doResourceListJar(uFolder, fpFolder, files, filter);
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param target the file to write the list (if null, only list - no file)
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public String[] resourceListAsFile(String folder, File target, FilenameFilter filter) {
    String content = resourceListAsString(folder, filter);
    if (content == null) {
      log(-1, "resourceListAsFile: did not work: %s", folder);
      return null;
    }
    if (target != null) {
      try {
        FileManager.deleteFileOrFolder(target.getAbsolutePath());
        target.getParentFile().mkdirs();
        PrintWriter aPW = new PrintWriter(target);
        aPW.write(content);
        aPW.close();
      } catch (Exception ex) {
        log(-1, "resourceListAsFile: %s:\n%s", target, ex);
      }
    }
    return content.split(sysPropLineSep);
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public String[] resourceListAsSikulixContent(String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = resourceList(folder, filter);
    if (contentList == null) {
      log(-1, "resourceListAsSikulixContent: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        FileManager.deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log(-1, "resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public String[] resourceListAsSikulixContentFromJar(String aJar, String folder, File targetFolder, FilenameFilter
      filter) {
    List<String> contentList = extractResourcesToFolderFromJar(aJar, folder, null, filter);
    if (contentList == null || contentList.size() == 0) {
      log(-1, "resourceListAsSikulixContentFromJar: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        FileManager.deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log(-1, "resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with system line separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return the resulting string
   */
  public String resourceListAsString(String folder, FilenameFilter filter) {
    return resourceListAsString(folder, filter, null);
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with given separator<br>
   * non-compact format: every file with full path
   *
   * @param folder    path of the subtree relative to root with leading /
   * @param filter    implementation of interface FilenameFilter or null for no filtering
   * @param separator to be used to separate the entries
   * @return the resulting string
   */
  public String resourceListAsString(String folder, FilenameFilter filter, String separator) {
    List<String> aList = resourceList(folder, filter);
    if (aList == null) {
      return null;
    }
    if (separator == null) {
      separator = sysPropLineSep;
    }
    String out = "";
    String subFolder = "";
    if (aList != null && aList.size() > 0) {
      for (String eFile : aList) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        out += eFile.replace("\\", "/") + separator;
      }
    }
    return out;
  }

  private List<String> doResourceListFolder(File fFolder, List<String> files, FilenameFilter filter) {
    int localLevel = testing ? lvl : lvl + 1;
    String subFolder = "";
    if (fFolder.isDirectory()) {
      if (files.size() > 0 && !FileManager.pathEquals(fFolder.getPath(), files.get(0))) {
        subFolder = fFolder.getPath().substring(files.get(0).length() + 1).replace("\\", "/") + "/";
        if (filter != null && !filter.accept(new File(files.get(0), subFolder), "")) {
          return files;
        }
      } else {
        logp(localLevel, "scanning folder:\n%s", fFolder);
        subFolder = "/";
        files.add(subFolder);
      }
      String[] subList = fFolder.list();
      for (String entry : subList) {
        File fEntry = new File(fFolder, entry);
        if (fEntry.isDirectory()) {
          files.add(fEntry.getAbsolutePath().substring(1 + files.get(0).length()).replace("\\", "/") + "/");
          doResourceListFolder(fEntry, files, filter);
          files.add(subFolder);
        } else {
          if (filter != null && !filter.accept(fFolder, entry)) {
            continue;
          }
          logp(localLevel, "from %s adding: %s", (subFolder.isEmpty() ? "." : subFolder), entry);
          files.add(fEntry.getAbsolutePath().substring(1 + fFolder.getPath().length()));
        }
      }
    }
    return files;
  }

  public List<String> doResourceListWithList(String folder, List<String> files, FilenameFilter filter) {
    String content = extractResourceToString(folder, fpContent, "");
    String[] contentList = content.split(content.indexOf("\r") != -1 ? "\r\n" : "\n");
    if (filter == null) {
      files.addAll(Arrays.asList(contentList));
    } else {
      for (String fpFile : contentList) {
        if (filter.accept(new File(fpFile), "")) {
          files.add(fpFile);
        }
      }
    }
    return files;
  }

  private List<String> doResourceListJar(URL uJar, String fpResource, List<String> files, FilenameFilter filter) {
    ZipInputStream zJar;
    String fpJar = uJar.getPath().split("!")[0];
    int localLevel = testing ? lvl : lvl + 1;
    String fileSep = "/";
    if (!fpJar.endsWith(".jar")) {
      return files;
    }
    logp(localLevel, "scanning jar:\n%s", uJar);
    fpResource = (fpResource.startsWith("/") ? fpResource.substring(1) : fpResource) + "/";
    File fFolder = new File(fpResource);
    File fSubFolder = null;
    ZipEntry zEntry;
    String subFolder = "";
    boolean skip = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        if (zEntry.getName().endsWith("/")) {
          continue;
        }
        String zePath = zEntry.getName();
        if (zePath.startsWith(fpResource)) {
//          if (fpResource.length()  == zePath.length()) {
//            files.add(zePath);
//            return files;
//          }
          String zeName = zePath.substring(fpResource.length());
          int nSep = zeName.lastIndexOf(fileSep);
          String zefName = zeName.substring(nSep + 1, zeName.length());
          String zeSub = "";
          if (nSep > -1) {
            zeSub = zeName.substring(0, nSep + 1);
            if (!subFolder.equals(zeSub)) {
              subFolder = zeSub;
              fSubFolder = new File(fFolder, subFolder);
              skip = false;
              if (filter != null && !filter.accept(fSubFolder, "")) {
                skip = true;
                continue;
              }
              files.add(zeSub);
            }
            if (skip) {
              continue;
            }
          } else {
            if (!subFolder.isEmpty()) {
              subFolder = "";
              fSubFolder = fFolder;
              files.add("/");
            }
          }
          if (filter != null && !filter.accept(fSubFolder, zefName)) {
            continue;
          }
          files.add(zefName);
          logp(localLevel, "from %s adding: %s", (zeSub.isEmpty() ? "." : zeSub), zefName);
        }
      }
    } catch (Exception ex) {
      log(-1, "doResourceListJar: %s", ex);
      return files;
    }
    return files;
  }

  public List<String> listFilesInJar(URL uJar) {
    ZipInputStream zJar;
    String fpJar = uJar.getPath().split("!")[0];
    int localLevel = testing ? lvl : lvl + 1;
    String fileSep = "/";
    if (!fpJar.endsWith(".jar")) {
      return null;
    }
    logp(localLevel, "listFilesInJar: scanning jar:\n%s", uJar);
    List<String> files = new ArrayList<>();
    ZipEntry zEntry;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        if (zEntry.getName().endsWith("/")) {
          continue;
        }
        String zePath = zEntry.getName();
        files.add(zePath);
        logp(localLevel, "listFilesInJar: adding: %s", zePath);
      }
    } catch (Exception ex) {
      log(-1, "listFilesInJar: %s", ex);
      return files;
    }
    return files;
  }

  private boolean copyFromJarToFolderWithList(URL uJar, String fpRessource, List<String> files, File fFolder) {
    if (files == null || files.isEmpty()) {
      log(lvl, "copyFromJarToFolderWithList: list of files is empty");
      return false;
    }
    String fpJar = uJar.getPath().split("!")[0];
    if (!fpJar.endsWith(".jar")) {
      return false;
    }
    int localLevel = testing ? lvl : lvl + 1;
    logp(localLevel, "scanning jar:\n%s", uJar);
    fpRessource = fpRessource.startsWith("/") ? fpRessource.substring(1) : fpRessource;

    String subFolder = "";

    int maxFiles = files.size() - 1;
    int nFiles = 0;

    ZipEntry zEntry;
    ZipInputStream zJar;
    String zPath;
    int prefix = fpRessource.length();
    fpRessource += !fpRessource.isEmpty() ? "/" : "";
    String current = "/";
    boolean shouldStop = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        zPath = zEntry.getName();
        if (zPath.endsWith("/")) {
          continue;
        }
        while (current.endsWith("/")) {
          if (nFiles > maxFiles) {
            shouldStop = true;
            break;
          }
          subFolder = current.length() == 1 ? "" : current;
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
            break;
          }
        }
        if (shouldStop) {
          break;
        }
        if (zPath.startsWith(current)) {
          if (zPath.length() == fpRessource.length() - 1) {
            log(-1, "extractResourcesToFolderFromJar: only ressource folders allowed - use filter");
            return false;
          }
          logp(localLevel, "copying: %s", zPath);
          File out = new File(fFolder, zPath.substring(prefix));
          if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
          }
          FileOutputStream aFileOS = new FileOutputStream(out);
          copy(zJar, aFileOS);
          aFileOS.close();
          if (nFiles > maxFiles) {
            break;
          }
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
          }
        }
      }
      zJar.close();
    } catch (Exception ex) {
      log(-1, "doResourceListJar: %s", ex);
      return false;
    }
    return true;
  }

  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
    out.flush();
  }

  private byte[] copy(InputStream inputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length = 0;
    while ((length = inputStream.read(buffer)) != -1) {
      baos.write(buffer, 0, length);
    }
    return baos.toByteArray();
  }

  public class oneFileFilter implements FilenameFilter {

    String aFile;

    public oneFileFilter(String aFileGiven) {
      aFile = aFileGiven;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (name.contains(aFile)) {
        return true;
      }
      return false;
    }

  }

  //</editor-fold>

  //<editor-fold desc="16 get resources NEW">
  public List<String> getResourceList(String res) {
    return getResourceList(res, runTimeClass);
  }

  public List<String> getResourceList(String res, Class classReference) {
    List<String> resList = new ArrayList<>();
    CodeSource codeSource = classReference.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      return resList;
    }
    InputStream aIS = null;
    String content = null;
    res = new File(res, "sikulixcontent").getPath();
    if (runningWindows) {
      res = res.replace("\\", "/");
    }
    if (!res.startsWith("/")) {
      res = "/" + res;
    }
    try {
      aIS = (InputStream) classReference.getResourceAsStream(res);
      if (aIS != null) {
        content = new String(copy(aIS));
        aIS.close();
      }
      log(lvl + 1, "getResourceList: %s (%s)", res, content);
      aIS = null;
    } catch (Exception ex) {
      log(-1, "getResourceList: %s (%s)", res, ex);
    }
    try {
      if (aIS != null) {
        aIS.close();
      }
    } catch (Exception ex) {
    }
    if (null != content) {
      String[] names = content.split("\n");
      for (String name : names) {
        if (name.equals("sikulixcontent")) continue;
        resList.add(name.trim());
      }
    }
    return resList;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="20 helpers">
  public int getLastScriptRunReturnCode() {
    return lastScriptRunReturnCode;
  }

  public void setLastScriptRunReturnCode(int lastScriptRunReturnCode) {
    this.lastScriptRunReturnCode = lastScriptRunReturnCode;
  }

  private int lastScriptRunReturnCode = 0;

  public void crash() {
    int x = 1 / 0;
  }

  public static void pause(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException ex) {
    }
  }

  public static void pause(float time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }

  public static boolean isRunningFromJar() {
    return sxRunningJar.getName().endsWith(".jar");
  }

  public static boolean isJava9(String... args) {
    if (javaVersion > 8) {
      if (args.length > 0) {
        log(-1, "*** Java 9+: %s", args[0]);
      }
      return true;
    } else {
      return false;
    }
  }

  public static boolean isJava8() {
    return javaVersion > 7;
  }

  public static void show() {
    if (sxOptions != null && sxOptions.hasOptions()) {
      sxOptions.dumpOptions();
    }
    logp("***** show environment for %s %s", SXVersionLong, runType);
    logp("user.home: %s", fUserHome);
    logp("user.dir (work dir): %s", fWorkDir);
    logp("user.name: %s", sysPropUserName);
    logp("java.io.tmpdir: %s", fTempPath);
    logp("running %dBit(%s) on %s (%s)", javaArch, sysPropOsArch, osNameShort,
        (linuxDistro.contains("???") ? osVersion : linuxDistro));
    logp(javaShow);
    logp("app data folder: %s", getAppDataFolder());
    logp("executing jar/classes: %s", getRunningJar());
    logp("***** show environment end");
  }

  public static String getVersion() {
    return SXVersion;
  }
  private static String getVersionShort() {
    return SXVersionShort;
  }

  public static String getSystemInfo() {
    return String.format("%s/%s/%s", SXVersionLong, SXSystemVersion, SXJavaVersion);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="21 runcmd">
  public final static String runCmdError = "*****error*****";
  public static String NL = "\n";

  public static String arrayToQuotedString(String[] args) {
    String ret = "";
    for (String s : args) {
      if (s.contains(" ")) {
        s = "\"" + s + "\"";
      }
      ret += s + " ";
    }
    return ret;
  }

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param cmd the command as it would be given on command line, quoting is preserved
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public String runcmd(String cmd) {
    return runcmd(new String[]{cmd});
  }

  /**
   * run a system command finally using Java::Runtime.getRuntime().exec(args) and waiting for completion
   *
   * @param args the command as it would be given on command line splitted into the space devided parts, first part is
   *             the command, the rest are parameters and their values
   * @return the output produced by the command (sysout [+ "*** error ***" + syserr] if the syserr part is present, the
   * command might have failed
   */
  public String runcmd(String args[]) {
    if (args.length == 0) {
      return "";
    }
    boolean silent = false;
    if (args.length == 1) {
      String separator = "\"";
      ArrayList<String> argsx = new ArrayList<String>();
      StringTokenizer toks;
      String tok;
      String cmd = args[0];
      if (Settings.isWindows()) {
        cmd = cmd.replaceAll("\\\\ ", "%20;");
      }
      toks = new StringTokenizer(cmd);
      while (toks.hasMoreTokens()) {
        tok = toks.nextToken(" ");
        if (tok.length() == 0) {
          continue;
        }
        if (separator.equals(tok)) {
          continue;
        }
        if (tok.startsWith(separator)) {
          if (tok.endsWith(separator)) {
            tok = tok.substring(1, tok.length() - 1);
          } else {
            tok = tok.substring(1);
            tok += toks.nextToken(separator);
          }
        }
        argsx.add(tok.replaceAll("%20;", " "));
      }
      args = argsx.toArray(new String[0]);
    }
    if (args[0].startsWith("!")) {
      silent = true;
      args[0] = args[0].substring(1);
    }
    if (args[0].startsWith("#")) {
      String pgm = args[0].substring(1);
      args[0] = (new File(pgm)).getAbsolutePath();
      runcmd(new String[]{"chmod", "ugo+x", args[0]});
    }
    String result = "";
    String error = runCmdError + NL;
    String errorOut = "";
    boolean hasError = false;
    int retVal;
    try {
      if (!silent) {
        if (lvl <= Debug.getDebugLevel()) {
          log(lvl, arrayToQuotedString(args));
        } else {
          Debug.info("runcmd: " + arrayToQuotedString(args));
        }
      }
      //TODO use ProcessRunner
      Process process = Runtime.getRuntime().exec(args);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      String s;
      while ((s = stdInput.readLine()) != null) {
        if (!s.isEmpty()) {
          result += s + NL;
        }
      }
      while ((s = stdError.readLine()) != null) {
        if (!s.isEmpty()) {
          errorOut += s + NL;
        }
      }
      if (!errorOut.isEmpty()) {
        error = error + errorOut;
        hasError = true;
      }
      process.waitFor();
      retVal = process.exitValue();
      process.destroy();
    } catch (Exception e) {
      log(-1, "fatal error: " + e);
      result = String.format(error + "%s", e);
      retVal = 9999;
      hasError = true;
    }
    if (hasError) {
      result += error;
    }
    lastResult = result;
    return String.format("%d%s%s", retVal, NL, result);
  }

  public String getLastCommandResult() {
    return lastResult;
  }
  private String lastResult = "";
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="22 system enviroment">
  public static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  /**
   * print the current java system properties key-value pairs sorted by key
   */

  public static void dumpSysProps() {
    dumpSysProps(null);
  }

  /**
   * print the current java system properties key-value pairs sorted by key but only keys containing filter
   *
   * @param filter the filter string
   */
  public static void dumpSysProps(String filter) {
    filter = filter == null ? "" : filter;
    Debug.logp("*** system properties dump " + filter);
    ArrayList<String> keysProp = new ArrayList<String>();
    Integer nL = 0;
    String entry;
    for (Object e : sysProps.keySet()) {
      entry = (String) e;
      if (entry.length() > nL) {
        nL = entry.length();
      }
      if (filter.isEmpty() || !filter.isEmpty() && entry.contains(filter)) {
        keysProp.add(entry);
      }
    }
    Collections.sort(keysProp);
    String form = "%-" + nL.toString() + "s = %s";
    for (Object e : keysProp) {
      Debug.logp(form, e, sysProps.get(e));
    }
    Debug.logp("*** system properties dump end" + filter);
  }
  //</editor-fold>

  //<editor-fold desc="99 cleanUp">
  public static void terminate() {
    terminate(0, "");
  }

  public static void terminate(int retval, String message, Object... args) {
    String outMsg = String.format(message, args);
    if (!outMsg.isEmpty()) {
      System.out.println("TERMINATING: " + outMsg);
    }
    if (retval < 999) {
      isTerminating = true;
      cleanUp();
      System.exit(retval);
    }
    throw new SikuliXception(String.format("fatal: " + outMsg));
  }

  public static void cleanUp() {
    if (!isTerminating) {
      runTime.log(3, "***** running cleanUp *****");
      Highlight.closeAll();
      Settings.DefaultHighlightColor = "RED";
      Settings.DefaultHighlightTime = 2.0f;
      Settings.Highlight = false;
      Settings.setShowActions(false);
      FindFailed.reset();
    }

    try {
      VNCScreen.stopAll();
      ExtensionManager.invokeStatic("ADBScreen.stop");
    } catch (Exception e) {
      Debug.info("Error while stopping VNCScreen: %s", e.getMessage());
    }

    Observing.cleanUp();
    HotkeyManager.reset(isTerminating);
    if (null != cleanupRobot) {
      cleanupRobot.keyUp();
    }
    Mouse.reset();
    if (isTerminating) {
      stopPythonServer();
    }
  }

  private static void runShutdownHook() {
    isTerminating = true;
    if (Debug.isStartWithTrace()) {
      Debug.on(3);
      Debug.globalTraceOn();
    }
    runTime.log(runTime.lvl, "***** final cleanup at System.exit() *****");
    cleanUp();

    if (runTime.isRunning != null) {
      try {
        runTime.isRunningFile.close();
      } catch (IOException ex) {
      }
      runTime.isRunning.delete();
    }

    for (File f : runTime.fTempPath.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name.toLowerCase().contains("sikuli")) {
          if (name.contains("Sikulix_")) {
            if (isObsolete(new File(dir, name).lastModified()) || name.equals(runTime.fBaseTempPath.getName())) {
              return true;
            }
          } else {
            return true;
          }
        }
        return false;
      }
    })) {
      runTime.log(4, "cleanTemp: " + f.getName());
      FileManager.deleteFileOrFolder(f.getAbsolutePath());
    }
  }
  //</editor-fold>
}

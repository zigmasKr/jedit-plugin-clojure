package clojure;
 /**
 * @author Damien Radtke
 * class ClojurePlugin
 * The main class for the clojure plugin.
 * Handles all loading/unloading of clojure jars
 * @author Zigmantas Kryzius - June, 2017:
 * Appended with Cojure scripting (JSR223) jar.
 */
//{{{ Imports
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.IOUtilities;
//}}}

public class ClojurePlugin extends EditPlugin {

	public static final String propCorePath = "options.clojure.clojure-core-path";
	public static final String propContribPath = "options.clojure.clojure-contrib-path";
	public static final String propScriptingPath = "options.clojure.clojure-jsr223-path";

	public static final String nameCore = jEdit.getProperty("options.clojure.clojure-core-jar");
	public static final String nameContrib = jEdit.getProperty("options.clojure.clojure-contrib-jar");
	public static final String nameScripting = jEdit.getProperty("options.clojure.clojure-jsr223-jar");

	public static final String dirSettings = jEdit.getSettingsDirectory();
	public static final String dirHome = jEdit.getJEditHome();

	// 'included...' jars may be placed in the jEdit settings directory:
	public static final String coreInSettings =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameCore);
	public static final String contribInSettings =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameContrib);
	public static final String scriptingInSettings =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameScripting);
	// OR
	//'included...' jars may be placed in the jEdit install directory:
	public static final String coreInHome =
		MiscUtilities.constructPath(dirHome, "jars/" + nameCore);
	public static final String contribInHome =
		MiscUtilities.constructPath(dirHome, "jars/" + nameContrib);
	public static final String scriptingInHome =
		MiscUtilities.constructPath(dirHome, "jars/" + nameScripting);

	private static String defineIncluded(String settings, String home) {
		String included = null;
		File inSettings = new File(settings);
		File inHome = new File(home);
		if (inSettings.exists()) {
			included = settings;
		} else if (inHome.exists()) {
			included = home;
		}
		return included;
	}

	private String defineWorking(String propJarPath, String includedJar) {
		String workingJar;
		File pathJar;
		if (!(jEdit.getProperty(propJarPath) == null)) {
			pathJar = new File(jEdit.getProperty(propJarPath));
			if (pathJar.exists()) {
				// jEdit.getProperty(propJarPath) points to working *.jar.
				workingJar = jEdit.getProperty(propJarPath);
			} else {
				// IF path by the property does not exist, ...
				jEdit.setProperty(propJarPath, includedJar);
				workingJar = includedJar;
			}
		} else {
			// OR property is null
			// the property is set to 'included...'
			jEdit.setProperty(propJarPath, includedJar);
			workingJar = includedJar;
		}
		return workingJar;
	}

	// declare 'included...' jars:
	public static String includedCore = null;
	public static String includedContrib = null;
	public static String includedScripting = null;
	// declare 'working...' jars:
	private String workingCore = null;
	private String workingContrib = null;
	private String workingScripting = null;

	public void start() {
	   // define 'included...' jars:
	   includedCore = defineIncluded(coreInSettings, coreInHome);
	   includedContrib = defineIncluded(contribInSettings, contribInHome);
	   includedScripting = defineIncluded(scriptingInSettings, scriptingInHome);
	   // define 'working...' jars:
	   workingCore = defineWorking(propCorePath, includedCore);
	   workingContrib = defineWorking(propContribPath, includedContrib);
	   workingScripting = defineWorking(propScriptingPath, includedScripting);

		setVars();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (jEdit.getPlugin("console.ConsolePlugin") != null) {
					File clojureCommand = new File(console.ConsolePlugin.getUserCommandDirectory(), "clojure.xml");
					if (!clojureCommand.exists()) {
						try {
							InputStream in = getClass().getResourceAsStream(File.separator + "commands" +
							   File.separator + "clojure.xml");
							OutputStream out = new FileOutputStream(clojureCommand);
							IOUtilities.copyStream(null, in, out, false);
							IOUtilities.closeQuietly(in);
							IOUtilities.closeQuietly(out);
							console.ConsolePlugin.rescanCommands();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
	}

	public void stop() {}

	/**
	 * Set the loaded embeddable clojure core jar; method used in ..ProviderOptionPane
	 */
	public void setClojureCore(String path) {
		jEdit.setProperty(propCorePath, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(workingCore), false);
		jEdit.addPluginJAR(path);
		workingCore = path;
	}

	/**
	 * Set the loaded embeddable clojure contrib jar; method used in ..ProviderOptionPane
	 */
	public void setClojureContrib(String path) {
		jEdit.setProperty(propContribPath, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(workingContrib), false);
		jEdit.addPluginJAR(path);
		workingContrib = path;
	}

	/**
	 * Set the loaded embeddable clojure scripting (JSR223) jar; method used in ..ProviderOptionPane
	 */
	public void setClojureScripting(String path) {
		jEdit.setProperty(propScriptingPath, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(workingScripting), false);
		jEdit.addPluginJAR(path);
		workingScripting = path;
	}

	/**
	 * If Console is installed, set some environment variables
	 * - Set CLOJURE to the path of the clojure jar
	 */
	public void setVars() {
		if (jEdit.getPlugin("console.ConsolePlugin") != null) {
			console.ConsolePlugin.setSystemShellVariableValue("CLOJURE", getClojure());
		}
	}

	/**
	 * Returns the location of the clojure core jar
	 */
	public String getClojureCore() {
		return jEdit.getProperty(propCorePath);
	}

	/**
	 * Returns the location of the clojure contrib jar
	 */
	public String getClojureContrib() {
		return jEdit.getProperty(propContribPath);
	}

	/**
	 * Returns the location of the clojure scripting (JSR223) jar
	 */
	public String getClojureScripting() {
		return jEdit.getProperty(propScriptingPath);
	}

	/**
	 * Returns the paths of core, contrib and scripting, separated by a path separator
	 * Ideal for setting environment paths and for use in the system shell
	 */
	public String getClojure() {
		String core = getClojureCore();
		String contrib = getClojureContrib();
		String scripting = getClojureScripting();
		return core + File.pathSeparator + contrib +
			File.pathSeparator + scripting;
	}

}

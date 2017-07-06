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

	public static final String coreProp = "options.clojure.clojure-core-path";
	public static final String contribProp = "options.clojure.clojure-contrib-path";
	public static final String scriptingProp = "options.clojure.clojure-jsr223-path";

	// If ...Prop are set to null initially,
	// then properties from a previous session cannot be utilized.
	// If included jars are moved from/to settings/home,
	// then ...Prop in properties file should be removed before starting jEdit
	
	public static final String nameCore = jEdit.getProperty("options.clojure.clojure-core-jar");
	public static final String nameContrib = jEdit.getProperty("options.clojure.clojure-contrib-jar");
	public static final String nameScripting = jEdit.getProperty("options.clojure.clojure-jsr223-jar");

	public static final String dirSettings = jEdit.getSettingsDirectory();
	
	// 'included...' jars are initially meant in settings
	public static String includedCore =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameCore);
	public static String includedContrib =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameContrib);
	public static String includedScripting =
		MiscUtilities.constructPath(dirSettings, "jars/" + nameScripting);

	// 'included...' jars may be placed in the jEdit install directory, too:
	public static final String dirHome = jEdit.getJEditHome();
	
	public static final String inhomeCore =
		MiscUtilities.constructPath(dirHome, "jars/" + nameCore);
	public static final String inhomeContrib =
		MiscUtilities.constructPath(dirHome, "jars/" + nameContrib);
	public static final String inhomeScripting =
		MiscUtilities.constructPath(dirHome, "jars/" + nameScripting);

	private static String findIncluded(String settings, String home) {
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
	                            
	private String installedCore = null;
	private String installedContrib = null;
	private String installedScripting = null;
	
	public void start() {
		
		// finally, 'included...' jars can be in settings or in home dir:
		includedCore = findIncluded(includedCore, inhomeCore);
		includedContrib = findIncluded(includedContrib, inhomeContrib);
		includedScripting = findIncluded(includedScripting, inhomeScripting);
		
		// If core/contrib/scripting properties are not defined, 
		// they are set to 'included...' jars
		if (jEdit.getProperty(coreProp) == null) {
			jEdit.setProperty(coreProp, includedCore);
		}

		if (jEdit.getProperty(contribProp) == null) {
			jEdit.setProperty(contribProp, includedContrib);
		}
		
		if (jEdit.getProperty(scriptingProp) == null) {
			jEdit.setProperty(scriptingProp, includedScripting);
		}

		installedCore = getClojureCore();
		if (!installedCore.equals(includedCore)) {
			jEdit.removePluginJAR(jEdit.getPluginJAR(includedCore), false);
			jEdit.addPluginJAR(installedCore);
		}

		installedContrib = getClojureContrib();
		if (!installedContrib.equals(includedContrib)) {
			jEdit.removePluginJAR(jEdit.getPluginJAR(includedContrib), false);
			jEdit.addPluginJAR(installedContrib);
		}
		
		installedScripting = getClojureScripting();
		if (!installedScripting.equals(includedScripting)) {
			jEdit.removePluginJAR(jEdit.getPluginJAR(includedScripting), false);
			jEdit.addPluginJAR(installedScripting);
		}

		setVars();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (jEdit.getPlugin("console.ConsolePlugin") != null) {
					File clojureCommand = new File(console.ConsolePlugin.getUserCommandDirectory(), "clojure.xml");
					if (!clojureCommand.exists()) {
						try {
							InputStream in = getClass().getResourceAsStream("/commands/clojure.xml");
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
	 * Set the loaded embeddable clojure core jar
	 */
	public void setClojureCore(String path) {
		jEdit.setProperty(coreProp, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(installedCore), false);
		jEdit.addPluginJAR(path);
		installedCore = path;
	}

	/**
	 * Set the loaded embeddable clojure contrib jar
	 */
	public void setClojureContrib(String path) {
		jEdit.setProperty(contribProp, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(installedContrib), false);
		jEdit.addPluginJAR(path);
		installedContrib = path;
	}
	
	/**
	 * Set the loaded embeddable clojure scripting (JSR223) jar
	 */
	public void setClojureScripting(String path) {
		jEdit.setProperty(scriptingProp, path);
		jEdit.removePluginJAR(jEdit.getPluginJAR(installedScripting), false);
		jEdit.addPluginJAR(path);
		installedScripting = path;
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
		return jEdit.getProperty(coreProp);
	}

	/**
	 * Returns the location of the clojure contrib jar
	 */
	public String getClojureContrib() {
		return jEdit.getProperty(contribProp);
	}
	
	/**
	 * Returns the location of the clojure scripting (JSR223) jar
	 */
	public String getClojureScripting() {
		return jEdit.getProperty(scriptingProp);
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

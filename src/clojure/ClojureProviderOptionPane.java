package clojure;
/**
 * @author Damien Radtke
 * class ClojureProviderOptionPane
 * An option pane that can be used to configure the clojure jars
 * @author Zigmantas Kryzius - June, 2017:
 * An option pane appended with buttons to configure Clojure scripting (JSR223).
 */
//{{{ Imports
import clojure.ClojurePlugin;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.browser.VFSFileChooserDialog;
import org.gjt.sp.jedit.jEdit;
//}}}
public class ClojureProviderOptionPane extends AbstractOptionPane {

	// included: included into the plugin's library
	// custom: chosen via plugin's option pane

	private JRadioButton coreIncluded;
	private JRadioButton coreCustom;
	private JTextField corePath;
	private JButton coreBrowse;

	private JRadioButton contribIncluded;
	private JRadioButton contribCustom;
	private JTextField contribPath;
	private JButton contribBrowse;

	private JRadioButton scriptingIncluded;
	private JRadioButton scriptingCustom;
	private JTextField scriptingPath;
	private JButton scriptingBrowse;

	private ClojurePlugin plugin;

	public ClojureProviderOptionPane() {
		super("clojure-provider");
		plugin = (ClojurePlugin) jEdit.getPlugin("clojure.ClojurePlugin");
	}

	protected void _init() {
		ButtonHandler handler = new ButtonHandler();

		// Core
		JPanel corePanel = new JPanel();
		corePanel.setLayout(new BoxLayout(corePanel, BoxLayout.X_AXIS));
		corePanel.add(coreIncluded = new JRadioButton(jEdit.getProperty(
			"options.clojure.included-core-label")));
		corePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		corePanel.add(coreCustom = new JRadioButton(jEdit.getProperty(
			"options.clojure.choose-label")));
		ButtonGroup coreGroup = new ButtonGroup();
		coreGroup.add(coreIncluded);
		coreGroup.add(coreCustom);
		corePanel.add(corePath = new JTextField());
		coreBrowse = new JButton(jEdit.getProperty("vfs.browser.browse.label"));
		coreBrowse.addActionListener(new BrowseHandler(corePath));
		corePanel.add(coreBrowse);
		String core = plugin.getClojureCore();
		if (core.equals(ClojurePlugin.includedCore)) {
		   coreIncluded.setSelected(true);
		   corePath.setText("");
			corePath.setEnabled(false);
			coreBrowse.setEnabled(false);
		} else {
			coreCustom.setSelected(true);
			corePath.setText(core);
		}
		coreIncluded.addActionListener(handler);
		coreCustom.addActionListener(handler);
		addComponent("Core:", corePanel);

		// Contrib
		JPanel contribPanel = new JPanel();
		contribPanel.setLayout(new BoxLayout(contribPanel, BoxLayout.X_AXIS));
		contribPanel.add(contribIncluded = new JRadioButton("Included (1.2.0)"));
		contribPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		contribPanel.add(contribCustom = new JRadioButton("Choose jar"));
		ButtonGroup contribGroup = new ButtonGroup();
		contribGroup.add(contribIncluded);
		contribGroup.add(contribCustom);
		contribPanel.add(contribPath = new JTextField());
		contribBrowse = new JButton(jEdit.getProperty("vfs.browser.browse.label"));
		contribBrowse.addActionListener(new BrowseHandler(contribPath));
		contribPanel.add(contribBrowse);
		String contrib = plugin.getClojureContrib();
		if (contrib.equals(ClojurePlugin.includedContrib)) {
			contribIncluded.setSelected(true);
			contribPath.setText("");
			contribPath.setEnabled(false);
			contribBrowse.setEnabled(false);
		} else {
			contribCustom.setSelected(true);
			contribPath.setText(contrib);
		}
		contribIncluded.addActionListener(handler);
		contribCustom.addActionListener(handler);
		addComponent("Contrib:", contribPanel);

		// JSR223 (scripting)
		String strA = "1.2.0";
		String strB = "1.2";
		JLabel lblA = new JLabel(strA);
		JLabel lblB = new JLabel(strB);
		Font fA = lblA.getFont();
		// font of lblB is assumed to be the same
		// https://stackoverflow.com/questions/2843601/java-fontmetrics-without-graphics
		FontMetrics fmA = lblA.getFontMetrics(fA);
		FontMetrics fmB = lblB.getFontMetrics(fA);
		int diff = fmA.stringWidth(strA) - fmB.stringWidth(strB);
		//
		JPanel scriptingPanel = new JPanel();
		scriptingPanel.setLayout(new BoxLayout(scriptingPanel, BoxLayout.X_AXIS));
		scriptingPanel.add(scriptingIncluded = new JRadioButton("Included (1.2)"));
		scriptingPanel.add(Box.createRigidArea(new Dimension(diff, 0)));
		scriptingPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		scriptingPanel.add(scriptingCustom = new JRadioButton("Choose jar"));
		ButtonGroup scriptingGroup = new ButtonGroup();
		scriptingGroup.add(scriptingIncluded);
		scriptingGroup.add(scriptingCustom);
		scriptingPanel.add(scriptingPath = new JTextField());
		scriptingBrowse = new JButton(jEdit.getProperty("vfs.browser.browse.label"));
		scriptingBrowse.addActionListener(new BrowseHandler(scriptingPath));
		scriptingPanel.add(scriptingBrowse);
		String scripting = plugin.getClojureScripting();
		if (scripting.equals(ClojurePlugin.includedScripting)) {
			scriptingIncluded.setSelected(true);
			scriptingPath.setText("");
			scriptingPath.setEnabled(false);
			scriptingBrowse.setEnabled(false);
		} else {
			scriptingCustom.setSelected(true);
			scriptingPath.setText(scripting);
		}
		scriptingIncluded.addActionListener(handler);
		scriptingCustom.addActionListener(handler);
		addComponent("JSR223:", scriptingPanel);
	}

	protected void _save() {

		if (coreIncluded.isSelected()) {
			plugin.setClojureCore(ClojurePlugin.includedCore);
		} else {
			plugin.setClojureCore(corePath.getText());
		}

		if (contribIncluded.isSelected()) {
			plugin.setClojureContrib(ClojurePlugin.includedContrib);
		} else {
			plugin.setClojureContrib(contribPath.getText());
		}

		if (scriptingIncluded.isSelected()) {
			plugin.setClojureScripting(ClojurePlugin.includedScripting);
		} else {
			plugin.setClojureScripting(scriptingPath.getText());
		}

		plugin.setVars();
	}

	class ButtonHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == coreIncluded) {
			   corePath.setText("");
				corePath.setEnabled(false);
				coreBrowse.setEnabled(false);
			} else if (source == coreCustom) {
				corePath.setEnabled(true);
				coreBrowse.setEnabled(true);
			} else if (source == contribIncluded) {
			   contribPath.setText("");
				contribPath.setEnabled(false);
				contribBrowse.setEnabled(false);
			} else if (source == contribCustom) {
				contribPath.setEnabled(true);
				contribBrowse.setEnabled(true);
			} else if (source == scriptingIncluded) {
			   scriptingPath.setText("");
				scriptingPath.setEnabled(false);
				scriptingBrowse.setEnabled(false);
			} else if (source == scriptingCustom) {
				scriptingPath.setEnabled(true);
				scriptingBrowse.setEnabled(true);
			}
		}

	}

	class BrowseHandler implements ActionListener {

		private JTextField txt;

		public BrowseHandler(JTextField txt) {
			this.txt = txt;
		}

		public void actionPerformed(ActionEvent e) {
			VFSFileChooserDialog dialog = new VFSFileChooserDialog(
				jEdit.getActiveView(), System.getProperty("user.dir")+File.separator,
				VFSBrowser.OPEN_DIALOG, false, true);
			String[] files = dialog.getSelectedFiles();
			if (files != null && files.length == 1) {
				txt.setText(files[0]);
			}
		}

	}

}

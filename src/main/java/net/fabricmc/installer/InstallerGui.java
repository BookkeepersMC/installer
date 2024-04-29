/*
 * MIT License
 *
 * Copyright (c) 2023, 2024 BookkeepersMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.installer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.xml.stream.XMLStreamException;

import net.fabricmc.installer.util.Utils;

@SuppressWarnings("serial")
public class InstallerGui extends JFrame {
	public static InstallerGui instance;

	private JTabbedPane contentPane;

	public InstallerGui() throws IOException {
		initComponents();
		setContentPane(contentPane);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Image iconImage = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
		setIconImage(iconImage);
		setTaskBarImage(iconImage);

		instance = this;

		Main.loadMetadata();
	}

	public static void selectInstallLocation(Supplier<String> initalDir, Consumer<String> selectedDir) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(initalDir.get()));
		chooser.setDialogTitle(Utils.BUNDLE.getString("prompt.select.location"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			selectedDir.accept(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	public static void start() throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, XMLStreamException {
		//This will make people happy
		String lafCls = UIManager.getSystemLookAndFeelClassName();
		UIManager.setLookAndFeel(lafCls);

		if (lafCls.endsWith("AquaLookAndFeel")) { // patch osx tab text color bug JDK-8251377
			UIManager.put("TabbedPane.foreground", Color.BLACK);
		}

		InstallerGui dialog = new InstallerGui();
		dialog.updateSize(true);
		dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	public void updateSize(boolean updateMinimum) {
		if (updateMinimum) setMinimumSize(null);
		setPreferredSize(null);
		pack();
		Dimension size = getPreferredSize();
		if (updateMinimum) setMinimumSize(size);
		setPreferredSize(new Dimension(Math.max(450, size.width), size.height));
		setSize(getPreferredSize());
	}

	private void initComponents() {
		contentPane = new JTabbedPane(JTabbedPane.TOP);
		Main.HANDLERS.forEach(handler -> contentPane.addTab(Utils.BUNDLE.getString("tab." + handler.name().toLowerCase(Locale.ROOT)), handler.makePanel(this)));
	}

	private static void setTaskBarImage(Image image) {
		try {
			// Only supported in Java 9 +
			Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
			Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
			Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
			Object taskbar = getTaskbar.invoke(null);
			setIconImage.invoke(taskbar, image);
		} catch (Exception e) {
			// Ignored, running on Java 8
		}
	}
}

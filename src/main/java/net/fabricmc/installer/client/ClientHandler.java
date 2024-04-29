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

package net.fabricmc.installer.client;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import net.fabricmc.installer.Handler;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.launcher.MojangLauncherHelperWrapper;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.NoopCaret;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ClientHandler extends Handler {
	private JCheckBox createProfile;

	@Override
	public String name() {
		return "Client";
	}

	@Override
	public void install() {
		if (MojangLauncherHelperWrapper.isMojangLauncherOpen()) {
			showLauncherOpenMessage();
			return;
		}

		doInstall();
	}

	private void doInstall() {
		String gameVersion = (String) gameVersionComboBox.getSelectedItem();
		LoaderVersion loaderVersion = queryLoaderVersion();
		if (loaderVersion == null) return;

		System.out.println("Installing");

		new Thread(() -> {
			try {
				updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing")).format(new Object[]{loaderVersion.name}));
				Path mcPath = Paths.get(installLocation.getText());

				if (!Files.exists(mcPath)) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
				}

				final ProfileInstaller profileInstaller = new ProfileInstaller(mcPath);
				ProfileInstaller.LauncherType launcherType = null;

				if (createProfile.isSelected()) {
					List<ProfileInstaller.LauncherType> types = profileInstaller.getInstalledLauncherTypes();

					if (types.size() == 0) {
						throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
					} else if (types.size() == 1) {
						launcherType = types.get(0);
					} else {
						launcherType = showLauncherTypeSelection();

						if (launcherType == null) {
							// canceled
							statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
							return;
						}
					}
				}

				String profileName = ClientInstaller.install(mcPath, gameVersion, loaderVersion, this);

				if (createProfile.isSelected()) {
					if (launcherType == null) {
						throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
					}

					profileInstaller.setupProfile(profileName, gameVersion, launcherType);
				}

				SwingUtilities.invokeLater(() -> showInstalledMessage(loaderVersion.name, gameVersion, mcPath.resolve("mods")));
			} catch (Exception e) {
				error(e);
			} finally {
				buttonInstall.setEnabled(true);
			}
		}).start();
	}

	private void showInstalledMessage(String loaderVersion, String gameVersion, Path modsDirectory) {
		JEditorPane pane = new JEditorPane("text/html", "<html><body style=\"" + buildEditorPaneStyle() + "\">" + new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(new Object[]{loaderVersion, gameVersion, Reference.FABRIC_API_URL}) + "</body></html>");
		pane.setBackground(new Color(0, 0, 0, 0));
		pane.setEditable(false);
		pane.setCaret(new NoopCaret());

		pane.addHyperlinkListener(e -> {
			try {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e.getDescription().equals("fabric://mods")) {
						Desktop.getDesktop().open(modsDirectory.toFile());
					} else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} else {
						throw new UnsupportedOperationException("Failed to open " + e.getURL().toString());
					}
				}
			} catch (Throwable throwable) {
				error(throwable);
			}
		});

		final Image iconImage = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
		JOptionPane.showMessageDialog(
				null,
				pane,
				Utils.BUNDLE.getString("prompt.install.successful.title"),
				JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT))
		);
	}

	private ProfileInstaller.LauncherType showLauncherTypeSelection() {
		Object[] options = { Utils.BUNDLE.getString("prompt.launcher.type.xbox"), Utils.BUNDLE.getString("prompt.launcher.type.win32")};

		int result = JOptionPane.showOptionDialog(null,
				Utils.BUNDLE.getString("prompt.launcher.type.body"),
				Utils.BUNDLE.getString("installer.title"),
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
				);

		if (result == JOptionPane.CLOSED_OPTION) {
			return null;
		}

		return result == JOptionPane.YES_OPTION ? ProfileInstaller.LauncherType.MICROSOFT_STORE : ProfileInstaller.LauncherType.WIN32;
	}

	private void showLauncherOpenMessage() {
		int result = JOptionPane.showConfirmDialog(null, Utils.BUNDLE.getString("prompt.launcher.open.body"), Utils.BUNDLE.getString("prompt.launcher.open.tile"), JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			doInstall();
		} else {
			buttonInstall.setEnabled(true);
		}
	}

	@Override
	public void installCli(ArgumentParser args) throws Exception {
		Path path = Paths.get(args.getOrDefault("dir", () -> Utils.findDefaultInstallDir().toString()));

		if (!Files.exists(path)) {
			throw new FileNotFoundException("Launcher directory not found at " + path);
		}

		String gameVersion = getGameVersion(args);
		LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));

		String profileName = ClientInstaller.install(path, gameVersion, loaderVersion, InstallerProgress.CONSOLE);

		if (args.has("noprofile")) {
			return;
		}

		ProfileInstaller profileInstaller = new ProfileInstaller(path);
		List<ProfileInstaller.LauncherType> types = profileInstaller.getInstalledLauncherTypes();
		ProfileInstaller.LauncherType launcherType = null;

		if (args.has("launcher")) {
			launcherType = ProfileInstaller.LauncherType.valueOf(args.get("launcher").toUpperCase(Locale.ROOT));
		}

		if (launcherType == null) {
			if (types.size() == 0) {
				throw new FileNotFoundException("Could not find a valid launcher profile .json");
			} else if (types.size() == 1) {
				// Only 1 launcher type found, install to that.
				launcherType = types.get(0);
			} else {
				throw new FileNotFoundException("Multiple launcher installations were found, please specify the target launcher using -launcher");
			}
		}

		profileInstaller.setupProfile(profileName, gameVersion, launcherType);
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir> -mcversion <minecraft version, default latest> -loader <loader version, default latest> -launcher [win32, microsoft_store]";
	}

	@Override
	public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
		addRow(pane, c, null,
				createProfile = new JCheckBox(Utils.BUNDLE.getString("option.create.profile"), true));

		installLocation.setText(Utils.findDefaultInstallDir().toString());
	}
}

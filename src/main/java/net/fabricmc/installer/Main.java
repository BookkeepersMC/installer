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

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.installer.client.ClientHandler;
import net.fabricmc.installer.server.ServerHandler;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.CrashDialog;
import net.fabricmc.installer.util.NotebookService;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.OperatingSystem;

public class Main {
	public static MetaHandler GAME_VERSION_META;
	public static MetaHandler LOADER_META;

	public static final List<Handler> HANDLERS = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
			// Use the operating system cert store
			System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
		}

		System.out.println("Loading Fabric Installer: " + Main.class.getPackage().getImplementationVersion());

		HANDLERS.add(new ClientHandler());
		HANDLERS.add(new ServerHandler());

		ArgumentParser argumentParser = ArgumentParser.create(args);
		String command = argumentParser.getCommand().orElse(null);

		//Can be used if you wish to re-host or provide custom versions. Ensure you include the trailing /
		String metaUrl = argumentParser.has("metaurl") ? argumentParser.get("metaurl") : null;
		String mavenUrl = argumentParser.has("mavenurl") ? argumentParser.get("mavenurl") : null;

		if (metaUrl != null || mavenUrl != null) {
			NotebookService.setFixed(metaUrl, mavenUrl);
		}

		GAME_VERSION_META = new MetaHandler("game.json");
		LOADER_META = new MetaHandler("loader.json");

		//Default to the help command in a headless environment
		if (GraphicsEnvironment.isHeadless() && command == null) {
			command = "help";
		}

		if (command == null) {
			try {
				InstallerGui.start();
			} catch (Exception e) {
				e.printStackTrace();
				new CrashDialog(e);
			}
		} else if (command.equals("help")) {
			System.out.println("help - Opens this menu");
			HANDLERS.forEach(handler -> System.out.printf("%s %s\n", handler.name().toLowerCase(), handler.cliHelp()));
			loadMetadata();

			System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n", GAME_VERSION_META.getLatestVersion(argumentParser.has("snapshot")).getVersion(), Main.LOADER_META.getLatestVersion(false).getVersion());
		} else {
			loadMetadata();

			for (Handler handler : HANDLERS) {
				if (command.equalsIgnoreCase(handler.name())) {
					try {
						handler.installCli(argumentParser);
					} catch (Exception e) {
						throw new RuntimeException("Failed to install " + handler.name(), e);
					}

					return;
				}
			}

			//Only reached if a handler is not found
			System.out.println("No handler found for " + args[0] + " see help");
		}
	}

	public static void loadMetadata() {
		try {
			LOADER_META.load();
			GAME_VERSION_META.load();
		} catch (Throwable t) {
			throw new RuntimeException("Unable to load metadata", t);
		}
	}
}

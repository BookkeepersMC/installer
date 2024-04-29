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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import mjson.Json;

import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.util.NotebookService;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Library;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ClientInstaller {
	public static String install(Path mcDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion.name);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (!Files.exists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		Path profileJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(profileJar);

		Json json = NotebookService.queryMetaJson(String.format("download/%s/notebook-loader-%s-%s.json", loaderVersion.name, loaderVersion.name, gameVersion));
		Files.write(profileJson, json.toString().getBytes(StandardCharsets.UTF_8));

		/*
		Downloading the libraries isn't strictly necessary as the launcher will do it for us.
		Do it anyway in case the launcher fails, we know we have a working connection to maven here.
		 */
		Path libsDir = mcDir.resolve("libraries");

		for (Json libraryJson : json.at("libraries").asJsonList()) {
			Library library = new Library(libraryJson);
			Path libraryFile = libsDir.resolve(library.getPath());
			String url = library.getURL();

			//System.out.println("Downloading "+url+" to "+libraryFile);
			progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(new Object[]{library.name}));
			NotebookService.downloadSubstitutedMaven(url, libraryFile);
		}

		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}

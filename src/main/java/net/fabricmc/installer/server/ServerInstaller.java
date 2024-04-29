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

package net.fabricmc.installer.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import mjson.Json;

import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.util.NotebookService;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Library;
import net.fabricmc.installer.util.Utils;

public class ServerInstaller {
	private static final String servicesDir = "META-INF/services/";
	private static final String manifestPath = "META-INF/MANIFEST.MF";
	public static final String DEFAULT_LAUNCH_JAR_NAME = "notebook-server-launch.jar";
	private static final Pattern SIGNATURE_FILE_PATTERN = Pattern.compile("META-INF/[^/]+\\.(SF|DSA|RSA|EC)");

	public static void install(Path dir, LoaderVersion loaderVersion, String gameVersion, InstallerProgress progress) throws IOException {
		Path launchJar = dir.resolve(DEFAULT_LAUNCH_JAR_NAME);
		install(dir, loaderVersion, gameVersion, progress, launchJar);
	}

	public static void install(Path dir, LoaderVersion loaderVersion, String gameVersion, InstallerProgress progress, Path launchJar) throws IOException {
		progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing.server")).format(new Object[]{String.format("%s(%s)", loaderVersion.name, gameVersion)}));

		Files.createDirectories(dir);

		Path libsDir = dir.resolve("libraries");
		Files.createDirectories(libsDir);

		progress.updateProgress(Utils.BUNDLE.getString("progress.download.libraries"));

		List<Library> libraries = new ArrayList<>();
		String mainClassMeta;

		if (loaderVersion.path == null) { // loader jar unavailable, grab everything from meta
			Json json = NotebookService.queryMetaJson(String.format("download/%s/notebook-loader-%s-%s-server.json", loaderVersion.name, loaderVersion.name, gameVersion));

			for (Json libraryJson : json.at("libraries").asJsonList()) {
				libraries.add(new Library(libraryJson));
			}

			mainClassMeta = json.at("mainClass").asString();
		} else { // loader jar available, generate library list from it
			libraries.add(new Library(String.format("com.bookkeepersmc:notebook-loader:%s", loaderVersion.name), null, loaderVersion.path));
			libraries.add(new Library(String.format("net.fabricmc:intermediary:%s", gameVersion), "https://maven.fabricmc.net/", null));

			try (ZipFile zf = new ZipFile(loaderVersion.path.toFile())) {
				ZipEntry entry = zf.getEntry("notebook-installer.json");
				Json json = Json.read(Utils.readString(zf.getInputStream(entry)));
				Json librariesElem = json.at("libraries");

				for (Json libraryJson : librariesElem.at("common").asJsonList()) {
					libraries.add(new Library(libraryJson));
				}

				for (Json libraryJson : librariesElem.at("server").asJsonList()) {
					libraries.add(new Library(libraryJson));
				}

				mainClassMeta = json.at("mainClass").at("server").asString();
			}
		}

		String mainClassManifest = "net.fabricmc.loader.launch.server.FabricServerLauncher";
		List<Path> libraryFiles = new ArrayList<>();

		for (Library library : libraries) {
			Path libraryFile = libsDir.resolve(library.getPath());

			if (library.inputPath == null) {
				progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(new Object[]{library.name}));
				NotebookService.downloadSubstitutedMaven(library.getURL(), libraryFile);
			} else {
				Files.createDirectories(libraryFile.getParent());
				Files.copy(library.inputPath, libraryFile, StandardCopyOption.REPLACE_EXISTING);
			}

			libraryFiles.add(libraryFile);

			if (library.name.matches("net\\.fabricmc:fabric-loader:.*")) {
				try (JarFile jarFile = new JarFile(libraryFile.toFile())) {
					Manifest manifest = jarFile.getManifest();
					mainClassManifest = manifest.getMainAttributes().getValue("Main-Class");
				}
			}
		}

		progress.updateProgress(Utils.BUNDLE.getString("progress.generating.launch.jar"));

		boolean shadeLibraries = Utils.compareVersions(loaderVersion.name, "0.12.5") <= 0; // FabricServerLauncher in Fabric Loader 0.12.5 and earlier requires shading the libs into the launch jar
		makeLaunchJar(launchJar, mainClassMeta, mainClassManifest, libraryFiles, shadeLibraries, progress);
	}

	private static void makeLaunchJar(Path file, String launchMainClass, String jarMainClass, List<Path> libraryFiles,
			boolean shadeLibraries, InstallerProgress progress) throws IOException {
		Files.deleteIfExists(file);

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(file))) {
			Set<String> addedEntries = new HashSet<>();

			addedEntries.add(manifestPath);
			zipOutputStream.putNextEntry(new ZipEntry(manifestPath));

			Manifest manifest = new Manifest();
			Attributes mainAttributes = manifest.getMainAttributes();

			mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			mainAttributes.put(Attributes.Name.MAIN_CLASS, jarMainClass);

			if (!shadeLibraries) {
				mainAttributes.put(Attributes.Name.CLASS_PATH, libraryFiles.stream()
						.map(f -> file.getParent().relativize(f).normalize().toString().replace("\\", "/"))
						.collect(Collectors.joining(" ")));
			}

			manifest.write(zipOutputStream);

			zipOutputStream.closeEntry();

			addedEntries.add("notebook-server-launch.properties");
			zipOutputStream.putNextEntry(new ZipEntry("notebook-server-launch.properties"));
			zipOutputStream.write(("launch.mainClass=" + launchMainClass + "\n").getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();

			if (shadeLibraries) {
				Map<String, Set<String>> services = new HashMap<>();
				byte[] buffer = new byte[32768];

				for (Path f : libraryFiles) {
					progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.generating.launch.jar.library")).format(new Object[]{f.getFileName().toString()}));

					// read service definitions (merging them), copy other files
					try (JarInputStream jis = new JarInputStream(Files.newInputStream(f))) {
						JarEntry entry;

						while ((entry = jis.getNextJarEntry()) != null) {
							if (entry.isDirectory()) continue;

							String name = entry.getName();

							if (name.startsWith(servicesDir) && name.indexOf('/', servicesDir.length()) < 0) { // service definition file
								parseServiceDefinition(name, jis, services);
							} else if (SIGNATURE_FILE_PATTERN.matcher(name).matches()) {
								// signature file, ignore
							} else if (!addedEntries.add(name)) {
								System.out.printf("duplicate file: %s%n", name);
							} else {
								JarEntry newEntry = new JarEntry(name);
								zipOutputStream.putNextEntry(newEntry);

								int r;

								while ((r = jis.read(buffer, 0, buffer.length)) >= 0) {
									zipOutputStream.write(buffer, 0, r);
								}

								zipOutputStream.closeEntry();
							}
						}
					}
				}

				// write service definitions
				for (Map.Entry<String, Set<String>> entry : services.entrySet()) {
					JarEntry newEntry = new JarEntry(entry.getKey());
					zipOutputStream.putNextEntry(newEntry);

					writeServiceDefinition(entry.getValue(), zipOutputStream);

					zipOutputStream.closeEntry();
				}
			}
		}
	}

	private static void parseServiceDefinition(String name, InputStream rawIs, Map<String, Set<String>> services) throws IOException {
		Collection<String> out = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(rawIs, StandardCharsets.UTF_8));
		String line;

		while ((line = reader.readLine()) != null) {
			int pos = line.indexOf('#');
			if (pos >= 0) line = line.substring(0, pos);
			line = line.trim();

			if (!line.isEmpty()) {
				if (out == null) out = services.computeIfAbsent(name, ignore -> new LinkedHashSet<>());

				out.add(line);
			}
		}
	}

	private static void writeServiceDefinition(Collection<String> defs, OutputStream os) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

		for (String def : defs) {
			writer.write(def);
			writer.write('\n');
		}

		writer.flush();
	}
}

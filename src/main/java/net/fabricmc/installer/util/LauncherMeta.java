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

package net.fabricmc.installer.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import mjson.Json;

public class LauncherMeta {
	private static LauncherMeta launcherMeta = null;

	public static LauncherMeta getLauncherMeta() throws IOException {
		if (launcherMeta == null) {
			launcherMeta = load();
		}

		return launcherMeta;
	}

	private static LauncherMeta load() throws IOException {
		List<Version> versions = new ArrayList<>();
		versions.addAll(getVersionsFromUrl(Reference.MINECRAFT_LAUNCHER_MANIFEST));
		versions.addAll(getVersionsFromUrl(Reference.EXPERIMENTAL_LAUNCHER_MANIFEST));

		return new LauncherMeta(versions);
	}

	private static List<Version> getVersionsFromUrl(String url) throws IOException {
		Json json = NotebookService.queryJsonSubstitutedMaven(url);

		List<Version> versions = json.at("versions").asJsonList()
				.stream()
				.map(Version::new)
				.collect(Collectors.toList());

		return versions;
	}

	public final List<Version> versions;

	public LauncherMeta(List<Version> versions) {
		this.versions = versions;
	}

	public static class Version {
		public final String id;
		public final String url;

		private VersionMeta versionMeta = null;

		public Version(Json json) {
			this.id = json.at("id").asString();
			this.url = json.at("url").asString();
		}

		public VersionMeta getVersionMeta() throws IOException {
			if (versionMeta == null) {
				Json json = NotebookService.queryJsonSubstitutedMaven(url);
				versionMeta = new VersionMeta(json);
			}

			return versionMeta;
		}
	}

	public Version getVersion(String version) {
		return versions.stream().filter(v -> v.id.equals(version)).findFirst().orElse(null);
	}
}

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import mjson.Json;

import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ProfileInstaller {
	private final Path mcDir;

	public ProfileInstaller(Path mcDir) {
		this.mcDir = mcDir;
	}

	public List<LauncherType> getInstalledLauncherTypes() {
		return Arrays.stream(LauncherType.values())
				.filter(launcherType -> Files.exists(mcDir.resolve(launcherType.profileJsonName)))
				.collect(Collectors.toList());
	}

	public void setupProfile(String name, String gameVersion, LauncherType launcherType) throws IOException {
		Path launcherProfiles = mcDir.resolve(launcherType.profileJsonName);

		if (!Files.exists(launcherProfiles)) {
			throw new FileNotFoundException("Could not find " + launcherType.profileJsonName);
		}

		System.out.println("Creating profile");

		Json jsonObject = Json.read(Utils.readString(launcherProfiles));

		Json profiles = jsonObject.at("profiles");

		if (profiles == null) {
			profiles = Json.object();
			jsonObject.set("profiles", profiles);
		}

		String profileName = Reference.LOADER_NAME + "-" + gameVersion;

		Json profile = profiles.at(profileName);

		if (profile == null) {
			profile = createProfile(profileName);
			profiles.set(profileName, profile);
		}

		profile.set("lastVersionId", name);

		Utils.writeToFile(launcherProfiles, jsonObject.toString());

		// Create the mods directory
		Path modsDir = mcDir.resolve("mods");

		if (Files.notExists(modsDir)) {
			Files.createDirectories(modsDir);
		}
	}

	private static Json createProfile(String name) {
		Json jsonObject = Json.object();
		jsonObject.set("name", name);
		jsonObject.set("type", "custom");
		jsonObject.set("created", Utils.ISO_8601.format(new Date()));
		jsonObject.set("lastUsed", Utils.ISO_8601.format(new Date()));
		jsonObject.set("icon", Utils.getProfileIcon());
		return jsonObject;
	}

	public enum LauncherType {
		WIN32("launcher_profiles.json"),
		MICROSOFT_STORE("launcher_profiles_microsoft_store.json");

		public final String profileJsonName;

		LauncherType(String profileJsonName) {
			this.profileJsonName = profileJsonName;
		}
	}
}

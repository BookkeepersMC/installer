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

public class Reference {
	public static final String LOADER_NAME = "notebook-loader";

	public static final String FABRIC_API_URL = "https://www.curseforge.com/minecraft/mc-mods/notebook-api/";
	public static final String SERVER_LAUNCHER_URL = "https://fabricmc.net/use/server/";
	public static final String MINECRAFT_LAUNCHER_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
	public static final String EXPERIMENTAL_LAUNCHER_MANIFEST = "https://maven.fabricmc.net/net/minecraft/experimental_versions.json";

	static final String DEFAULT_META_SERVER = "https://raw.githubusercontent.com/BookkeepersMC/notebook-loader/master/";
	static final String NOTEBOOK_OTHER_META = "https://raw.githubusercontent.com/BookkeepersMC/meta/master/";
	static final String JSON_PROVIDER = "https://github.com/BookkeepersMC/notebook-loader/releases/";
	static final String JSON_PROVIDER_2 = "https://github.com/BookkeepersMC/meta/releases/";
	static final String FABRIC_MAVEN = "https://maven.fabricmc.net/";
	static final String NOTEBOOK_MAVEN = "https://bookkeepersmc.github.io/m2/";

	static final NotebookService[] FABRIC_SERVICES = {
			new NotebookService(JSON_PROVIDER_2, JSON_PROVIDER_2),
			new NotebookService(DEFAULT_META_SERVER, FABRIC_MAVEN),
			new NotebookService(DEFAULT_META_SERVER, NOTEBOOK_MAVEN),
			new NotebookService(NOTEBOOK_OTHER_META, NOTEBOOK_OTHER_META),
			new NotebookService(JSON_PROVIDER, JSON_PROVIDER),
			new NotebookService("https://meta2.fabricmc.net/", "https://maven2.fabricmc.net/"),
			new NotebookService("https://meta3.fabricmc.net/", "https://maven3.fabricmc.net/")
	};
}

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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;
import net.fabricmc.installer.util.VersionMeta;

public class MinecraftServerDownloader {
	private final String gameVersion;

	public MinecraftServerDownloader(String gameVersion) {
		this.gameVersion = gameVersion;
	}

	public void downloadMinecraftServer(Path serverJar) throws IOException {
		if (isServerJarValid(serverJar)) {
			System.out.println("Existing server jar valid, not downloading");
			return;
		}

		Path serverJarTmp = serverJar.resolveSibling(serverJar.getFileName().toString() + ".tmp");
		Files.deleteIfExists(serverJar);
		Utils.downloadFile(new URL(getServerDownload().url), serverJarTmp);

		if (!isServerJarValid(serverJarTmp)) {
			throw new IOException("Failed to validate downloaded server jar");
		}

		Files.move(serverJarTmp, serverJar, StandardCopyOption.REPLACE_EXISTING);
	}

	private boolean isServerJarValid(Path serverJar) throws IOException {
		if (!Files.exists(serverJar)) {
			return false;
		}

		return Utils.sha1String(serverJar).equalsIgnoreCase(getServerDownload().sha1);
	}

	private VersionMeta getVersionMeta() throws IOException {
		LauncherMeta.Version version = LauncherMeta.getLauncherMeta().getVersion(gameVersion);

		if (version == null) {
			throw new RuntimeException("Failed to find version info for minecraft " + gameVersion);
		}

		return version.getVersionMeta();
	}

	private VersionMeta.Download getServerDownload() throws IOException {
		return getVersionMeta().downloads.get("server");
	}
}

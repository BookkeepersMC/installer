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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

import mjson.Json;

import net.fabricmc.installer.util.Utils;

public final class LoaderVersion {
	public final String name;
	public final Path path;

	public LoaderVersion(String name) {
		this.name = name;
		this.path = null;
	}

	public LoaderVersion(Path path) throws IOException {
		try (ZipFile zf = new ZipFile(path.toFile())) {
			ZipEntry entry = zf.getEntry("fabric.mod.json");
			if (entry == null) throw new FileNotFoundException("fabric.mod.json");

			String modJsonContent;

			try (InputStream is = zf.getInputStream(entry)) {
				modJsonContent = Utils.readString(is);
			}

			this.name = Json.read(modJsonContent).at("version").asString();
		} catch (ZipError e) {
			throw new IOException(e);
		}

		this.path = path;
	}
}

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
import java.net.URL;
import java.nio.file.Path;

import mjson.Json;

public final class NotebookService {
	private static int activeIndex = 0; // index into INSTANCES or -1 if set to a fixed service
	private static NotebookService fixedService;

	private final String meta;
	private final String maven;

	/**
	 * Query Fabric Meta path and decode as JSON.
	 */
	public static Json queryMetaJson(String path) throws IOException {
		return invokeWithFallbacks((service, arg) -> Json.read(Utils.readString(new URL(service.meta + arg))), path);
	}

	/**
	 * Query and decode JSON from url, substituting Fabric Maven with fallbacks or overrides.
	 */
	public static Json queryJsonSubstitutedMaven(String url) throws IOException {
		if (!url.startsWith(Reference.FABRIC_MAVEN)) {
			return Json.read(Utils.readString(new URL(url)));
		}

		String path = url.substring(Reference.FABRIC_MAVEN.length());

		return invokeWithFallbacks((service, arg) -> Json.read(Utils.readString(new URL(service.maven + arg))), path);
	}

	/**
	 * Download url to file, substituting Fabric Maven with fallbacks or overrides.
	 */
	public static void downloadSubstitutedMaven(String url, Path out) throws IOException {
		if (!url.startsWith(Reference.FABRIC_MAVEN)) {
			Utils.downloadFile(new URL(url), out);
			return;
		}

		String path = url.substring(Reference.FABRIC_MAVEN.length());

		invokeWithFallbacks((service, arg) -> {
			Utils.downloadFile(new URL(service.maven + arg), out);
			return null;
		}, path);
	}

	private static <A, R> R invokeWithFallbacks(Handler<A, R> handler, A arg) throws IOException {
		if (fixedService != null) return handler.apply(fixedService, arg);

		int index = activeIndex;
		IOException exc = null;

		do {
			NotebookService service = Reference.FABRIC_SERVICES[index];

			try {
				R ret = handler.apply(service, arg);
				activeIndex = index;

				return ret;
			} catch (IOException e) {
				System.out.println("service "+service+" failed: "+e);

				if (exc == null) {
					exc = e;
				} else {
					exc.addSuppressed(e);
				}
			}

			index = (index + 1) % Reference.FABRIC_SERVICES.length;
		} while (index != activeIndex);

		throw exc;
	}

	private interface Handler<A, R> {
		R apply(NotebookService service, A arg) throws IOException;
	}

	/**
	 * Configure fixed service urls, disabling fallbacks or the defaults.
	 */
	public static void setFixed(String metaUrl, String mavenUrl) {
		if (metaUrl == null && mavenUrl == null) throw new NullPointerException("both meta and maven are null");

		if (metaUrl == null) metaUrl = Reference.DEFAULT_META_SERVER;
		if (mavenUrl == null) mavenUrl = Reference.FABRIC_MAVEN;

		activeIndex = -1;
		fixedService = new NotebookService(metaUrl, mavenUrl);
	}

	NotebookService(String meta, String maven) {
		this.meta = meta;
		this.maven = maven;
	}

	public String getMetaUrl() {
		return meta;
	}

	public String getMavenUrl() {
		return maven;
	}

	@Override
	public String toString() {
		return "FabricService{"
				+ "meta='" + meta + '\''
				+ ", maven='" + maven + "'}";
	}
}

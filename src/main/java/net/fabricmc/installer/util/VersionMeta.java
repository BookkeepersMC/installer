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

import java.util.HashMap;
import java.util.Map;

import mjson.Json;

public class VersionMeta {
	public final String id;
	public final Map<String, Download> downloads;

	public VersionMeta(Json json) {
		id = json.at("id").asString();
		downloads = new HashMap<>();

		for (Map.Entry<String, Json> entry : json.at("downloads").asJsonMap().entrySet()) {
			downloads.put(entry.getKey(), new Download(entry.getValue()));
		}
	}

	public static class Download {
		public final String sha1;
		public final long size;
		public final String url;

		public Download(Json json) {
			sha1 = json.at("sha1").asString();
			size = json.at("size").asLong();
			url = json.at("url").asString();
		}
	}
}

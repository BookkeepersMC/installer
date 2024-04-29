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

package net.fabricmc.installer.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import net.fabricmc.installer.util.OperatingSystem;

public class NativesHelper {
	private static final String OS_ID = OperatingSystem.CURRENT.name().toLowerCase(Locale.ROOT) + "-" + System.getProperty("os.arch").toLowerCase(Locale.ROOT);
	private static final Map<String, String> NATIVES_MAP = getNativesMap();

	private static boolean loaded = false;

	static {
		System.out.println("OS_ID: " + OS_ID);
	}

	private static Map<String, String> getNativesMap() {
		Map<String, String> natives = new HashMap<>();

		natives.put("windows-aarch64", "natives/windows-ARM64.dll");
		natives.put("windows-win32", "natives/windows-Win32.dll");
		natives.put("windows-amd64", "natives/windows-x64.dll");

		natives.put("macos-x86_64", "natives/macos-x86_64_arm64.dylib");
		natives.put("macos-aarch64", "natives/macos-x86_64_arm64.dylib");

		return natives;
	}

	public static boolean loadSafelyIfCompatible() {
		if (isCompatible()) {
			try {
				loadNatives();
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	private static boolean isCompatible() {
		return NATIVES_MAP.containsKey(OS_ID);
	}

	private static void loadNatives() throws IOException {
		if (loaded) {
			return;
		}

		String nativeName = NATIVES_MAP.get(OS_ID);
		Path nativePath = Files.createTempFile("fabric-installer-native", null);

		try (InputStream is = NativesHelper.class.getClassLoader().getResourceAsStream(nativeName)) {
			Objects.requireNonNull(is, "Could not load: " + nativeName);
			Files.copy(is, nativePath, StandardCopyOption.REPLACE_EXISTING);
		}

		System.load(nativePath.toString());

		loaded = true;
	}
}

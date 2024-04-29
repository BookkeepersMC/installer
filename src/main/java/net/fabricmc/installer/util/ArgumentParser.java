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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ArgumentParser {
	private String[] args;
	private Map<String, String> argMap;
	//The command will be the first argument passed, and if it doesnt start with -
	private String command = null;

	private ArgumentParser(String[] args) {
		this.args = args;
		parse();
	}

	public String get(String argument) {
		if (!argMap.containsKey(argument)) {
			throw new IllegalArgumentException(String.format("Could not find %s in the arguments", argument));
		}

		String arg = argMap.get(argument);

		if (arg == null) {
			throw new IllegalArgumentException(String.format("Could not value for %s", argument));
		}

		return arg;
	}

	public String getOrDefault(String argument, Supplier<String> stringSuppler) {
		if (!argMap.containsKey(argument)) {
			return stringSuppler.get();
		}

		return argMap.get(argument);
	}

	public boolean has(String argument) {
		return argMap.containsKey(argument);
	}

	public void ifPresent(String argument, Consumer<String> consumer) {
		if (has(argument)) {
			consumer.accept(get(argument));
		}
	}

	public Optional<String> getCommand() {
		return command == null ? Optional.empty() : Optional.of(command);
	}

	private void parse() {
		argMap = new HashMap<>();

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				String key = args[i].substring(1);
				String value = null;

				if (i + 1 < args.length) {
					value = args[i + 1];

					if (value.startsWith("-")) {
						argMap.put(key, "");
						continue;
					}

					i++;
				}

				if (argMap.containsKey(key)) {
					throw new IllegalArgumentException(String.format("Argument %s already passed", key));
				}

				argMap.put(key, value);
			} else if (i == 0) {
				command = args[i];
			}
		}
	}

	public static ArgumentParser create(String[] args) {
		return new ArgumentParser(args);
	}
}

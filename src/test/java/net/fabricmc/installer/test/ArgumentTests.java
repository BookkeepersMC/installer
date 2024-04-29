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

package net.fabricmc.installer.test;

import org.junit.Assert;
import org.junit.Test;

import net.fabricmc.installer.util.ArgumentParser;

public class ArgumentTests {
	@Test
	public void test() {
		String[] args = new String[]{"command", "-arg1", "Hello", "-arg2", "-anotherArg", "123"};
		ArgumentParser handler = ArgumentParser.create(args);

		Assert.assertTrue(handler.has("arg1"));
		Assert.assertEquals(handler.get("arg1"), "Hello");

		Assert.assertEquals(handler.getOrDefault("arg3", () -> "World"), "World");

		Assert.assertTrue(handler.has("arg2"));
		Assert.assertFalse(handler.has("arg3"));

		Assert.assertEquals(handler.getCommand().get(), "command");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArgs() {
		ArgumentParser.create(new String[]{"-arg1", "Hello", "-arg1"});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownArg() {
		ArgumentParser.create(new String[]{"-arg1", "Hello"}).get("arg2");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullArg() {
		ArgumentParser.create(new String[]{"-arg1"}).get("arg1");
	}

	@Test
	public void testCommands() {
		Assert.assertTrue(ArgumentParser.create(new String[]{"command", "-arg1", "Hello"}).getCommand().isPresent());
		Assert.assertEquals(ArgumentParser.create(new String[]{"command", "-arg1", "Hello"}).getCommand().get(), "command");

		Assert.assertFalse(ArgumentParser.create(new String[]{"-arg1", "Hello"}).getCommand().isPresent());
	}
}

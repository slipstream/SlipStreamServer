package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VersionCounterTest {

	private static final int numberOfThreads = 30;

	@Test
	public void increment() {
		int counter = VersionCounter.getNextVersion();

		assertThat(VersionCounter.getNextVersion(), is(counter+1));
		assertThat(VersionCounter.getNextVersion(), is(counter+2));
	}

	@Test
	public void testParallelIncrement() throws InterruptedException {
		List<Thread> threads = new ArrayList<>();

		int initialVersion = VersionCounter.getNextVersion();
		int finalVersion = initialVersion + numberOfThreads + 1;

		for (int i = 1; i <= numberOfThreads; i++) {
			Thread thread = new Thread(new IncrementVersionRunnable());
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		assertThat(VersionCounter.getNextVersion(), is(finalVersion));
	}

	public class IncrementVersionRunnable implements Runnable {

		@Override
		public void run() {
			VersionCounter.getNextVersion();
		}

	}
}

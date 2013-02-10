/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jrtc27.portallink;

public class LinkEntry {
	private final String targetWorldName;
	private final int whichNether;

	public LinkEntry(final String targetWorldName, final int whichNether) {
		this.targetWorldName = targetWorldName;
		this.whichNether = whichNether;
	}

	public String getTargetWorldName() {
		return this.targetWorldName;
	}

	public int getWhichNether() {
		return this.whichNether;
	}

}

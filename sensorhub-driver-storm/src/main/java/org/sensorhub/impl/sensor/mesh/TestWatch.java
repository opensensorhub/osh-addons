/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mesh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import org.sensorhub.impl.utils.grid.DirectoryWatcher;
import org.sensorhub.impl.utils.grid.FileListener;

public class TestWatch implements FileListener
{

	private DirectoryWatcher watcher;
	private Thread watcherThread;

	public TestWatch() {
		try {
			Path path = Paths.get("C:/Data/sensorhub/delta/MESH");
			watcher = new DirectoryWatcher(path, StandardWatchEventKinds.ENTRY_CREATE);
			watcherThread = new Thread(watcher);
			watcher.addListener(this);
			watcherThread.start();
			System.err.println("****** past run");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	} 

	@Override
	public void newFile(Path p) throws IOException {
		String fn = p.getFileName().toString().toLowerCase();
		if(!fn.contains("mesh") || !fn.endsWith(".grb2")) {
			System.err.println("*** Ignore: " + fn);
			return;
		}
		File sourceFile = p.toFile();
		while(!sourceFile.renameTo(sourceFile)) {
			// Cannot read from file, windows still working on it.
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.err.println("*** Read: " + fn);
		MeshReader reader = new MeshReader(p.toString());
		MeshRecord rec = reader.readMesh();
		if(rec == null) {
			throw new IOException("MeshReader returned null mesh record");
		}
		System.err.println("*** Send: " + rec.timeUtc);
	}
	public static void main(String[] args) throws Exception {
		TestWatch test = new TestWatch();
	}
}

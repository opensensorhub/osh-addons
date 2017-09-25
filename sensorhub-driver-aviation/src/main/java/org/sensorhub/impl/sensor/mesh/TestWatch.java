package org.sensorhub.impl.sensor.mesh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

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
		MeshRecord rec = reader.createMeshRecord();
		if(rec == null) {
			throw new IOException("MeshReader returned null mesh record");
		}
		System.err.println("*** Send: " + rec.timeUtc);
	}
	public static void main(String[] args) throws Exception {
		TestWatch test = new TestWatch();
	}
}

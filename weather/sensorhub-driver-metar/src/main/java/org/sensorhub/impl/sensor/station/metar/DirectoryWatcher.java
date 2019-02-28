package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: DirectoryWatcher.java</p>
 * <p>Description: Simple way to monitor a directory for changes.</p>
 *
 * @author Tony Cook
 * @date Jul 22, 2014
 * 
 */
public class DirectoryWatcher implements Runnable
{
	List<FileListener> listeners = new ArrayList<>();
	WatchService watcher;
	Path path;
	
	public DirectoryWatcher(Path path, Kind<?> ... eventKinds) throws IOException {
		watcher = path.getFileSystem().newWatchService();
		path.register(watcher, eventKinds);
		this.path = path;
	}
	
	public boolean addListener(FileListener f) {
		return listeners.add(f);
	}
	
	public boolean removeListener(FileListener f) {
		return listeners.remove(f);
	}
	
	@Override
	public void run()  { //, InterruptedException {
		while (true) {
			WatchKey watchKey;
			try {
				watchKey = watcher.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			} 
			List<WatchEvent<?>> events = watchKey.pollEvents();

			for (WatchEvent<?> event : events) {
				//				System.out.println(event.kind() + " : " + event.context());
				WatchEvent.Kind<?> kind = event.kind();

				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();

				System.out.println(kind.name() + ": " + filename);

				if (kind == StandardWatchEventKinds.ENTRY_CREATE ) {
					System.out.println("Created: " + filename);
					for(FileListener l: listeners) {
						try {
							l.newFile(Paths.get(path.toString(), filename.toString()));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}			
			} 

			if(!watchKey.reset()) {
				System.err.println("Now what?");
				break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
//		DirectoryWatcher watcher = new DirectoryWatcher(Paths.get("C:/Data/tmp"),StandardWatchEventKinds.ENTRY_CREATE);
		DirectoryWatcher watcher = new DirectoryWatcher(Paths.get(args[0]),StandardWatchEventKinds.ENTRY_CREATE);

		watcher.run();
	}

}

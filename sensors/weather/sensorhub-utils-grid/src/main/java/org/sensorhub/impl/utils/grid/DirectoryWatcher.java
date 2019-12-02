/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.utils.grid;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Utility class to monitor a directory for changes.
 * </p>
 *
 * @author Tony Cook
 */
public class DirectoryWatcher implements Runnable
{
	Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);
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
	public void run()  {
	    Thread.currentThread().setName("DirWatcher");
	    WatchKey watchKey = null;
	    
		while (!Thread.currentThread().isInterrupted()) {
			try
            {
                try {
                	watchKey = watcher.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent<?> event : events) {
//				    System.out.println(event.kind() + " : " + event.context());
                	WatchEvent.Kind<?> kind = event.kind();

                	@SuppressWarnings("unchecked")
                	WatchEvent<Path> ev = (WatchEvent<Path>) event;
                	Path filename = ev.context();

//                	System.out.println(kind.name() + ": " + filename);

//				    if (kind == StandardWatchEventKinds.ENTRY_CREATE ) {
                		for(FileListener l: listeners) {
                			l.newFile(Paths.get(path.toString(), filename.toString()));                			
                		}
//				    }			
                } 
            }
            catch (Throwable e)
            {
                log.error("Error while processing watch events", e);
            }
			finally
			{
                watchKey.reset();
			}
		}
	}

}

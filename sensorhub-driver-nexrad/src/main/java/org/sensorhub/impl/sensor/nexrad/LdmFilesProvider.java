package org.sensorhub.impl.sensor.nexrad;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.PriorityBlockingQueue;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.aws.nexrad.LdmLevel2Reader;
import org.sensorhub.impl.module.AbstractModule;

/**
 * <p>
 * Title: LdmFilesProvider.java
 * </p>
 * <p>
 * Description:
 * </p>
 *
 * @author T
 * @date Mar 29, 2016
 */
public class LdmFilesProvider { // extends AbstractModule<LdmFilesProviderConfig> { //implements ICommProvider<LdmFilesProviderConfig> {

	WatchService watcher;
	PriorityBlockingQueue<String> files;
	InputStream multiFileInputStream;
	boolean started, init;
	private LdmFilesConsumer consumer;
	private Path dataFolder;

	public LdmFilesProvider(String dataFolder) {
		files = new PriorityBlockingQueue<>();
		consumer = new LdmFilesConsumer(files);
		this.dataFolder = Paths.get(dataFolder);
	}

	public void start() throws SensorHubException {
		// register dir watcher
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
			dataFolder.register(watcher, ENTRY_CREATE);
			System.err.println("Provider folder: " + dataFolder.toString());
		} catch (IOException e) {
			throw new SensorHubException(
					"Error while registering watcher on LDM Nexrad files directory", e);
		}

		// start directory watcher thread
		started = true;
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (started) {
					WatchKey key;
					try {
						key = watcher.take();
					} catch (InterruptedException | ClosedWatchServiceException x) {
						return;
					}

					// process each new file event
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == ENTRY_CREATE) // needed because we can also receive OVERFLOW
						{
							// add file to queue
							Path newFile = (Path) event.context();
							String fname = newFile.toString();
							if (fname.endsWith("-I") || fname.endsWith("-S") || fname.endsWith("-E")) {
								files.add(fname);
							}
						}

						key.reset();
					}
				}
			}
		});
		t.start();
		consumer.init();
	}


	public void stop()  {
		started = false;

		try {
			if (watcher != null)
				watcher.close();

			multiFileInputStream.close();
			files.clear();
		} catch (IOException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sensorhub.api.module.IModule#cleanup()
	 */
	public void cleanup() throws SensorHubException {
	}

	public Path nextFile() throws IOException
	{
		try
		{
			//            File nextFile = files.takeFirst();
			String nextFile = consumer.nextFile();
			//			System.err.println("Next data file: " + nextFile);
			return Paths.get(dataFolder.toString(), nextFile);
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}

	public static void main(String[] args) throws SensorHubException, InterruptedException {
		LdmFilesProvider prov = new LdmFilesProvider("C:/Data/sensorhub/Level2/test/KARX");
		prov.start();
		//		Thread.sleep(10000L);
		//		LdmFilesConsumer consumer = new LdmFilesConsumer(prov.files);
		//		consumer.run();
	}

}

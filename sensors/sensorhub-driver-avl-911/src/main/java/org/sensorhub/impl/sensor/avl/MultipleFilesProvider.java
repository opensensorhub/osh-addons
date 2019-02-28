/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.avl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * AVL data provider reading data from multiple .trk files in the same folder. 
 * The folder is also watched so that any new file added during execution is
 * detected and processed.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2015
 */
public class MultipleFilesProvider extends AbstractModule<MultipleFilesProviderConfig> implements ICommProvider<MultipleFilesProviderConfig>
{
    WatchService watcher;
    BlockingQueue<File> files;
    InputStream multiFileInputStream;
    boolean started, init;


    public MultipleFilesProvider()
    {
        this.files = new PriorityBlockingQueue<File>();
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        if (multiFileInputStream == null)
        {        
            // use a filter input stream so we can change behavior on EOF
            // here we keep blocking on read until a new file is added
            multiFileInputStream = new FilterInputStream(nextFile()) {
    
                Thread readThread;
                
                @Override
                public int read() throws IOException
                {
                    initDelay();
                    readThread = Thread.currentThread();
                    
                    int b = super.read();
                    if (b < 0)
                    {
                        this.in = nextFile();
                        if (this.in == null)
                            return -1;
                        return super.read();
                    }
                    
                    return b;
                }
                
                @Override
                public int read(byte[] b, int off, int len) throws IOException
                {
                    initDelay();
                    readThread = Thread.currentThread();
                    
                    int count = super.read(b, off, len);
                    if (count < 0)
                    {
                        this.in = nextFile();
                        if (this.in == null)
                            return -1;
                        return super.read(b, off, len);
                    }
                    
                    return count;
                }

                @Override
                public void close() throws IOException
                {
                    if (in != null)
                    {
                        super.close();
                        readThread.interrupt();
                    }
                }            
            };
        }
        
        return multiFileInputStream;
    }
    
    
    // initial delay so storage has time to start to receive data
    private final void initDelay()
    {
        if (init)
            return;
            
        try
        {
            Thread.sleep(1000);
            init = true;
        }
        catch (InterruptedException e)
        {
        }
    }
    
    
    private FileInputStream nextFile() throws IOException
    {
        try
        {
            File nextFile = files.take();
            AVLDriver.log.debug("Next data file: " + nextFile);
            return new FileInputStream(nextFile);
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return null;
    }


    @Override
    public synchronized void start() throws SensorHubException
    {
        final Path dir = Paths.get(config.dataFolder);

        // scan folder for files (not recursively)
        try
        {
            Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes att) throws IOException
                {
                    if (f.toString().endsWith(".trk"))
                        files.add(f.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error while scanning AVL data directory", e);
        }

        // register dir watcher
        try
        {
            this.watcher = FileSystems.getDefault().newWatchService();
            dir.register(watcher, ENTRY_CREATE);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error while registering watcher on AVL data directory", e);
        }

        // start directory watcher thread
        started = true;
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    // wait for key to be signalled
                    WatchKey key;
                    try
                    {
                        key = watcher.take();
                    }
                    catch (InterruptedException | ClosedWatchServiceException x)
                    {
                        return;
                    }

                    // process each new file event
                    for (WatchEvent<?> event : key.pollEvents())
                    {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == ENTRY_CREATE) // needed because we can also receive OVERFLOW
                        {
                            // add file to queue
                            Path newFile = (Path)event.context();
                            if (newFile.toString().endsWith(".trk"))
                            {
                                AVLDriver.log.debug("New AVL file detected: " + newFile);
                                files.add(dir.resolve(newFile).toFile());
                            }
                        }
                        
                        key.reset();
                    }
                }
            }
        });
        t.start();
    }


    @Override
    public void stop() throws SensorHubException
    {
        started = false;
        
        try
        {
            if (watcher != null)
                watcher.close();
            
            multiFileInputStream.close();
            files.clear();
        }
        catch (IOException e)
        {
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }
}

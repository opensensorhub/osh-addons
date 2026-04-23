/***************************** BEGIN COPYRIGHT BLOCK **************************

Copyright (C) 2025 Delta Air Lines, Inc. All Rights Reserved.

Notice: All information contained herein is, and remains the property of
Delta Air Lines, Inc. The intellectual and technical concepts contained herein
are proprietary to Delta Air Lines, Inc. and may be covered by U.S. and Foreign
Patents, patents in process, and are protected by trade secret or copyright law.
Dissemination, reproduction or modification of this material is strictly
forbidden unless prior written permission is obtained from Delta Air Lines, Inc.

******************************* END COPYRIGHT BLOCK ***************************/

package org.sensorhub.utils.datafiles;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.regex.Pattern;
import org.sensorhub.api.common.SensorHubException;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.google.common.base.Strings;


/**
 * <p>
 * Generic class to get notified when new data files are available.
 * </p><p>
 * Note that this doesn't work out-of-the-box for watching on an NFS mount.
 * In this case, a separate service must be used to subscribe to pub/sub
 * notifications of new file created and touch every new file to generate a local
 * event that this watcher can detect.
 * </p>
 *
 * @author Alex Robin
 * @since Apr 30, 2025
 */
public class DataFileWatcher implements FileFilter, FileListener
{
    final String dataSrcName;
    final String baseDir;
    final Pattern dirNamePattern;
    final Pattern fileNamePattern;
    final String latestPointerFileName;
    final FileListener listener;
    final Logger logger;
    DirectoryWatcher watcher;
    Thread watcherThread;
    
    
    public DataFileWatcher(String dataSrcName, String baseDir, String fileNamePattern, FileListener listener, Logger logger)
    {
        this(dataSrcName, baseDir, null, fileNamePattern, null, listener, logger);
    }
    
    
    public DataFileWatcher(String dataSrcName, String baseDir, String dirNamePattern, String fileNamePattern, FileListener listener, Logger logger)
    {
        this(dataSrcName, baseDir, dirNamePattern, fileNamePattern, null, listener, logger);
    }
    
    
    public DataFileWatcher(String dataSrcName, String baseDir, String dirNamePattern, String fileNamePattern, String latestPointerFileName, FileListener listener, Logger logger)
    {
        this.dataSrcName = Asserts.checkNotNullOrBlank(dataSrcName, "dataSrcName");
        this.baseDir = Asserts.checkNotNullOrBlank(baseDir, "baseDir");
        this.fileNamePattern = Pattern.compile(Asserts.checkNotNullOrBlank(fileNamePattern, "fileNamePattern"));
        this.dirNamePattern = dirNamePattern != null ? Pattern.compile(dirNamePattern) : null;
        this.latestPointerFileName = latestPointerFileName;
        this.listener = listener;
        this.logger = Asserts.checkNotNull(logger, Logger.class);
    }


    public void start() throws SensorHubException
    {
        try
        {
            watcher = new DirectoryWatcher(
                Paths.get(baseDir),
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            watcherThread = new Thread(watcher, "DataFileWatcher-" + this.dataSrcName);
            watcher.addListener(this);
            watcherThread.start();
            
            logger.info("Watching directory {} for {} data updates", baseDir, dataSrcName);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error creating directory watcher on " + baseDir, e);
        }
    }
        
    
    public void stop()
    {
        if (watcherThread != null)
            watcherThread.interrupt();
        watcherThread = null;
        watcher = null;
    }


    public void readLatestDataFile()
    {
        var baseDirFile = new File(baseDir);
        
        var latestFile = readPathOfLatestDataFile(baseDirFile);
        if (latestFile == null)
            latestFile = findLatestDataFile(baseDirFile);
        
        if (latestFile != null)
        {
            // notify listener
            try {
                listener.newFile(latestFile.toPath());
            }
            catch (IOException e) {
                logger.error("Invalid path: {}", latestFile, e);
            }
        }
    }
    
    
    File readPathOfLatestDataFile(File parentDir)
    {
        if (Strings.isNullOrEmpty(latestPointerFileName))
            return null;
        
        var latestPathIndexFile = new File(parentDir, latestPointerFileName);
        if (!latestPathIndexFile.exists() || !latestPathIndexFile.canRead())
        {
            logger.warn("No {} pointer file found", latestPointerFileName);
            return null;
        }
        
        try
        {
            var latestDataFilePath = Files.lines(latestPathIndexFile.toPath())
                .findFirst()
                .orElse(null);
                
            if (latestDataFilePath == null)
                return null;
            
            var latestDataFile = new File(latestDataFilePath);
            if (latestDataFile.isDirectory()) {
                logger.info("Latest data directory is {}", latestDataFilePath);
                return findLatestDataFile(latestDataFile);
            }
            else {
                return latestDataFile;
            }   
        }
        catch (IOException e)
        {
            logger.error("Error reading pointer file {}", latestPointerFileName, e);
            return null;
        }            
    }
    
    
    File findLatestDataFile(File parentDir)
    {
        // list all available data files or directories
        File[] dataFiles = parentDir.listFiles(this);

        // skip if nothing is available
        if (dataFiles.length == 0)
        {
            logger.warn("No {} file available", dataSrcName);
            return null;
        }

        // get the one with latest time stamp
        File latestFile = dataFiles[0];
        for (File f : dataFiles)
        {
            if (f.lastModified() > latestFile.lastModified())
                latestFile = f;
        }
        
        // if it's a directory, look inside
        if (latestFile.isDirectory())
            return findLatestDataFile(latestFile);
        else
            return latestFile; 
    }
    
    
    @Override
    public boolean accept(File f)
    {
        if (f.isFile() && fileNamePattern.matcher(f.getName()).matches())
            return true;
        
        if (f.isDirectory() && dirNamePattern != null && dirNamePattern.matcher(f.getName()).matches())
            return true;
        
        return false;
    }


    @Override
    public void newFile(Path p) throws IOException
    {
        var f = p.toFile();
        
        if (!accept(f))
            return;
        
        if (f.isDirectory())
        {
            f = findLatestDataFile(f);
            if (f != null)
                listener.newFile(f.toPath());
        }
        else
            listener.newFile(p);
    }
}

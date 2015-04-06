/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.vast.physics.TLEParser;


public class TLEUpdater extends TLEParser
{
    int satID;
    
    
    public TLEUpdater(String fetchUrl, int satID)
    {
        super(fetchUrl);
        this.satID = satID;
    }
    

    @Override
    public void reset()
    {
        closeFile();
        
        currentTime = Double.NEGATIVE_INFINITY;
        nextTime = Double.NEGATIVE_INFINITY;
        currentLine1 = "";
        previousLine1 = "";
        nextLine1 = "";
        currentLine2 = "";
        previousLine2 = "";
        nextLine2 = "";
        lineNumber = 0;
        
        // reopen file at beginning!
        try
        {
            URL url = new URL(tleFilePath);
            tleReader = new BufferedReader(new InputStreamReader(url.openStream()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    
    @Override
    protected boolean readNextEntry()
    {
        int lineSatID;
        
        // read until we find an entry for the desired satID
        do
        {
            boolean isLastEntry = super.readNextEntry();
            if (isLastEntry)
                return true;
            
            lineSatID = Integer.parseInt(nextLine1.substring(2, 7));
        }
        while (lineSatID != satID);
        
        return false;
    }
}

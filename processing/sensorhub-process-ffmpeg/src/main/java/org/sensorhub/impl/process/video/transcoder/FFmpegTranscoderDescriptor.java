/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.video.transcoder;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.processing.AbstractProcessProvider;


public class FFmpegTranscoderDescriptor extends AbstractProcessProvider
{
    
    public FFmpegTranscoderDescriptor()
    {
        addImpl(FFMpegTranscoder.INFO);
    }


    @Override
    public String getModuleName()
    {
        return "FFmpeg Transcoder";
    }


    @Override
    public String getModuleDescription()
    {
        return "Collection of FFmpeg Video Processors";
    }

    // TODO Set these two after creating module and module config
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return FFmpegTranscoderProcess.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return FFmpegTranscoderConfig.class;
    }

}

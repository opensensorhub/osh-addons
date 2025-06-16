package org.sensorhub.impl.process.video;

import net.opengis.swe.v20.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.data.AbstractDataInterface;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockCompressed;
import org.vast.data.DataBlockMixed;
import org.vast.process.*;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SimpleProcessImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class FFmpegProcess extends AbstractProcessModule<FFmpegProcessConfig> {
    SimpleProcessImpl process;
    IModule<?> sensorModule;
    IStreamingDataInterface inputVideoOutport;
    AbstractSWEIdentifiable inputVideoInport, outputVideoOutport;
    DataQueue dataQueue, outQueue;
    IStreamingDataInterface videoOutput;
    SMLHelper smlHelper;
    RasterHelper rasterHelper;
    boolean started = false;
    Future<?> execFuture;
    Count width, height;

    public FFmpegProcess() {
        super();
        smlHelper = new SMLHelper();
        process = new SimpleProcessImpl();
        process.setUniqueIdentifier(UUID.randomUUID().toString());
        processDescription = process;

    }

    @Override
    public void doInit() throws SensorException, SensorHubException {

        // Get input module from registry
        if (hasParentHub()) {
            sensorModule = getParentHub().getModuleRegistry().getModuleById(config.videoUID);
        } else {
            throw new SensorException("Parent Hub not set.");
        }

        // Determine type and get video output stream
        if (sensorModule instanceof AbstractSensorModule<?>) {
            var opt = ((AbstractSensorModule) sensorModule).getOutputs().values().stream().filter(output -> {
                var desc = ((IStreamingDataInterface)output).getRecordDescription();
                // True if desc definition is video frame or name is img, videoframe, or video
                return desc != null && ( (desc.getDefinition() != null && desc.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                    || (desc.getName() != null && (desc.getName().equalsIgnoreCase("img") || desc.getName().equalsIgnoreCase("videoframe")
                        || desc.getName().equalsIgnoreCase("video"))));
            }).findFirst();

            if (opt.isPresent()) {
                inputVideoOutport = (IStreamingDataInterface) opt.get();
            } else {
                throw new SensorException("No video stream output found for module " + sensorModule.getName() + ".");
            }
        } else if (sensorModule instanceof AbstractProcessModule<?>) {
            var opt = ((AbstractProcessModule) sensorModule).getOutputs().values().stream().filter(output -> {
                var desc = ((IStreamingDataInterface)output).getRecordDescription();
                // True if desc definition is video frame or name is img, videoframe, or video
                return desc != null && ( (desc.getDefinition() != null && desc.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                        || (desc.getName() != null && (desc.getName().equalsIgnoreCase("img") || desc.getName().equalsIgnoreCase("videoframe")
                        || desc.getName().equalsIgnoreCase("video"))));
            }).findFirst();

            if (opt.isPresent()) {
                inputVideoOutport = (IStreamingDataInterface) opt.get();
            } else {
                throw new SensorException("No video stream output found for module " + sensorModule.getName() + ".");
            }
        } else {
            throw new SensorException("Module type " + sensorModule.getClass().getName() + " not supported.");
        }

        // Create an executable process
        IProcessExec executable;
        try {
            executable = config.execProcess.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new SensorException("Could not instantiate process.", e);
        }

        // Use config to modify inputs, outputs, params, etc. for the process
        initExcProcess(executable);

        // Get input port, create connection
        var opt = executable.getInputList().stream().filter(input -> {
            var desc = ((DataRecord)input);
            // True if desc definition is video frame or name is img, videoframe, or video
            return desc != null && ( (desc.getDefinition() != null && desc.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                    || (desc.getName() != null && (desc.getName().equalsIgnoreCase("img") || desc.getName().equalsIgnoreCase("videoframe")
                    || desc.getName().equalsIgnoreCase("video"))));
        }).findFirst();

        if (opt.isPresent()) {
            inputVideoInport = opt.get();
        } else {
            throw new SensorException("No video stream input found for process " + executable.getInstanceName() + ".");
        }

        var opt2 = executable.getOutputList().stream().filter(output -> {
            var desc = ((DataRecord)output);
            // True if desc definition is video frame or name is img, videoframe, or video
            return desc != null && ( (desc.getDefinition() != null && desc.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                    || (desc.getName() != null && (desc.getName().equalsIgnoreCase("img") || desc.getName().equalsIgnoreCase("videoframe")
                    || desc.getName().equalsIgnoreCase("video"))));
        }).findFirst();

        if (opt2.isPresent()) {
            outputVideoOutport = opt2.get();
        } else {
            throw new SensorException("No video stream output found for process " + executable.getInstanceName() + ".");
        }

        // TODO: Video output never seems to have data. Figure out why this is.
        dataQueue = new DataQueue();
        dataQueue.setSource(null, SMLHelper.getIOComponent(inputVideoOutport.getRecordDescription()));
        dataQueue.setDestination(executable, SMLHelper.getIOComponent(inputVideoInport));

        try {
            executable.connect(SMLHelper.getIOComponent(inputVideoInport), dataQueue);
        } catch (ProcessException e) {
            throw new SensorException("Could not connect to process " + executable.getInstanceName() + ".");
        }

        //addInput();

        height = smlHelper.createCount().value(inputVideoOutport.getRecordDescription().getComponent("img").getComponentCount()).build();
        width = smlHelper.createCount().value(inputVideoOutport.getRecordDescription().getComponent("img").getComponent("row").getComponentCount()).build();
        ((DataArray)SMLHelper.getIOComponent(inputVideoInport).getComponent("img")).setElementCount(height);
        ((DataArray)SMLHelper.getIOComponent(inputVideoInport).getComponent("img").getComponent("row")).setElementCount(width);
        ((DataArray)SMLHelper.getIOComponent(outputVideoOutport).getComponent("img")).setElementCount(height);
        ((DataArray)SMLHelper.getIOComponent(outputVideoOutport).getComponent("img").getComponent("row")).setElementCount(width);

        videoOutput = new VideoDataInterface("inputVideo", width, height, this);

        inputVideoOutport.registerListener(new DataQueuePusher(dataQueue));
        //eventHandler.registerListener(new DataQueuePusher(dataQueue));
        //IEventListener
        //.getParentHub().getEventBus().get;
        //((IDataProducerModule<?>) sensorModule).getOutputs().
        //eventHandler.publish();
        //dataQueue.publishData();
        //ListenerSubscriber
        // TODO: Register listener to push to the DataQueues
        // TODO 1) Register data listener on video sensor, simple process, invoke some method with the data (?)
        // TODO 2) Push the data received into their respective DataQueue (either dataQueue or outQueue)
        outQueue = new DataQueue();
        outQueue.setSource(executable, SMLHelper.getIOComponent(outputVideoOutport));
        outQueue.setDestination(null, SMLHelper.getIOComponent(videoOutput.getRecordDescription()));


        //eventHandler.registerListener(new DataQueuePusher(outQueue));

        try {
            executable.connect(SMLHelper.getIOComponent(outputVideoOutport), outQueue);
        } catch (ProcessException e) {
            throw new SensorException("Could not connect output from process " + executable.getInstanceName() + ".");
        }



        //((VideoDataInterface)videoOutput).setSize(width, height);

        //((DataArrayImpl)imgOut.getComponent("row")).setElementCount(swe.createCount().value(packet.size()).build());
        addOutput(videoOutput);

        try {
            process.setExecutableImpl(executable);
            process.init();
        } catch (Exception e) {
            throw new SensorException("Could not initialize process.", e);
        }

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }

    private <T extends Throwable> void onError(T throwable) {
        //throw new SensorException("Error starting process.", throwable);
    }

    @Override
    public void doStart() throws SensorException {

        try {
            process.start(this::onError);
        } catch (ProcessException e) {
            throw new SensorException("Could not start process.", e);
        }

        started = true;

        execFuture = Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run()
            {
                getLogger().debug("Process thread started");

                try
                {
                    while (started) {
                        outQueue.transferData(true);
                        ((VideoDataInterface)videoOutput).publishData();

                    }
                }
                catch (Throwable e)
                {
                    started = false;
                    getLogger().error("Error during execution", e);
                }

                getLogger().debug("Process thread stopped");
            }
        });
    }

    @Override
    public void doStop() {
        started = false;
        process.stop();

        if (execFuture != null)
        {
            execFuture.cancel(true);
            execFuture = null;
        }
    }

    // TODO: Start using this method to avoid long repeat code
    boolean isVideoData(DataComponent dataDescription) {
        return dataDescription != null && ( (dataDescription.getDefinition() != null && dataDescription.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                || (dataDescription.getName() != null && (dataDescription.getName().equalsIgnoreCase("img") || dataDescription.getName().equalsIgnoreCase("videoframe")
                || dataDescription.getName().equalsIgnoreCase("video"))));
    }



    public abstract void initExcProcess(IProcessExec executable);

    protected class VideoDataInterface extends AbstractSensorOutput<FFmpegProcess> {
        DataRecord dataStruct;
        RasterHelper swe;
        //Count width, height;
        private final Object histogramLock = new Object();
        private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
        private static final int MAX_NUM_TIMING_SAMPLES = 10;

        protected VideoDataInterface(String name, Count width, Count height, FFmpegProcess parent) {
            super(name, parent);
            swe = new RasterHelper();
            this.dataStruct = swe.createRecord()
                    .name(name)
                    .label("Video Stream")
                    .description("")
                    .definition(SWEHelper.getPropertyUri("VideoFrame"))
                    .addField("sampleTime", swe.createTime()
                            .asSamplingTimeIsoUTC()
                            .label("Sample Time")
                            .description("Time of data collection"))
                    .addField("img", swe.newRgbImage(width, height, DataType.BYTE))
                    .build();

            dataStruct.renewDataBlock();
            latestRecord = dataStruct.createDataBlock();
        }


        protected void setStruct(DataRecord newStruct) {
            this.dataStruct = newStruct;
        }

        @Override
        public DataComponent getRecordDescription() {
            return dataStruct;
        }

        /*
        public void setSize(Count width, Count height) {
            this.width = width;
            this.height = height;
        }*/

        @Override
        public DataEncoding getRecommendedEncoding() {
            return swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);
        }

        @Override
        public double getAverageSamplingPeriod() {
            return Double.NaN;
        }
        // TODO Add a data publishing method

        public void publishData() {
            /*

            updateIntervalHistogram();*/
            logger.debug("Publishing output");
            //latestRecord.setUnderlyingObject(dataStruct.get)[1];
            latestRecord = dataStruct.getData().clone();
            eventHandler.publish(new DataEvent(System.currentTimeMillis(), this, latestRecord));
        }

        private void updateIntervalHistogram() {
            synchronized (histogramLock) {
                if (latestRecord != null && latestRecordTime != Long.MIN_VALUE) {
                    long interval = System.currentTimeMillis() - latestRecordTime;
                    intervalHistogram.add(interval / 1000d);

                    if (intervalHistogram.size() > MAX_NUM_TIMING_SAMPLES) {
                        intervalHistogram.remove(0);
                    }
                }
            }
        }
    }


    protected class DataQueuePusher implements IEventListener {
        DataQueue dataQueue;

        public DataQueuePusher(DataQueue dataQueue) {
            this.dataQueue = dataQueue;
        }

        @Override
        public void handleEvent(Event e) {
            // Don't want queue to be filled before we can start processing
            if (getCurrentState() != ModuleEvent.ModuleState.STARTED) {
                return;
            }
            try {
                // Force data through if coming from a sensor
                if (e.getSource() instanceof AbstractDataInterface<?> output) {
                    if (isVideoData(output.getRecordDescription())) {
                        dataQueue.getSourceComponent().setData(output.getLatestRecord().clone());
                        dataQueue.publishData();
                        logger.debug("Published data queue");
                    } else {
                        logger.debug("Not video");
                    }
                }
            } catch (Exception ex) {
                logger.error("Could not publish data queue.", ex);
            }
            /*
            logger.debug("Received event");
            if (e.getSource() instanceof AbstractDataInterface<?> output) {
                logger.debug("Received data interface event");
                try {
                    if (output.getRecordDescription().getId().equals(dataQueue.getSourceComponent().getId())) {
                        dataQueue.publishData();
                        logger.debug("Published data queue");
                    }
                } catch (Exception ex) {
                    logger.error("Could not publish data queue.", ex);
                }
            }

             */
        }
    }
}

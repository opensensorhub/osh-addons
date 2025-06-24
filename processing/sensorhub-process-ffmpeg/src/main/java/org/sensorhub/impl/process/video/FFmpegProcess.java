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
import org.vast.cdm.common.CDMException;
import org.vast.data.*;
import org.vast.process.*;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SimpleProcessImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public abstract class FFmpegProcess extends AbstractProcessModule<FFmpegProcessConfig> {
    SimpleProcessImpl process;
    IModule<?> sensorModule;
    IProcessExec executable;
    IStreamingDataInterface inputVideoOutport;
    AbstractSWEIdentifiable inputVideoInport, outputVideoOutport;
    DataQueue dataQueue, outQueue;
    IStreamingDataInterface videoOutput;
    SMLHelper smlHelper;
    RasterHelper rasterHelper;
    boolean started = false;
    Thread execFuture;
    Count width, height;
    String uuid = UUID.randomUUID().toString();

    public FFmpegProcess() {
        super();
        smlHelper = new SMLHelper();
        process = new SimpleProcessImpl();
        process.setUniqueIdentifier(uuid);
        processDescription = process;

    }

    @Override
    public void doInit() throws SensorException, SensorHubException {

        //
        execFuture = null;
        process = new SimpleProcessImpl();
        process.setUniqueIdentifier(uuid);
        processDescription = process;
        executable = null;
        sensorModule = null;
        width = null;
        height = null;

        // Get input module from registry
        if (hasParentHub()) {
            sensorModule = getParentHub().getModuleRegistry().getModuleById(config.videoUID);
        } else {
            throw new SensorException("Parent Hub not set.");
        }

        // Determine type and get video output stream
        if (sensorModule instanceof AbstractSensorModule<?>) {
            var opt = ((AbstractSensorModule) sensorModule).getOutputs().values().stream().filter(output -> (isVideoData(((IStreamingDataInterface)output).getRecordDescription()))).findFirst();

            if (opt.isPresent()) {
                inputVideoOutport = (IStreamingDataInterface) opt.get();
            } else {
                throw new SensorException("No video stream output found for module " + sensorModule.getName() + ".");
            }
        } else if (sensorModule instanceof AbstractProcessModule<?>) {
            var opt = ((AbstractProcessModule) sensorModule).getOutputs().values().stream().filter(output -> (isVideoData(((IStreamingDataInterface)output).getRecordDescription()))).findFirst();
            if (opt.isPresent()) {
                inputVideoOutport = (IStreamingDataInterface) opt.get();
            } else {
                throw new SensorException("No video stream output found for module " + sensorModule.getName() + ".");
            }
        } else {
            throw new SensorException("Module type " + sensorModule.getClass().getName() + " not supported.");
        }

        height = smlHelper.createCount().value(inputVideoOutport.getRecordDescription().getComponent("img").getComponentCount()).build();
        width = smlHelper.createCount().value(inputVideoOutport.getRecordDescription().getComponent("img").getComponent("row").getComponentCount()).build();

        // Create an executable process

        try {
            executable = config.execProcess.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new SensorException("Could not instantiate process.", e);
        }

        // Use config to modify inputs, outputs, params, etc. for the process
        initExcProcess(executable);

        // Get input port, create connection
        var opt = executable.getInputList().stream().filter(input -> (isVideoData((DataRecord)input))).findFirst();

        if (opt.isPresent()) {
            inputVideoInport = opt.get();
        } else {
            throw new SensorException("No video stream input found for process " + executable.getInstanceName() + ".");
        }

        setArraySize(inputVideoInport);

        var opt2 = executable.getOutputList().stream().filter(output -> isVideoData((DataRecord)output)).findFirst();

        if (opt2.isPresent()) {
            outputVideoOutport = opt2.get();
        } else {
            throw new SensorException("No video stream output found for process " + executable.getInstanceName() + ".");
        }

        setArraySize(outputVideoOutport);

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

        videoOutput = new VideoDataInterface("inputVideo", width, height, ((DataArray)SMLHelper.getIOComponent(outputVideoOutport).getComponent("img")).isVariableSize(), this);
        //((VideoDataInterface)videoOutput).setSize(width, height);
        //var videoOutputRecord = videoOutput.getRecordDescription();
        //((DataArray)SMLHelper.getIOComponent(videoOutputRecord).getComponent("img")).setElementCount(height);
        //((DataArray)SMLHelper.getIOComponent(videoOutputRecord).getComponent("img").getComponent("row")).setElementCount(width);


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

        try {
            process.setExecutableImpl(executable);
            process.init();
        } catch (Exception e) {
            throw new SensorException("Could not initialize process.", e);
        }

        // Adding output last so that the structure (and compression/encoding) can be copied after process init
        ((VideoDataInterface)videoOutput).setStruct(outQueue.getSourceComponent());
        addOutput(videoOutput);

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }

    private void setArraySize(AbstractSWEIdentifiable videoComponent) {
        ((DataArray) SMLHelper.getIOComponent(videoComponent).getComponent("img")).setElementCount(height);
        ((DataArray)SMLHelper.getIOComponent(videoComponent).getComponent("img").getComponent("row")).setElementCount(width);
        ((DataArray)SMLHelper.getIOComponent(videoComponent).getComponent("img")).getArraySizeComponent().setValue(height.getValue());
        ((DataArray)SMLHelper.getIOComponent(videoComponent).getComponent("img").getComponent("row")).getArraySizeComponent().setValue(width.getValue());
    }

    private <T extends Throwable> void onError(T throwable) {
        //throw new SensorException("Error starting process.", throwable);
        logger.error(throwable.getMessage());
    }



    @Override
    public void doStart() throws SensorException, SensorHubException {

        try {
            process.init();
            process.start(this::onError);
        } catch (ProcessException e) {
            throw new SensorException("Could not start process.", e);
        }

        started = true;

        execFuture = new Thread(new Runnable() {
            @Override
            public void run()
            {
                logger.debug("Process thread started");
                try
                {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            outQueue.transferData(true);
                            ((VideoDataInterface)videoOutput).publishData();
                        } catch (InterruptedException e) {
                            logger.debug("Process output queue interrupted.");
                            return;
                        }
                    }
                }
                catch (Throwable e)
                {
                    started = false;
                    logger.error("Error during execution", e);
                }

                logger.debug("Process thread stopped");
            }
        });
        execFuture.start();
    }

    @Override
    public void doStop() throws SensorHubException {
        // TODO Move some of this to doInit()
        started = false;
        process.stop();
        //process.dispose();

        if (execFuture != null)
        {
            execFuture.interrupt();
            try {
                execFuture.join();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while joining process.", e);
            }

        }

        setState(ModuleEvent.ModuleState.STOPPED);
    }

    // TODO: Start using this method to avoid long repeat code
    // Best way to have data struct detected as video is by adding the VideoFrame sml definition
    boolean isVideoData(DataComponent dataDescription) {
        return dataDescription != null && ( (dataDescription.getDefinition() != null && dataDescription.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame"))
                || (dataDescription.getName() != null && (dataDescription.getName().equalsIgnoreCase("videoframe")
                || dataDescription.getName().equalsIgnoreCase("video"))));
    }



    public abstract void initExcProcess(IProcessExec executable);

    protected class VideoDataInterface extends AbstractSensorOutput<FFmpegProcess> {
        DataComponent dataStruct;
        RasterHelper swe;
        //Count width, height;
        private final Object histogramLock = new Object();
        private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
        private static final int MAX_NUM_TIMING_SAMPLES = 10;
        BinaryEncoding dataEnc;
        BinaryBlock compressedBlock;

        protected VideoDataInterface(String name, Count width, Count height, boolean isVariable, FFmpegProcess parent) {
            super(name, parent);
            swe = new RasterHelper();
            DataArray img;
            // This is just the default structure. Overwritten during the init.
            this.dataStruct = swe.createRecord()
                    .name(name)
                    .label("Video Stream")
                    .description("")
                    .definition(SWEHelper.getPropertyUri("VideoFrame"))
                    .addField("sampleTime", swe.createTime()
                            .asSamplingTimeIsoUTC()
                            .label("Sample Time")
                            .description("Time of data collection"))
                    .addField("img", img = swe.newRgbImage(swe.createCount().id("width").build(), swe.createCount().id("height").build(), DataType.BYTE))
                    .build();

            if (isVariable) {
                img.updateSize(height.getValue());
                ((DataArray) img.getComponent("row")).updateSize(width.getValue());
            } else {
                img.setElementCount(height);
                ((DataArray) img.getComponent("row")).setElementCount(width);
            }

            dataEnc = swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

            BinaryComponent timeEnc = swe.newBinaryComponent();
            timeEnc.setRef("/" + dataStruct.getComponent(0).getName());
            timeEnc.setCdmDataType(DataType.DOUBLE);
            dataEnc.addMemberAsComponent(timeEnc);

            compressedBlock = swe.newBinaryBlock();
            compressedBlock.setRef("/" + dataStruct.getComponent(1).getName());
            compressedBlock.setCompression(""); //TODO: CHANGE THIS TO ACCURATELY REFLECT COMPRESSION
            dataEnc.addMemberAsBlock(compressedBlock);

            try {
                SWEHelper.assignBinaryEncoding(dataStruct, dataEnc);
            } catch (CDMException e) {
                throw new RuntimeException("Invalid binary encoding configuration", e);
            }

            dataStruct.renewDataBlock();
            latestRecord = dataStruct.createDataBlock();
        }

        public void setStruct(DataComponent newStruct) {
            dataStruct = newStruct;
            if (newStruct.hasData()) {
                dataStruct.setData(newStruct.getData());
            } else {
                dataStruct.setData(newStruct.createDataBlock());
            }
            dataStruct.renewDataBlock();
            latestRecord = dataStruct.createDataBlock();
        }

        @Override
        public DataComponent getRecordDescription() {
            return dataStruct;
        }


        public void setSize(Count width, Count height) {
            ((DataArray)SMLHelper.getIOComponent(dataStruct).getComponent("img")).getArraySizeComponent().setValue(height.getValue());
            ((DataArray)SMLHelper.getIOComponent(dataStruct).getComponent("img").getComponent("row")).getArraySizeComponent().setValue(width.getValue());
            dataStruct.renewDataBlock();
            latestRecord = dataStruct.getData();
        }

        @Override
        public DataEncoding getRecommendedEncoding() {
            if (dataEnc != null) {
                return dataEnc;
            }
            return swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);
        }

        // TODO Avg sampling period
        @Override
        public double getAverageSamplingPeriod() {
            return Double.NaN;
        }

        public void publishData() {
            if (!dataStruct.hasData()) { return; }

            var curtime = System.currentTimeMillis();
            logger.debug("Publishing output");
            //(DataBlockCompressed)
            //latestRecord.setUnderlyingObject(data);
            //((DataBlock)latestRecord).setUnderlyingObject(dataStruct.getData().getUnderlyingObject());
            //((DataBlockMixed)latestRecord).getUnderlyingObject()[1].setUnderlyingObject(dataStruct.getComponent("img").getData().getUnderlyingObject());
            //((DataBlockMixed)latestRecord).setDoubleValue(curtime / 1000d);
            dataStruct.getData().setDoubleValue(curtime / 1000d);
            latestRecord = dataStruct.getData().clone();
            latestRecordTime = curtime;
            eventHandler.publish(new DataEvent(curtime, this, latestRecord));
            dataStruct.renewDataBlock();
        }

        // TODO Either use this somewhere or remove
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


    // Listens for event from dara
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
                // If event data is from source component, add to queue and publish
                if (e.getSource() instanceof AbstractDataInterface<?> output) {
                    if (output.getRecordDescription() == null || output.getLatestRecord() == null) {
                        return;
                    }
                    // Set the image data in the queue
                    if (isVideoData(output.getRecordDescription())) {
                        if (!dataQueue.getSourceComponent().hasData()) {
                            dataQueue.getSourceComponent().assignNewDataBlock();
                        }
                        dataQueue.getSourceComponent().setData(output.getLatestRecord().clone());
                        //((DataBlockCompressed)dataQueue.getSourceComponent().getData()).setUnderlyingObject(((DataBlockMixed) output.getLatestRecord()).getUnderlyingObject()[1]);
                        //((DataBlockCompressed)dataQueue.getSourceComponent().getData()).setUnderlyingObject(((AbstractDataBlock)output.getLatestRecord().getUnderlyingObject()).getByteValue());
                        dataQueue.publishData();

                        logger.debug("Published data queue");
                    } else {
                        //logger.debug("Not video");
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

package org.sensorhub.process.universalcontroller.helpers;

import com.botts.impl.sensor.universalcontroller.helpers.UniversalControllerProcessHelper;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;

public abstract class AbstractControllerTaskingProcess extends ExecutableProcessImpl {

    public UniversalControllerProcessHelper fac;
    public AbstractControllerTaskingProcess(ProcessInfo processInfo) {
        super(processInfo);

        fac = new UniversalControllerProcessHelper();

        inputData.add(fac.getComponentRecord().getName(), fac.getComponentRecord());

        paramData.add(fac.createControlStreamIndexParameter().getName(), fac.createControlStreamIndexParameter());
    }

    public abstract void updateOutputs() throws ProcessException;

    @Override
    public void execute() throws ProcessException {
        fac.setComponentRecord(fac.getComponentRecord().getData());
        if(fac.getPrimaryControlStreamIndexInput() == paramData.getComponent(fac.createControlStreamIndexParameter().getName()).getData().getIntValue()) {
            updateOutputs();
        }
    }
}

package org.sensorhub.impl.sensor.mesh;

import java.io.IOException;
import java.nio.file.Path;

public interface FileListener {
	public void newFile(Path p) throws IOException;
}

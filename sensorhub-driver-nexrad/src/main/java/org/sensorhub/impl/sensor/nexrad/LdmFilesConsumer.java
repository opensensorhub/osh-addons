package org.sensorhub.impl.sensor.nexrad;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: LdmFilesConsumer.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 29, 2016
 */
public class LdmFilesConsumer { //implements Runnable {

	Logger logger = LoggerFactory.getLogger(LdmFilesConsumer.class);
	PriorityBlockingQueue<String> queue;
	int vol, chunk;
	char type;
	boolean first = true;
	static final int START_SIZE = 3;
	static final int SIZE_LIMIT = 5;

	public LdmFilesConsumer(PriorityBlockingQueue<String> queue) {
		this.queue = queue;
	}

	public void init() {
		while (queue.size() < START_SIZE) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void dump(BlockingQueue queue) {
		String [] sarr = (String[]) queue.toArray(new String [] {});
		Arrays.sort(sarr);
		for (String st: sarr)
			System.err.println(st);

	}

	boolean isNext(int v, int c, char t) {
		boolean isNext;
		if(type == 'E') {
			isNext = (v == vol + 1) && (c == 1);
		} else {
			isNext = (v == vol) && (c == chunk + 1);
		}
		if(isNext) {
			vol = v;
			chunk = c;
			type = t;
		}
		//System.err.println(isNext + ": " + v +"," + c + "," + t);
		return isNext;
	}

	public String nextFile() throws InterruptedException {
		boolean nextFile = false;
		while(!nextFile) {
			if(first) {
				String f = queue.take();
				String [] sarr = f.split("_");
				vol = Integer.parseInt(sarr[1]);
				int dashIdx = f.lastIndexOf('-');
				assert (dashIdx > 10);
				String s = f.substring(dashIdx - 3, dashIdx);
				chunk = Integer.parseInt(s);
				type = f.charAt(f.length() - 1);
				first = false;
				continue;
			}

			String f = queue.peek();
			if(f == null)
				continue;
//			System.err.println("Peek that: " + f);
			String [] sarr = f.split("_");
			int v = Integer.parseInt(sarr[1]);
			int dashIdx = f.lastIndexOf('-');
			assert (dashIdx > 10);
			String s = f.substring(dashIdx - 3, dashIdx);
			int c = Integer.parseInt(s);
			char t = f.charAt(f.length() - 1);
			if (isNext(v,c,t)) {
				f = queue.take();
				logger.debug("Take that: {}" + f);
				return f;
			} else if(queue.size() > SIZE_LIMIT) {
				f = queue.take(); 
				logger.debug("Force take: {}" + f);
				chunk = c;
				vol = v;
				type = t;
			}
			dump(queue);					
			Thread.sleep(1000L);
		}

		return null;
	}
}

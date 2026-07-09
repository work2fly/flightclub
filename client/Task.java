/*
 * @(#)Task.java (part of 'Flight Club')
 * 
 * This code is covered by the GNU General Public License
 * detailed at http://www.gnu.org/copyleft/gpl.html
 *	
 * Flight Club docs located at http://www.danb.dircon.co.uk/hg/hg.htm
 * Copyright 2001-2003 Dan Burton <danb@dircon.co.uk>
 */
package flightclub.client;

import flightclub.framework3d.*;
import java.io.*;

/** This class implements a task. */
public class Task implements CameraSubject {
    XCModelViewer xcModelViewer;
    String taskID;
    NodeManager nodeManager;
    TurnPointManager turnPointManager;
    RoadManager roadManager;
    Trigger[] triggers;
    SinkZone[] sinkZones;
    Ridge[] ridges;
    float wind_x, wind_y;

    // for the default course
    static final float HEXAGON = Cloud.CLOUDBASE * 7; //8;

    /** Parses the specifed file to create the task. */
    public Task(XCModelViewer xcModelViewer, String taskID) throws IOException {
		this.xcModelViewer = xcModelViewer;
		this.taskID = taskID;
		parseFile(taskID);
		nodeManager = new NodeManager(xcModelViewer, this);
    }

    /** No file to parse. Create a simple default task. */
    public Task(XCModelViewer xcModelViewer) {
		this.xcModelViewer = xcModelViewer;
		this.taskID = "default";

		// turn points
		float x = Cloud.CLOUDBASE * 10;
		//x /= 5; // tmp - small course for testing gliding around the turn points
		float[] xs = {x, x, 2 * x, x};
		float[] ys = {x, 2 * x, 1.5f * x, x};
		turnPointManager = new TurnPointManager(xcModelViewer, xs, ys);

		// wind
		wind_x = 0.1f;
		wind_y = 0.1f;

		// triggers (thermals with visible clouds + some blue thermals)
		int numRegular = 4 * 4 * 6;
		int numBlue = 3;
		triggers = new Trigger[numRegular + numBlue];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				flatLand(i * HEXAGON, j * HEXAGON);
			}
		}

		// add a few blue thermals (invisible lift sources - no cloud marker)
		Trigger bt;
		bt = new Trigger(xcModelViewer, HEXAGON * 1.2f, HEXAGON * 1.8f);
		bt.blueThermal = true;
		triggers[next++] = bt;
		bt = new Trigger(xcModelViewer, HEXAGON * 2.8f, HEXAGON * 0.7f);
		bt.blueThermal = true;
		triggers[next++] = bt;
		bt = new Trigger(xcModelViewer, HEXAGON * 0.4f, HEXAGON * 3.2f);
		bt.blueThermal = true;
		triggers[next++] = bt;

		// roads - specify start and end points
		float[][] r1 = new float[][] {{0, 0, 0}, {0.8f * x, x, 0}, {x, 2 * x, 0}, {2 * x, 4 * x,0}};
		float[][] r2 = new float[][] {{0, x, 0}, {x, 1.4f * x, 0}, {2 * x, 1.2f * x, 0}, {3 * x, 2 * x, 0}, {4 * x, 2 * x,0}};
		roadManager = new RoadManager(xcModelViewer, new float[][][] {r1, r2});

		// sink zones - small pockets of descending air placed at the center
		// of each hexagon cell, well clear of the trigger points themselves
		// (which sit roughly 7+ units from the cell center - see flatLand()).
		// A previous version used large zones aligned with the thermal grid
		// which overlapped the trigger clusters directly, cancelling out
		// the lift gliders were trying to climb in.
		sinkZones = new SinkZone[4];
		float sinkSize = HEXAGON * 0.25f; // small enough to clear trigger points
		sinkZones[0] = new SinkZone(xcModelViewer, HEXAGON * 0.5f, HEXAGON * 1.5f, sinkSize, sinkSize, 1.5f);
		sinkZones[1] = new SinkZone(xcModelViewer, HEXAGON * 1.5f, HEXAGON * 0.5f, sinkSize, sinkSize, 1.0f);
		sinkZones[2] = new SinkZone(xcModelViewer, HEXAGON * 2.5f, HEXAGON * 2.5f, sinkSize, sinkSize, 2.0f);
		sinkZones[3] = new SinkZone(xcModelViewer, HEXAGON * 3.0f, HEXAGON * 1.0f, sinkSize, sinkSize, 1.2f);

		// ridges - placed within easy reach of launch (x,x) so they can
		// actually be tested/used, but off the direct line to the first
		// turn point (x,x)->(x,2x), which runs straight north at x-coord x.
		ridges = new Ridge[2];
		// a small hill just east of launch, ~9 units away - reachable on
		// first glide from any glider type
		ridges[0] = new Ridge(xcModelViewer,
			x + 6, x + 3, x + 6, x + 9,
			Cloud.CLOUDBASE * 1.0f, Cloud.CLOUDBASE * 1.8f, Cloud.CLOUDBASE * 1.2f);
		// a longer mountain ridge further out along the second leg
		ridges[1] = new Ridge(xcModelViewer,
			x + 8, 1.6f * x, x + 8, 1.9f * x,
			Cloud.CLOUDBASE * 1.5f, Cloud.CLOUDBASE * 2.2f, Cloud.CLOUDBASE * 1.4f);
		for (int i = 0; i < ridges.length; i++) {
			ridges[i].renderMe();
		}
  
		nodeManager = new NodeManager(xcModelViewer, this);
    }

    private void parseFile(String taskID) throws IOException {
		InputStream is = xcModelViewer.modelEnv.openFile(taskID + ".task");
		StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
		st.eolIsSignificant(true);
		st.commentChar('#');
		st.wordChars(':', ':');
		st.wordChars('_', '_'); //TODO

		// gobble EOL's due to comments
		while (st.nextToken() == StreamTokenizer.TT_EOL) {;}

		// wind
		if (! "WIND:".equals(st.sval)) {
			throw new FileFormatException("Unable to read wind: " + st.sval);
		}
		st.nextToken();
		wind_x = (float) st.nval;
		st.nextToken();
		wind_y = (float) st.nval;
		st.nextToken(); // new line	

		// turn points
		turnPointManager = new TurnPointManager(xcModelViewer, st);

		// triggers
		st.nextToken();
		if (! "NUM_TRIGGERS:".equals(st.sval)) {
			throw new FileFormatException("Unable to read number of triggers: " + st.sval);
		}
		st.nextToken();
		int numTriggers = (int) st.nval;
		triggers = new Trigger[numTriggers];
		st.nextToken();	

		for (int i = 0; i < triggers.length; i++) {
			triggers[i] = new Trigger(xcModelViewer, st);
		}

		// roads
		roadManager = new RoadManager(xcModelViewer, st);

		// gobble any trailing EOL's
		while (st.nextToken() == StreamTokenizer.TT_EOL) {;}

		// should be at end of file
		is.close();
		if (st.ttype != StreamTokenizer.TT_EOF) {
			throw new FileFormatException(st.toString());
		}
    }

    /** Returns the total distance of this task. */
    float getTotalDistance() { return turnPointManager.getTotalDistance(); }

    /** Prints debug info. */
    void asString() {
		System.out.println("Task: " + taskID);
		turnPointManager.asString();
		roadManager.asString();
		nodeManager.asString();
    }

    /** Returns the mid point of the bounding box. */
    public float[] getFocus() {
		float[] a = new float[3];
		float[] b = new float[3];
		float[] p = new float[3];
		turnPointManager.boundingBox(a, b);
		Tools3d.add(a, b, p);
		p[0] /= 2;
		p[1] /= 2;
		return p;
    }

    /**
       Returns the 'near' corner of the bounding box but with the z
       component set to either the width or the height of the box,
       whichever is greater.

       Changed to mid point. ?
    */
    public float[] getEye() {
		float[] a = new float[3];
		float[] b = new float[3];
		float[] p = new float[3];
		turnPointManager.boundingBox(a, b);
		Tools3d.subtract(b, a, p);

		// aspect ratio
		if (p[0] > p[1]) {
			// x maps to screen height
			a[2] = p[0] * 0.7f;
		} else {
			// y maps to screen width
			a[2] = p[1] * 1.4f;
		}

		a[0] += p[0] * 0.5f;
		a[1] += p[1] * 0.4f; // look along the y axis
		return a;
    }

    private int next = 0;
    /**
       Produces a hexagon structure of thermal triggers.

       <pre>

       y
       |
       |
       .--->x
                .
            .       .

            .       .
                .

       </pre>
    */
    void flatLand(float x0, float y0) {
        Trigger trigger;
        float y1, x1;

        y1 = y0 + HEXAGON;
        x1 = x0 + HEXAGON;
        float dh = HEXAGON/6;

        trigger = new Trigger(xcModelViewer, x0 + HEXAGON/2, y0 + dh);
        triggers[next++] = trigger;

        trigger = new Trigger(xcModelViewer, x0 + HEXAGON/2, y1 - dh);
        triggers[next++] = trigger;

        trigger = new Trigger(xcModelViewer, x0 + dh, y0 + 2 * dh);
        triggers[next++] = trigger;

        trigger = new Trigger(xcModelViewer, x0 + dh, y1 - 2 * dh);
        triggers[next++] = trigger;

        trigger = new Trigger(xcModelViewer, x1 - dh, y0 + 2 * dh);
        triggers[next++] = trigger;

        trigger = new Trigger(xcModelViewer, x1 - dh, y1 - 2 * dh);
        triggers[next++] = trigger;
    }

}




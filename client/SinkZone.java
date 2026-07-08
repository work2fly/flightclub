/*
 * SinkZone.java (part of 'Flight Club')
 * 
 * This code is covered by the GNU General Public License
 * detailed at http://www.gnu.org/copyleft/gpl.html
 *
 * Added as part of the linux-revival branch.
 * Implements Dan Burton's TODO: "Sink (simply rectangles)"
 */
package flightclub.client;

import flightclub.framework3d.*;
import java.awt.Color;

/**
 * A rectangular zone of sinking air between thermals.
 * Gliders passing through sink zones will descend faster.
 * 
 * Sink zones are static (no lifecycle, no drift) and are placed
 * between thermal triggers to create more realistic air.
 */
public class SinkZone implements LiftSource {
    XCModelViewer xcModelViewer;
    float x, y;       // center position
    float halfW, halfH; // half-width and half-height of the rectangle
    float sinkRate;    // negative value (descent rate)
    private Obj3d obj3d;

    static final float SINK_UNIT = -0.03f; // weaker than thermals
    static final Color COLOR = new Color(200, 200, 255); // pale blue tint

    /**
     * Creates a sink zone centered at (x, y) with given dimensions.
     * @param strength 1.0 to 3.0 - multiplied by SINK_UNIT for actual sink rate
     */
    public SinkZone(XCModelViewer xcModelViewer, float x, float y, 
                    float width, float height, float strength) {
        this.xcModelViewer = xcModelViewer;
        this.x = x;
        this.y = y;
        this.halfW = width / 2;
        this.halfH = height / 2;
        this.sinkRate = strength * SINK_UNIT;
    }

    public float[] getP() {
        return new float[] {x, y, 0};
    }

    /**
     * Returns the sink (negative lift) at a point.
     * Uniform sink across the whole rectangle - no gradient.
     */
    public float getLift(float[] p) {
        if (!contains(p)) return 0;
        // sink diminishes with altitude (weaker near cloudbase)
        float altFactor = 1.0f - (p[2] / (Cloud.CLOUDBASE * 1.5f));
        if (altFactor < 0) altFactor = 0;
        return sinkRate * altFactor;
    }

    /** Returns true if point is within the rectangular zone. */
    public boolean contains(float[] p) {
        if (p[2] >= Cloud.CLOUDBASE) return false; // above cloudbase, no effect
        return (Math.abs(p[0] - x) <= halfW && Math.abs(p[1] - y) <= halfH);
    }

    /** Sink zones are always active. */
    public boolean isActive() { return true; }
    public boolean isActive(float t) { return true; }

    /** Returns the max sink rate (negative). */
    public float getLift() { return sinkRate; }

    /** Render as a subtle rectangle on the ground. */
    void renderMe() {
        obj3d = new Obj3d(xcModelViewer, 0, true);
        obj3d.setNumPolywires(1);
        float[][] ps = new float[][] {
            {x - halfW, y - halfH, 0},
            {x + halfW, y - halfH, 0},
            {x + halfW, y + halfH, 0},
            {x - halfW, y + halfH, 0}
        };
        obj3d.addPolywireClosed(ps, COLOR);
    }

    void destroyMe() {
        if (obj3d != null) {
            obj3d.destroyMe();
            obj3d = null;
        }
    }
}

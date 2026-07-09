/*
 * Ridge.java (part of 'Flight Club')
 *
 * This code is covered by the GNU General Public License
 * detailed at http://www.gnu.org/copyleft/gpl.html
 *
 * Added as part of the linux-revival branch.
 * Implements Dan Burton's TODO: "Terrain !? => finish TaskDesigner"
 * plus the user's request for hills/mountains/ridges with physically
 * modelled slope soaring (windward lift, leeward sink + turbulence).
 */
package flightclub.client;

import flightclub.framework3d.*;
import java.awt.Color;

/**
 * A linear terrain feature (ridge, spine of a mountain range, hill flank)
 * defined by a crest line between two points, with a triangular
 * cross-section sloping down to the ground on both sides.
 *
 * <pre>
 *
 *           crestHeight
 *                |
 *      windward  .  leeward
 *      slope    / \  slope
 *              /   \
 *    ---------.     .---------   ground (z=0)
 *    |&lt;------&gt;|     |&lt;------&gt;|
 *    windwardWidth   leewardWidth
 *
 * </pre>
 *
 * Physics model:
 * <ul>
 *   <li>The windward face (the side the wind blows onto) deflects the
 *       horizontal wind up the slope. Lift strength scales with the wind
 *       speed component perpendicular to the ridge and with the slope
 *       angle (steeper = more lift), following liftMax = windSpeed *
 *       sin(slopeAngle) for flow that stays attached to the face.</li>
 *   <li>The leeward face (downwind side) gets sink as the air spills over
 *       the top and descends, plus mechanical turbulence (random jitter)
 *       from the separated, tumbling airflow - this is "rotor".</li>
 *   <li>Both effects fade with height above the local terrain (strongest
 *       right at the slope surface) and with lateral distance from the
 *       ridge line, and taper off near the ends of the ridge segment.</li>
 * </ul>
 */
public class Ridge implements LiftSource {
    XCModelViewer xcModelViewer;
    float x1, y1, x2, y2;    // ridge crest line endpoints
    float crestHeight;       // height of the ridge crest above ground
    float windwardWidth;     // horizontal run from crest to base, windward side
    float leewardWidth;      // horizontal run from crest to base, leeward side

    // derived geometry
    private float dirX, dirY;    // unit vector from p1 to p2 (along the ridge)
    private float perpX, perpY;  // unit vector perpendicular to dir (90 deg left)
    private float length;

    // which side is windward, in terms of the +perp / -perp sign convention
    private boolean windwardIsPos;
    private float widthPos, widthNeg; // terrain width on the +perp / -perp side

    // tuning constants
    static final float END_MARGIN_FRAC = 0.15f;  // fraction of length used to fade lift near the ends
    static final float LIFT_HEIGHT_FACTOR = 1.4f; // effect zone extends to this multiple of crestHeight
    static final float SINK_FACTOR = 0.6f;        // leeward sink relative to windward lift
    static final float TURBULENCE_FACTOR = 0.5f;  // max random jitter (in position units) per second

    // Ridge lift is scaled by slope steepness like real slope soaring, but
    // (like Cloud.LIFT_UNIT for thermals) uses a fixed unit rather than the
    // raw wind speed, since this game's wind values (~0.1) are far smaller
    // than glider sink rates (~0.06-0.15) and would never produce usable
    // lift if multiplied directly. A slope of ~45 degrees in light wind
    // gives lift comparable to a small thermal.
    static final float LIFT_UNIT = 0.35f;
    // minimum crosswind (component blowing onto the ridge) needed for the
    // ridge to work at all - below this, wind is too light/wrong direction
    static final float MIN_WIND = 0.03f;

    private Obj3d obj3d;

    /**
     * Creates a ridge. Wind direction is read from the task at construction
     * time to determine which side is windward - this game has static wind
     * per task so this only needs to be computed once.
     */
    public Ridge(XCModelViewer xcModelViewer, float x1, float y1, float x2, float y2,
                 float crestHeight, float windwardWidth, float leewardWidth) {
        this.xcModelViewer = xcModelViewer;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        this.crestHeight = crestHeight;
        this.windwardWidth = windwardWidth;
        this.leewardWidth = leewardWidth;

        float dx = x2 - x1;
        float dy = y2 - y1;
        length = (float) Math.sqrt(dx * dx + dy * dy);
        dirX = dx / length;
        dirY = dy / length;
        // perpendicular: rotate dir by +90 degrees
        perpX = -dirY;
        perpY = dirX;

        Task task = xcModelViewer.xcModel.task;
        float windX = (task != null) ? task.wind_x : 0;
        float windY = (task != null) ? task.wind_y : 0;
        float crossWind = windX * perpX + windY * perpY;

        // If wind has positive component along +perp, air is moving from the
        // -perp side to the +perp side, so -perp is windward (upwind).
        windwardIsPos = crossWind < 0;
        if (windwardIsPos) {
            widthPos = windwardWidth;
            widthNeg = leewardWidth;
        } else {
            widthPos = leewardWidth;
            widthNeg = windwardWidth;
        }
    }

    /** Signed distance along the ridge line (0 at p1, length at p2). */
    private float sAlong(float x, float y) {
        return (x - x1) * dirX + (y - y1) * dirY;
    }

    /** Signed distance perpendicular to the ridge line (+ve on the +perp side). */
    private float dCross(float x, float y) {
        return (x - x1) * perpX + (y - y1) * perpY;
    }

    /** Terrain height at a given perpendicular offset from the crest line. */
    private float terrainHeightAt(float d) {
        float width = (d >= 0) ? widthPos : widthNeg;
        if (width <= 0) return 0;
        float frac = 1 - Math.abs(d) / width;
        if (frac < 0) frac = 0;
        return crestHeight * frac;
    }

    /** Returns the ground height at (x, y) due to this ridge - used for terrain collision. */
    public float getGroundHeight(float x, float y) {
        float s = sAlong(x, y);
        if (s < 0 || s > length) return 0; // beyond the ends, ridge has no height
        float d = dCross(x, y);
        return terrainHeightAt(d);
    }

    /** Fades to zero over the last END_MARGIN_FRAC of the ridge at each end. */
    private float endFade(float s) {
        float margin = length * END_MARGIN_FRAC;
        if (margin <= 0) return (s >= 0 && s <= length) ? 1 : 0;
        if (s < 0 || s > length) return 0;
        if (s < margin) return s / margin;
        if (s > length - margin) return (length - s) / margin;
        return 1;
    }

    /** Magnitude of the wind component blowing across the ridge (perpendicular). */
    private float crossWindSpeed() {
        Task task = xcModelViewer.xcModel.task;
        if (task == null) return 0;
        float cw = task.wind_x * perpX + task.wind_y * perpY;
        return Math.abs(cw);
    }

    /**
     * Returns true if this ridge affects the point p - either lift on the
     * windward face or sink/turbulence on the leeward face.
     */
    public boolean contains(float[] p) {
        float s = sAlong(p[0], p[1]);
        if (s < -length * END_MARGIN_FRAC || s > length * (1 + END_MARGIN_FRAC)) return false;
        float d = dCross(p[0], p[1]);
        float terrainH = terrainHeightAt(d);
        float hAgl = p[2] - terrainH;
        float effectHeight = crestHeight * LIFT_HEIGHT_FACTOR;
        return (hAgl >= 0 && hAgl <= effectHeight);
    }

    /**
     * Returns lift (positive, windward face) or sink (negative, leeward
     * face) at the given point. Magnitude is windSpeed * sin(slopeAngle),
     * scaled by how close the point is to the slope surface and how far
     * along the ridge it is (fading near the ends).
     */
    public float getLift(float[] p) {
        float s = sAlong(p[0], p[1]);
        float d = dCross(p[0], p[1]);
        float terrainH = terrainHeightAt(d);
        float hAgl = p[2] - terrainH;
        float effectHeight = crestHeight * LIFT_HEIGHT_FACTOR;

        if (hAgl < 0 || hAgl > effectHeight) return 0;

        boolean onPosSide = (d >= 0);
        boolean isWindward = (onPosSide == windwardIsPos);
        float sideWidth = onPosSide ? widthPos : widthNeg;
        if (sideWidth <= 0) return 0;

        float lateral = 1 - Math.abs(d) / sideWidth;
        if (lateral < 0) lateral = 0;

        float heightFade = 1 - hAgl / effectHeight;
        float fadeAtEnds = endFade(s);

        float windSpeed = crossWindSpeed();
        if (windSpeed < MIN_WIND) return 0; // not enough wind to generate slope lift

        float slopeAngle = (float) Math.atan2(crestHeight, isWindward ? windwardWidth : leewardWidth);
        float magnitude = LIFT_UNIT * (float) Math.sin(slopeAngle) * lateral * heightFade * fadeAtEnds;

        return isWindward ? magnitude : -magnitude * SINK_FACTOR;
    }

    /**
     * Returns turbulence magnitude (0 = smooth air) at a point. Only
     * present on the leeward side, strongest just behind the crest and
     * close to the surface - modelling rotor / mechanical turbulence.
     */
    public float getTurbulence(float[] p) {
        float s = sAlong(p[0], p[1]);
        float d = dCross(p[0], p[1]);
        float terrainH = terrainHeightAt(d);
        float hAgl = p[2] - terrainH;
        float effectHeight = crestHeight * LIFT_HEIGHT_FACTOR;

        if (hAgl < 0 || hAgl > effectHeight) return 0;

        boolean onPosSide = (d >= 0);
        boolean isWindward = (onPosSide == windwardIsPos);
        if (isWindward) return 0; // turbulence is a leeward phenomenon

        float sideWidth = onPosSide ? widthPos : widthNeg;
        if (sideWidth <= 0) return 0;
        float lateral = 1 - Math.abs(d) / sideWidth;
        if (lateral < 0) lateral = 0;

        float heightFade = 1 - hAgl / effectHeight;
        float fadeAtEnds = endFade(s);
        if (crossWindSpeed() < MIN_WIND) return 0;

        return LIFT_UNIT * TURBULENCE_FACTOR * lateral * heightFade * fadeAtEnds;
    }

    /** Ridges are always "active" (no lifecycle like clouds). */
    public boolean isActive() { return true; }
    public boolean isActive(float t) { return true; }

    /** Approximate max lift, used by AI to compare lift sources. */
    public float getLift() {
        if (crossWindSpeed() < MIN_WIND) return 0;
        float slopeAngle = (float) Math.atan2(crestHeight, windwardWidth);
        return LIFT_UNIT * (float) Math.sin(slopeAngle);
    }

    /** Midpoint of the ridge crest - used as a reference point/camera focus. */
    public float[] getP() {
        return new float[] {(x1 + x2) / 2, (y1 + y2) / 2, crestHeight};
    }

    /**
     * Returns a circuit for AI gliders to ridge-soar back and forth along
     * the windward lift band, parallel to the crest line.
     *
     * The offset is measured out from the crest line on the windward side,
     * clear of the slope itself (beyond windwardWidth) so gliders fly in
     * open air alongside the hill rather than clipping through it.
     */
    Circuit getCircuit() {
        float windwardWidthActual = windwardIsPos ? widthPos : widthNeg;
        // fly a safe distance out beyond the base of the windward slope
        float offset = windwardWidthActual + crestHeight * 0.5f;
        float sign = windwardIsPos ? 1 : -1;
        float ox = perpX * offset * sign;
        float oy = perpY * offset * sign;
        float pad = length * 0.2f; // keep away from the very ends (lift fades there anyway)

        Circuit c = new Circuit(2);
        c.add(new float[] {x1 + dirX * pad + ox, y1 + dirY * pad + oy, 0});
        c.add(new float[] {x1 + dirX * (length - pad) + ox, y1 + dirY * (length - pad) + oy, 0});
        return c;
    }

    static final Color WINDWARD_COLOR = new Color(140, 125, 95);  // sunlit slope
    static final Color LEEWARD_COLOR = new Color(90, 80, 65);     // shaded slope

    // How many segments to chop each slope into along its length. A single
    // large polygon can vanish entirely when the camera gets close (this
    // engine culls a whole polygon if any one vertex falls outside the
    // near view), so we subdivide - same approach as Terrain.java's grid.
    static final int SEGMENTS = 8;

    /** Builds the 3d wedge geometry representing this ridge. */
    void renderMe() {
        if (obj3d != null) {
            obj3d.destroyMe();
        }
        // 2 slopes * SEGMENTS quads, plus 2 end caps
        obj3d = new Obj3d(xcModelViewer, SEGMENTS * 2 + 2, true);

        float windSign = windwardIsPos ? 1 : -1;
        float leeSign = -windSign;

        float[] crestStart = {x1, y1, crestHeight};
        float[] crestEnd = {x2, y2, crestHeight};
        float[] windBaseStart = {x1 + perpX * windwardWidth * windSign, y1 + perpY * windwardWidth * windSign, 0};
        float[] windBaseEnd = {x2 + perpX * windwardWidth * windSign, y2 + perpY * windwardWidth * windSign, 0};
        float[] leeBaseStart = {x1 + perpX * leewardWidth * leeSign, y1 + perpY * leewardWidth * leeSign, 0};
        float[] leeBaseEnd = {x2 + perpX * leewardWidth * leeSign, y2 + perpY * leewardWidth * leeSign, 0};

        // windward slope, subdivided along the ridge length
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = (float) i / SEGMENTS;
            float f1 = (float) (i + 1) / SEGMENTS;
            float[] c0 = lerp(crestStart, crestEnd, f0);
            float[] c1 = lerp(crestStart, crestEnd, f1);
            float[] b0 = lerp(windBaseStart, windBaseEnd, f0);
            float[] b1 = lerp(windBaseStart, windBaseEnd, f1);
            obj3d.addPolygon2(new float[][] {b0, b1, c1, c0}, WINDWARD_COLOR);
        }

        // leeward slope, subdivided along the ridge length
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = (float) i / SEGMENTS;
            float f1 = (float) (i + 1) / SEGMENTS;
            float[] c0 = lerp(crestStart, crestEnd, f0);
            float[] c1 = lerp(crestStart, crestEnd, f1);
            float[] b0 = lerp(leeBaseStart, leeBaseEnd, f0);
            float[] b1 = lerp(leeBaseStart, leeBaseEnd, f1);
            obj3d.addPolygon2(new float[][] {c0, c1, b1, b0}, LEEWARD_COLOR);
        }

        // end caps
        obj3d.addPolygon2(new float[][] {windBaseStart, crestStart, leeBaseStart}, LEEWARD_COLOR);
        obj3d.addPolygon2(new float[][] {windBaseEnd, crestEnd, leeBaseEnd}, LEEWARD_COLOR);
    }

    private static float[] lerp(float[] a, float[] b, float f) {
        return new float[] {
            a[0] + (b[0] - a[0]) * f,
            a[1] + (b[1] - a[1]) * f,
            a[2] + (b[2] - a[2]) * f
        };
    }

    void asString() {
        System.out.println("Ridge: (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + "), crest=" + crestHeight);
    }
}

package cz.cuni.lf1.lge.ThunderSTORM.datagen;

import cz.cuni.lf1.lge.ThunderSTORM.util.MathProxy;

public class Drift {

    public double dist;     // [pixels]
    public double angle;    // [radians]
    public double dist_step;    // assuming linear drift

    public Drift(double dist, double angle, boolean angle_in_rad, int nframes) {
        this.dist = dist;
        if(angle_in_rad) {
            this.angle = angle;
        } else {
            this.angle = angle / 180.0 * MathProxy.PI;
        }
        if(nframes > 1) {
            dist_step = dist / (double)(nframes-1); // nframes-1, because there is no drift in the first frame
        } else {
            dist_step = 0.0;
        }
    }

    public double getDriftX(int frame) {    // indexing from zero
        return ((double)frame * dist_step) * MathProxy.cos(angle);
    }

    public double getDriftY(int frame) {    // indexing from zero
        return ((double)frame * dist_step) * MathProxy.sin(angle);
    }

}
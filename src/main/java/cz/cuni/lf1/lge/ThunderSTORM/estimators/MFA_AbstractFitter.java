package cz.cuni.lf1.lge.ThunderSTORM.estimators;

import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.Molecule;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel;
import cz.cuni.lf1.lge.ThunderSTORM.util.MathProxy;

import java.util.Vector;

abstract public class MFA_AbstractFitter implements IOneLocationFitter {

    double defaultSigma;
    PSFModel basePsfModel;
    int maxN;

    public MFA_AbstractFitter(PSFModel basePsfModel, double defaultSigma, int maxN) {
        assert (maxN >= 1);
        //
        this.defaultSigma = defaultSigma;
        this.basePsfModel = basePsfModel;
        this.maxN = maxN;
    }

    // get rid of the molecules close to the fiting region boundary
    protected Molecule eliminateBadFits(Molecule mol, double maxX, double maxY) {
        if(!mol.isSingleMolecule()) {
            Vector<Molecule> detections = new Vector<Molecule>();
            for(Molecule m : mol.getDetections()) {
                if((MathProxy.abs(m.getX()) <= maxX) || (MathProxy.abs(m.getY()) <= maxY)) {
                    detections.add(m);
                }
            }
            mol.setDetections(detections);
        }
        return mol;
    }

    protected boolean isOutOfRegion(Molecule mol, double maxXY) {
        if((MathProxy.abs(mol.getX()) > maxXY) || (MathProxy.abs(mol.getY()) > maxXY)) {
            return true;
        }
        if(!mol.isSingleMolecule()) {
            for(Molecule m : mol.getDetections()) {
                if((MathProxy.abs(m.getX()) > maxXY) || (MathProxy.abs(m.getY()) > maxXY)) {
                    return true;
                }
            }
        }
        return false;
    }
}

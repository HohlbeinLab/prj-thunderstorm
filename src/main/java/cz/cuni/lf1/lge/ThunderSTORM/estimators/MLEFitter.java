package cz.cuni.lf1.lge.ThunderSTORM.estimators;

import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.Molecule;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.MoleculeDescriptor;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel.Params;
import cz.cuni.lf1.lge.ThunderSTORM.util.VectorMath;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

public class MLEFitter implements IOneLocationFitter, IOneLocationBiplaneFitter {
    public final static int MAX_ITERATIONS = 50000;

    public double[] fittedParameters;
    public PSFModel psfModel;

    private final int maxIter;
    private final int bkgStdColumn;

    public MLEFitter(PSFModel psfModel) {
        this(psfModel, MAX_ITERATIONS + 1, -1);
    }

    public MLEFitter(PSFModel psfModel, int bkgStdIndex) {
        this(psfModel, MAX_ITERATIONS + 1, bkgStdIndex);
    }

    public MLEFitter(PSFModel psfModel, int maxIter, int bkgStdIndex) {
        this.psfModel = psfModel;
        this.maxIter = maxIter;
        this.bkgStdColumn = bkgStdIndex;
        this.fittedParameters = null;
    }

    @Override
    public Molecule fit(SubImage subimage) {
        subimage.convertTo(MoleculeDescriptor.Units.PHOTON);
        return fit(new LsqMleSinglePlaneFunctions(psfModel, subimage));
    }

    @Override
    public Molecule fit(SubImage plane1, SubImage plane2) {
        plane1.convertTo(MoleculeDescriptor.Units.PHOTON);
        plane2.convertTo(MoleculeDescriptor.Units.PHOTON);
        return fit(new LsqMleBiplaneFunctions(psfModel, plane1, plane2));
    }

    public Molecule fit(IMleFunctions functions) {
        // init
        double[] observations = functions.getObservations();

        // fit
        SimplexOptimizer optimizer = new SimplexOptimizer(1.0e-8, 1.0e-8);
        PointValuePair optimum = optimizer.optimize(
                new MaxEval(Integer.MAX_VALUE),
                new MaxIter(maxIter),
                new ObjectiveFunction(functions.getLikelihoodFunction()),
                GoalType.MAXIMIZE,
                new InitialGuess(psfModel.transformParametersInverse(functions.getInitialParams())),
                new NelderMeadSimplex(psfModel.getInitialSimplex()));

        fittedParameters = optimum.getPointRef();

        // estimate background and return an instance of the `Molecule`
        fittedParameters[Params.BACKGROUND] = VectorMath.stddev(VectorMath.sub(observations, functions.getValueFunction().value(fittedParameters)));

        Molecule mol = psfModel.newInstanceFromParams(psfModel.transformParameters(fittedParameters), functions.getImageUnits(), true);

        if (mol.isSingleMolecule()) {
            convertMoleculeToDigitalUnits(mol);
        } else {
            for (Molecule detection : mol.getDetections()) {
                convertMoleculeToDigitalUnits(detection);
            }
        }
        return mol;
    }

    private void convertMoleculeToDigitalUnits(Molecule mol) {
        for (String param : mol.descriptor.names) {
            MoleculeDescriptor.Units paramUnits = mol.getParamUnits(param);
            MoleculeDescriptor.Units digitalUnits = MoleculeDescriptor.Units.getDigitalUnits(paramUnits);
            if (!digitalUnits.equals(paramUnits)) {
                mol.setParam(param, digitalUnits, mol.getParam(param, digitalUnits));
            }
        }
    }
}

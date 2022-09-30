package cz.cuni.lf1.lge.ThunderSTORM.estimators;

import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.Molecule;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel.Params;
import cz.cuni.lf1.lge.ThunderSTORM.util.VectorMath;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.DiagonalMatrix;

import static cz.cuni.lf1.lge.ThunderSTORM.util.VectorMath.sub;

public class LSQFitter implements IOneLocationFitter, IOneLocationBiplaneFitter {
    public final static int MAX_ITERATIONS = 1000;

    public double[] fittedParameters;
    public PSFModel psfModel;
    public boolean useWeighting;

    private final int maxIter; // after `maxIter` iterations the algorithm converges
    private final int bkgStdColumn;

    public LSQFitter(PSFModel psfModel, boolean useWeighting) {
        this(psfModel, useWeighting, MAX_ITERATIONS + 1, -1);
    }

    public LSQFitter(PSFModel psfModel, boolean useWeighting, int bkgStdIndex) {
        this(psfModel, useWeighting, MAX_ITERATIONS + 1, Params.BACKGROUND);
    }

    public LSQFitter(PSFModel psfModel, boolean useWeighting, int maxIter, int bkgStdIndex) { // throws an exception after `MAX_ITERATIONS` iterations
        this.psfModel = psfModel;
        this.maxIter = maxIter;
        this.useWeighting = useWeighting;
        this.bkgStdColumn = bkgStdIndex;
        this.fittedParameters = null;
    }

    @Override
    public Molecule fit(SubImage img) {
        return fit(new LsqMleSinglePlaneFunctions(psfModel, img));
    }

    @Override
    public Molecule fit(SubImage plane1, SubImage plane2) {
        return fit(new LsqMleBiplaneFunctions(psfModel, plane1, plane2));
    }

    protected Molecule fit(ILsqFunctions functions) {
        // init
        double[] weights = functions.calcWeights(useWeighting);
        double[] observations = functions.getObservations();

        // fit
        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer()
                .withCostRelativeTolerance(1.0e-2)
                .withParameterRelativeTolerance(1.0e-2);

        LeastSquaresProblem leastSquaresProblem = new LeastSquaresBuilder()
                .start(psfModel.transformParametersInverse(functions.getInitialParams()))
                .target(observations)
                .model(functions.getValueFunction(), functions.getJacobianFunction())
                .lazyEvaluation(false)
                .maxEvaluations(Integer.MAX_VALUE)
                .maxIterations(maxIter)
                .weight(new DiagonalMatrix(weights))
                .build();

        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(leastSquaresProblem);

        // estimate background and return an instance of the `Molecule`
        fittedParameters = optimum.getPoint().toArray();
        if (bkgStdColumn >= 0) {
            fittedParameters[bkgStdColumn] = VectorMath.stddev(sub(observations, functions.getValueFunction().value(fittedParameters)));
        }
        return psfModel.newInstanceFromParams(psfModel.transformParameters(fittedParameters), functions.getImageUnits(), true);
    }
}

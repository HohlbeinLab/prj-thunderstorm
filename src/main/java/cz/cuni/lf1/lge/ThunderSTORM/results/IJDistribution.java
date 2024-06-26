package cz.cuni.lf1.lge.ThunderSTORM.results;

import cz.cuni.lf1.lge.ThunderSTORM.util.MathProxy;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import ij.util.Tools;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Vector;

/**
 * This plugin implements the Analyze/Distribution command. It reads the data
 * from the ResultsTable and plots a frequency histogram.
 *
 * @author G. Landini at bham. ac. uk
 */
public class IJDistribution implements PlugIn, TextListener {

    static String parameter = "Area";
    static boolean autoBinning = true;
    static int nBins = 10;
    static String range = "0-0";
    Checkbox checkbox;
    TextField nBinsField, rangeField;
    String defaultNBins, defaultRange;

    @Override
    public void run(String arg) {
        GenericTable table;
        if("ground-truth".equals(arg)) {
            table = IJGroundTruthTable.getGroundTruthTable();
        } else {
            table = IJResultsTable.getResultsTable();
        }
        int count = table.getRowCount();
        if(count == 0) {
            IJ.error("ThunderSTORM: Distribution", "The \"" + table.getFrameTitle() + "\" table is empty");
            return;
        }
        //IJ.log(head);

        String[] strings = (String[])table.getColumnNames().toArray(new String[0]);

        defaultNBins = "" + nBins;
        defaultRange = range;
        GenericDialog gd = new GenericDialog("Distribution");
        gd.addChoice("Parameter: ", strings, strings[getIndex(strings)]);
        gd.addMessage("Data points: " + count);
        gd.addCheckbox("Automatic binning", autoBinning);
        gd.addNumericField("or specify bins:", nBins, 0);
        gd.addStringField("and range:", range);

        Vector v = gd.getNumericFields();
        nBinsField = (TextField) v.elementAt(0);
        nBinsField.addTextListener(this);
        v = gd.getStringFields();
        rangeField = (TextField) v.elementAt(0);
        rangeField.addTextListener(this);
        checkbox = (Checkbox) (gd.getCheckboxes().elementAt(0));
        gd.showDialog();
        if(gd.wasCanceled()) {
            return;
        }

        parameter = gd.getNextChoice();
        autoBinning = gd.getNextBoolean();
        double nMin = 0.0, nMax = 0.0;
        if(!autoBinning) {
            nBins = (int) gd.getNextNumber();
            range = gd.getNextString();
            String[] minAndMax = Tools.split(range, " -");
            nMin = Tools.parseDouble(minAndMax[0]);
            nMax = minAndMax.length == 2 ? Tools.parseDouble(minAndMax[1]) : Double.NaN;
            if(Double.isNaN(nMin) || Double.isNaN(nMax)) {
                nMin = 0.0;
                nMax = 0.0;
                range = "0-0";
            }
        }
        double[] data;
        if(table.columnExists(parameter)) {
            data = table.getColumnAsDoubles(parameter);
        } else {
            IJ.error("Distribution", "No available results: \"" + parameter + "\"");
            return;
        }

        double[] pars = new double[11];
        stats(count, data, pars);
        if(autoBinning) {
            //sd = 7, min = 3, max = 4
            // use Scott's method (1979 Biometrika, 66:605-610) for optimal binning: 3.49*sd*N^-1/3
            float binWidth = (float) (3.49 * pars[7] * (float) MathProxy.pow((float) count, -1.0 / 3.0));
            nBins = (int) MathProxy.floor(((pars[4] - pars[3]) / binWidth) + .5);
            if(nBins < 2) {
                nBins = 2;
            }
        }

        ImageProcessor ip = new FloatProcessor(count, 1, data);
        ImagePlus imp = new ImagePlus("", ip);
        ImageStatistics stats = new StackStatistics(imp, nBins, nMin, nMax);
        int maxCount = 0;
        for(int i = 0; i < stats.histogram.length; i++) {
            if(stats.histogram[i] > maxCount) {
                maxCount = stats.histogram[i];
            }
        }
        stats.histYMax = maxCount;
        new IJHistogramWindow(parameter, parameter + " Distribution", imp, stats);
    }

    int getIndex(String[] strings) {
        for(int i = 0; i < strings.length; i++) {
            if(strings[i].equals(parameter)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void textValueChanged(TextEvent e) {
        if(!defaultNBins.equals(nBinsField.getText())) {
            checkbox.setState(false);
        }
        if(!defaultRange.equals(rangeField.getText())) {
            checkbox.setState(false);
        }
    }

    void stats(int nc, double[] data, double[] pars) {
        // ("\tPoints\tEdges_n\tGraph_Length\tMin\tMax\tMean\tAvDev\tSDev\tVar\tSkew\tKurt");
        int i;
        double s = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE, totl = 0, ave = 0, adev = 0, sdev = 0, var = 0, skew = 0, kurt = 0, p;

        for(i = 0; i < nc; i++) {
            totl += data[i];
            //tot& = tot& + 1
            if(data[i] < min) {
                min = data[i];
            }
            if(data[i] > max) {
                max = data[i];
            }
        }

        ave = totl / nc;

        for(i = 0; i < nc; i++) {
            s = data[i] - ave;
            adev += MathProxy.abs(s);
            p = s * s;
            var += p;
            p *= s;
            skew += p;
            p *= s;
            kurt += p;
        }

        adev /= nc;
        var /= nc - 1;
        sdev = (float) MathProxy.sqrt(var);

        if(var > 0) {
            skew = (float) skew / (nc * (float) MathProxy.pow(sdev, 3));
            kurt = (float) kurt / (nc * (float) MathProxy.pow(var, 2)) - 3;
        }
        pars[1] = (float) nc;
        pars[2] = totl;
        pars[3] = min;
        pars[4] = max;
        pars[5] = ave;
        pars[6] = adev;
        pars[7] = sdev;
        pars[8] = var;
        pars[9] = skew;
        pars[10] = kurt;

    }
}

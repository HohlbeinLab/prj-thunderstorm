package cz.cuni.lf1.lge.ThunderSTORM.detectors;

import cz.cuni.lf1.lge.ThunderSTORM.detectors.ui.IDetectorUI;
import cz.cuni.lf1.lge.ThunderSTORM.FormulaParser.FormulaParserException;
import cz.cuni.lf1.lge.ThunderSTORM.thresholding.Thresholder;
import cz.cuni.lf1.lge.ThunderSTORM.util.Graph;
import cz.cuni.lf1.lge.ThunderSTORM.util.GridBagHelper;
import static cz.cuni.lf1.lge.ThunderSTORM.util.ImageProcessor.applyMask;
import static cz.cuni.lf1.lge.ThunderSTORM.util.ImageProcessor.threshold;
import cz.cuni.lf1.lge.ThunderSTORM.util.Point;
import ij.Macro;
import ij.Prefs;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.Recorder;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.GridBagLayout;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Look for pixels with their intensities equal or greater then a threshold and
 * if there are more of these pixels connected together account them as a single
 * molecule, unless a shape of these connected pixels indicates that there is in
 * fact more of them.
 */
public final class CentroidOfConnectedComponentsDetector extends IDetectorUI implements IDetector {

    private final String name = "Centroid of connected components";
    private String threshold;
    private boolean useWatershed;
    private transient float thresholdValue;
    private transient JTextField thrTextField;
    private transient JCheckBox watershedCheckBox;
    private transient final static String DEFAULT_THRESHOLD = "std(I-Wave.V1)";
    private transient final static boolean DEFAULT_USE_WATERSHED = true;

    public CentroidOfConnectedComponentsDetector() throws FormulaParserException {
        this(DEFAULT_THRESHOLD);
    }

    /**
     * Filter initialization.
     *
     * @param threshold a threshold value of intensity
     */
    public CentroidOfConnectedComponentsDetector(String threshold) throws FormulaParserException {
        this.threshold = threshold;
    }

    /**
     * Detection algorithm works simply by setting all values lower than a
     * threshold to zero, splitting close peaks by watershed and finding
     * centroids of connected components.
     *
     * In more detail this is how it is done:
     * <ol>
     * <li>apply the threshold to get thresholded binary image</li>
     * <li>in the original image, set intensity to zero, where the thresholded
     * image is zero. Leave the grayscale value otherwise.
     * </li>
     * <li>
     * perform a watershed transform (this is the trick for recognition of more
     * connected molecules
     * </li>
     * <li>AND the thresholded image with watershed image</li>
     * <li>
     * then we think of the resulting image as an undirected graph with
     * 8-connectivity and find all connected components with the same id
     * </li>
     * <li>
     * finally, positions of molecules are calculated as centroids of components
     * with the same id
     * </li>
     * </ol>
     *
     * @param image an input image
     * @return a {@code Vector} of {@code Points} containing positions of
     * detected molecules
     */
    @Override
    public Vector<Point> detectMoleculeCandidates(FloatProcessor image) throws FormulaParserException {

        //keep a local threshold value so the method remains thread safe
        float localThresholdValue = Thresholder.getThreshold(threshold);
        thresholdValue = localThresholdValue;   //publish the calculated threshold,(not thread safe but only used for preview logging)
        FloatProcessor thresholdedImage = (FloatProcessor) image.duplicate();
        threshold(thresholdedImage, localThresholdValue, 0.0f, 255.0f);

        FloatProcessor maskedImage = applyMask(image, thresholdedImage);

        if(useWatershed) {
            ImageJ_MaximumFinder watershedImpl = new ImageJ_MaximumFinder();
            ByteProcessor watershedImage = watershedImpl.findMaxima(maskedImage, 0, ImageProcessor.NO_THRESHOLD, MaximumFinder.SEGMENTED, false, false);
            FloatProcessor thresholdImageANDWatershedImage = applyMask(thresholdedImage, (FloatProcessor) watershedImage.convertToFloat());
            maskedImage = thresholdImageANDWatershedImage;
        }
        // finding a center of gravity (with subpixel precision)
        Vector<Point> detections = new Vector<Point>();
        for(Graph.ConnectedComponent c : Graph.getConnectedComponents((ImageProcessor) maskedImage, Graph.CONNECTIVITY_8)) {
            detections.add(c.centroid());
            detections.lastElement().val = null;
        }
        return detections;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JPanel getOptionsPanel() {
        thrTextField = new JTextField(Prefs.get("thunderstorm.detectors.centroid.thr", DEFAULT_THRESHOLD), 20);
        watershedCheckBox = new JCheckBox("enable", Prefs.get("thunderstorm.detectors.centroid.watershed", DEFAULT_USE_WATERSHED));
        //
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("Peak intensity threshold:"), GridBagHelper.leftCol());
        panel.add(thrTextField, GridBagHelper.rightCol());
        panel.add(new JLabel("Watershed segmentation:"), GridBagHelper.leftCol());
        panel.add(watershedCheckBox, GridBagHelper.rightCol());
        return panel;
    }

    @Override
    public void readParameters() {
        threshold = thrTextField.getText();
        useWatershed = watershedCheckBox.isSelected();

        Prefs.set("thunderstorm.detectors.centroid.thr", threshold);
        Prefs.set("thunderstorm.detectors.centroid.watershed", useWatershed);
    }

    @Override
    public IDetector getImplementation() {
        return this;
    }

    @Override
    public void recordOptions() {
        if(!DEFAULT_THRESHOLD.equals(threshold)) {
            Recorder.recordOption("threshold", threshold);
        }
        if(!DEFAULT_USE_WATERSHED == useWatershed) {
            Recorder.recordOption("watershed", useWatershed + "");
        }
    }

    @Override
    public void readMacroOptions(String options) {
        threshold = Macro.getValue(options, "threshold", DEFAULT_THRESHOLD);
        useWatershed = Boolean.parseBoolean(Macro.getValue(options, "watershed", DEFAULT_USE_WATERSHED + ""));
    }

    @Override
    public void resetToDefaults() {
        thrTextField.setText(DEFAULT_THRESHOLD);
        watershedCheckBox.setSelected(DEFAULT_USE_WATERSHED);
    }

    @Override
    public String getThresholdFormula() {
        return threshold;
    }

    @Override
    public float getThresholdValue() {
        return thresholdValue;
    }
}

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * project: ParallelogramDetection
 *
 * @author YubaiTao on 10/11/2017.
 *
 */
public class ParallelogramDetector {
    /* Fields */
    private static final int MIN_AREA = 100;
    private final String id;
    private int[][] image;
    private int[][] grayImage;
    private int[][] houghTable;
    private int width;
    private int height;

    private ImageProcessor processor;
    private int[][] localizedHoughTable;

    private TreeMap<Integer, List<Line>> thetaMap;

    /* Constructor */
    public ParallelogramDetector(ImageProcessor processor) {
        this.processor = processor;
        this.image = processor.getImage();
        this.grayImage = processor.getGrayImage();
        this.houghTable = processor.getHoughTable();
        this.width = image[0].length;
        this.height = image.length;
        this.localizedHoughTable = new int[houghTable.length][houghTable[0].length];
        this.id = processor.id;

        this.thetaMap = new TreeMap<>();
    }

    /* First reduce the line that with non-local-maximum accumulator value.
       Then set a threshold to get rid of the fake lines with too few pixels(points).
     */
    public void lineDistinction(int scale, int threshold) {
        localizeHoughTable(scale);
        // List<Line> lineList = ImageProcessor.extractLines(localizedHoughTable,10);
        // processor.drawLines(lineList, grayImage, "./OutputImages/" + id + "_distinct.jpg");
        List<Line> lineList = ImageProcessor.extractAllLines(localizedHoughTable, threshold);
        processor.drawLines(lineList, grayImage, "./OutputImages/" + id + "_allLines.jpg");
    }

    private void localizeHoughTable(int scale) {
        for (int m = 0; m < houghTable.length; m++) {
            for (int n = 0; n < houghTable[0].length; n++) {
                if (isDistinctInNeighbours(m, n, 3)) {
                    localizedHoughTable[m][n] = houghTable[m][n];
                } else {
                    localizedHoughTable[m][n] = 0;
                }
            }
        }
    }

    /* scale = 3 ; then (p, theta) is max in 3x3 cell */
    private boolean isDistinctInNeighbours(int i, int j, int scale) {
        int max = houghTable[i][j];
        int cornerI = i - scale / 2;
        int cornerJ = j - scale / 2;
        for (int m = 0; m < scale; m++) {
            for (int n = 0; n < scale; n++) {
                if (cornerI >= 0 && cornerI + scale < houghTable.length &&
                        cornerJ >= 0 && cornerJ + scale < houghTable[0].length) {
                    if (houghTable[cornerI + m][cornerJ + n] > max) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /* put the lines into the map */
    private void mapLines(List<Line> lineList) {

        int thetaStrides = houghTable.length;
        double maxTheta = 2 * Math.PI;
        double thetaStride = maxTheta / thetaStrides;

        int pStrides = houghTable[0].length;
        double maxP = Math.sqrt(image.length * image.length + image[0].length * image[0].length);
        double pStride = maxP / pStrides;

        /* map lines by theta index */
        for (Line l : lineList) {
            if ( !thetaMap.containsKey(l.thetaIndex)) {
                List<Line> curList = new LinkedList<>();
                curList.add(l);
                thetaMap.put(l.thetaIndex, curList);
            } else {
                List<Line> curList = thetaMap.get(l.thetaIndex);
                curList.add(l);
                // thetaMap.replace(l.thetaIndex, curList);
            }
        }


    }



}

class Pair {

}

class Parallelogram {

}


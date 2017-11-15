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
    private List<Line> originalLines;

    private TreeMap<Integer, List<Line>> thetaMap;
    private TreeMap<Integer, List<Pair>> pairMap;

    private List<Parallelogram> parallelogramsList;

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

        this.originalLines = new LinkedList<>();

        this.thetaMap = new TreeMap<>();
        this.pairMap = new TreeMap<>();

        this.parallelogramsList = new LinkedList<>();
    }

    /* ---- public methods ---- */

    /* First reduce the line that with non-local-maximum accumulator value.
       Then set a threshold to get rid of the fake lines with too few pixels(points).
     */
    public void lineDistinction(int scale, int threshold) {
        localizeHoughTable(scale);
        // List<Line> lineList = ImageProcessor.extractLines(localizedHoughTable,10);
        // processor.drawLines(lineList, grayImage, "./OutputImages/" + id + "_distinct.jpg");
        originalLines = ImageProcessor.extractAllLines(localizedHoughTable, threshold);
        processor.drawLines(originalLines, grayImage, "./OutputImages/" + id + "_allLines.jpg");
    }

    public void setUpParallelograms() {
        mapLines(originalLines);
        getIntoPairs();
        constructParallelograms();

        for (Parallelogram p : parallelogramsList) {
            drawParallelogram(p, processor.originalImage);
            break;
        }
        String path = "./OutputImages/" + id + "_parallelograms.jpg";
        processor.drawImage(processor.originalImage, path);


//        int[][] curImage = new int[image.length][image[0].length];
//        String path = "./OutputImages/" + id + "_dotssssssssssss.jpg";
//        for (int i = 0; i < 4; i++) {
//            int iValue = parallelogramsList.get(218).intersectDots[i][0];
//            int jValue = parallelogramsList.get(218).intersectDots[i][1];
//            processor.drawDot(iValue, jValue, curImage, 20);
//        }
//        processor.drawImage(curImage, path);

//        for (int i = 0; i < parallelogramsList.size(); i++) {
//            List<Line> lineList = parallelogramsList.get(i).lineList;
//            int[][] curImage = new int[image.length][image[0].length];
//            String path = "./OutputImages/" + id + "_lines_" + i + ".jpg";
//            processor.drawLines(lineList, curImage, path);
//        }

    }


    /* ---- private methods ---- */

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

        System.out.println("[PD]    mapLines finished. ----");

    }

    /* set pair map from theta map */
    private void getIntoPairs() {
        for (Map.Entry<Integer, List<Line>> entry : thetaMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<Pair> pairs;
                pairs = reducePairs(formPairs(entry.getValue()), 40);
                if (pairs.size() > 0) {
                    pairMap.put(entry.getKey(), pairs);
                }
            }
        }

        System.out.println("[PD]    getIntoPairs finished. ----");
    }

    /* get all pair combinations in a list of parallel lines */
    private List<Pair> formPairs(List<Line> lineList) {
        // line in the lineList all has same theta
        List<Pair> pairs = new LinkedList<>();
        for (int i = 0; i < lineList.size(); i++) {
            for (int j = i + 1; j < lineList.size(); j++) {
                Pair curPair = new Pair(lineList.get(i), lineList.get(j));
                pairs.add(curPair);
            }
        }

        System.out.println("[PD]    formPairs. ----");

        return pairs;
    }

    /*
     * reduce pairs that are actually same edge line
     */
    private List<Pair> reducePairs(List<Pair> pairList, int pBias) {
        List<Pair> newList = new LinkedList<>();
        for(Pair pair : pairList) {
            Line l1 = pair.line1;
            Line l2 = pair.line2;
            double para1[] = new double[2];
            double para2[] = new double[2];
            convert(l1, para1);
            convert(l2, para2);
            if (Math.abs( para1[1] - para2[1]) > pBias) {// 40
                newList.add(pair);
            }
        }

        System.out.println("[PD]    reducePairs. ----");

        return newList;
    }

    /* construct parallelograms with every theta combination */
    private List<Parallelogram> constructParallelograms() {
        List<Parallelogram> pList = new LinkedList<>();
        Set<Integer> thetaKeys = pairMap.keySet();
        Integer[] thetaArray = thetaKeys.toArray(new Integer[thetaKeys.size()]);
        for (int i = 0; i < pairMap.size(); i++) {
            for (int j = i + 1; j < pairMap.size(); j++) {
                appendParallelograms(pairMap.get(thetaArray[i]), pairMap.get(thetaArray[j]));
            }
        }

        System.out.println("[PD]    constructParallelograms. ----");

        return pList;
    }

    /* append the parallelograms generated from theta combination,
       (in two List<Pair> form)
     */
    private void appendParallelograms(List<Pair> group1, List<Pair> group2) {
        for (int i = 0; i < group1.size(); i++) {
            for (int j = 0; j < group2.size(); j++) {
                Parallelogram pg = new Parallelogram(group1.get(i), group2.get(j), this);
                if (checkValidity(pg)) {
                    parallelogramsList.add(pg);
                }
            }
        }


    }

    private boolean checkValidity(Parallelogram pg) {
        /*
        First pass: if all vertices are in the image ?
        Second pass: if all four border are valid ?
          sub-second: percent of the points int the border
        Third pass: if the area of the parallelogram is too small to make sense ?
         */
        // First pass:
        int[][] vertices = pg.intersectDots;
        int height = image.length;
        int width = image[0].length;
        for (int i = 0; i < 4; i++) {
            if (vertices[i][0] < 0 || vertices[i][0] > height - 1
                    || vertices[i][1] < 0  || vertices[i][1] > width - 1) {
                return false;
            }
        }

        // Second pass:
        for (int i = 0; i < 4; i++) {
            boolean bool = checkBorder(pg.intersectDots[i],
                    i == 3 ? pg.intersectDots[0] : pg.intersectDots[i + 1],
                    pg.lineList.get(i), 2, 0.3);
            if (!bool) {
                // return false;
            }
        }

        // Third pass:

        return true;
    }

    /* check one border of the intended parallelogram,
     * bias regulates the accuracy of this check method,
     * percent measures the acceptable percent of valid/all for all points on the border.
     */
    private boolean checkBorder(int[] dot1, int[] dot2, Line line, int scale, double percent) {
        double totalPointCounter = 0;
        double borderPointCounter = 0;
        for (int i = Math.min(dot1[0], dot2[0]); i <= Math.max(dot1[0], dot2[0]); i++) {
            for (int j = Math.min(dot1[1], dot2[1]); j <= Math.max(dot1[1], dot2[1]); j++) {
                int[] cur = {i, j};
                if (distance(line, cur) < scale) {
                    totalPointCounter++;
                    if (image[i][j] != 255 ) {
                        borderPointCounter++;
                    }
                }
            }
        }

        /*
        if (totalPointCounter == 0) {
            System.out.println("line: " + line.thetaIndex + " " + line.pIndex);
            System.out.println("dots: " + dot1[0] + " " + dot1[1] + " "
                    + dot2[0] + " " + dot2[1]);
        }

        System.out.println("border point count:" + borderPointCounter);
        System.out.println("total point count:" + totalPointCounter);
        System.out.println("percent: " + (borderPointCounter/totalPointCounter));
        System.out.println();
        */

        if ((borderPointCounter / totalPointCounter) >= percent) {
            return true;
        }

        return false;
    }

    private double distance(Line line, int dot[]) {
        double[] para = new double[2];
        convert(line, para);
        double theta = para[0];
        double p = para[1];
        // p = i * cos(theta) + j * sin(theta)
        double A = Math.cos(theta);
        double B = Math.sin(theta);
        double C = -p;
        double d = Math.abs(A * dot[0] + B * dot[1] + C) / Math.sqrt(A * A + B * B);

        return d;
    }

    /* convert the p, theta from index number to the real i, j value */
    public void convert(Line l, double[] para) {
        int pIndex = l.pIndex;
        int thetaIndex = l.thetaIndex;
        double maxP = Math.sqrt(image.length * image.length + image[0].length * image[0].length);
        double pStride = maxP / houghTable[0].length;
        double maxTheta = 2 * Math.PI;
        double thetaStride = maxTheta / houghTable.length;
        double p = pStride / 2 + pIndex * pStride;
        double theta = thetaStride / 2 + thetaIndex * thetaStride;
        para[0] = theta;
        para[1] = p;
    }

    private void drawParallelogram(Parallelogram p, int[][] matrix) {
        for (int i = 0; i < 4; i++) {
            double[] para = new double[2];
            convert(p.lineList.get(i), para);
            ImageProcessor.drawLine(para, matrix, p.intersectDots[i],
                    i == 3 ? p.intersectDots[0] : p.intersectDots[i + 1]);
        }

    }


    public void test() {
        System.out.println("[PD]    theta map size: " + thetaMap.size());
        System.out.println("        - <0> size:" + thetaMap.firstEntry().getValue().size());
        System.out.println("        - <e> size:" + thetaMap.lastEntry().getValue().size());
        System.out.println("[PD]    pair map size: " + pairMap.size());
        System.out.println("        - <0> size:" + pairMap.firstEntry().getValue().size());
        System.out.println("        - <e> size:" + pairMap.lastEntry().getValue().size());
        System.out.println("[PD]    parallelogram number: " + parallelogramsList.size());
        for (Parallelogram p : parallelogramsList) {
            // System.out.println("   -: " + p.intersectDots[0][0] );
        }
    }
}


/* ---- utility class ---- */

class Pair {
    int theta;
    Line line1;
    Line line2;

    public Pair(Line line1, Line line2) {
        this.line1 = line1;
        this.line2 = line2;
        this.theta = line1.thetaIndex;
    }
}

class LineSec {

}

class Parallelogram {
    public boolean validity;
    public int[][] intersectDots;
    public List<Line> lineList;
    private Pair pair1;
    private Pair pair2;
    private ParallelogramDetector PD;
    public Parallelogram(Pair pair1, Pair pair2, ParallelogramDetector PD) {
        validity = false;
        this.pair1 = pair1;
        this.pair2 = pair2;

        this.lineList = new LinkedList<>();

        this.intersectDots = new int[4][2];// [][0] = i; [][1] = j;
        this.PD = PD;
        this.getIntersectDots(pair1, pair2);
    }

    /* get 4 intersect points from two pairs of parallel lines */
    private void getIntersectDots(Pair p1, Pair p2) {
        // form a circle order of intersect points
        getIntersectDot(p1.line1, p2.line1, intersectDots[0]);
        getIntersectDot(p1.line1, p2.line2, intersectDots[1]);
        getIntersectDot(p1.line2, p2.line2, intersectDots[2]);
        getIntersectDot(p1.line2, p2.line1, intersectDots[3]);
        // lineList's order is accord to the intersect dot pair order in the array
        lineList.add(p1.line1);
        lineList.add(p2.line2);
        lineList.add(p1.line2);
        lineList.add(p2.line1);
    }

    /* get the two lines' intersect point */
    private void getIntersectDot(Line l1, Line l2, int[] result) {
        double[] para1 = new double[2];
        double[] para2 = new double[2];
        // get the real p and theta value
        PD.convert(l1, para1);
        PD.convert(l2, para2);
        // p = i * cos(theta) + j * sin(theta)
        // a = cos(theta)  b = sin(theta)  c = -p
        double a1, b1, c1;
        double a2, b2, c2;
        a1 = Math.cos(para1[0]);
        b1 = Math.sin(para1[0]);
        c1 = - para1[1];
        a2 = Math.cos(para2[0]);
        b2 = Math.sin(para2[0]);
        c2 = - para2[1];
        double denominator = a1 * b2 - a2 * b1;
        double i = (c2 * b1 - c1 * b2) / denominator;
        double j = (c1 * a2 - c2 * a1) / denominator;
        int iInt = (int)i;
        int jInt = (int)j;
        result[0] = iInt;
        result[1] = jInt;
    }


}




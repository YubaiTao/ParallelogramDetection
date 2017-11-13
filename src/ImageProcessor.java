import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * project: ParallelogramDetection
 *
 * @author YubaiTao on 29/10/2017.
 *
 *
 */
public class ImageProcessor {
    /* ---- Fields ---- */

    public final String id;
    private int[][] image;
    private int[][] grayImage;
    private int width;
    private int height;

    /* for gaussian blur */
    private static int[][] gaussianImage;

    /* for non-maximum suppression */
    private int[][] matrixGx;
    private int[][] matrixGy;
    // NOTICE:
    //     This is calculated in x-y coordinate system
    //     theta(gradient angle) = arctan(Gy / Gx)
    // which x increasing "right"-direction; y increasing "down"-direction
    private double[][] matrixGAngle;
    private int[][] nmsImage;

    /* for hough transform */
    private int thetaStrides;
    private int pStrides;
    /* Save sin and cos values for future calculation. */
    private double[] sinArray;
    private double[] cosArray;
    private int[][] houghTable;

    /* for threshold */
    private int[][] t1Image;
    private int[][] t2Image;

    /* Empty Constructor */
    public ImageProcessor(int[][] image) {
        this.id = null;
        this.image = image;
    }

    /* ----  Constructor ---- */
    public ImageProcessor(int[][] matrix, String id, int thetaStrides, int pStrides) {
        this.id = id;
        this.image = matrix;
        this.width = matrix[0].length;
        this.height = matrix.length;

        this.gaussianImage = new int[height][width];

        this.matrixGx = new int[height][width];
        this.matrixGy = new int[height][width];
        this.matrixGAngle = new double[height][width];
        this.nmsImage = new int[height][width];

        this.thetaStrides = thetaStrides;
        this.pStrides = pStrides;
        sinArray = new double[this.thetaStrides];
        cosArray = new double[this.thetaStrides];
        houghTable = new int[thetaStrides][pStrides];
        grayImage = image.clone();

        this.t1Image = new int[height][width];
        this.t2Image = new int[height][width];
    }

    /* --------------------- public methods ----------------------- */

    public void GaussianBlur(int sigma, int scale) {
        // G(x, y) = (1 / 2 * PI * sigma * sigma) ^ -(x * x + y * y) / 2 * sigma * sigma
        double[][] mask = new double[scale][scale];
        gaussianMask(mask, scale, sigma);
        int corner = scale / 2;
        for (int i = corner; i < height - corner; i++) {
            for (int j = corner; j < width - corner; j++) {
                int value = doGaussian(mask, scale, i, j);
                if (value > 255) {
                    gaussianImage[i][j] = 255;
                } else {
                    gaussianImage[i][j] = value;
                }
            }
        }


        String output = "./OutputImages/" + id + "_gaussian.jpg";
        drawImage(gaussianImage, output);

        image = gaussianImage.clone();

    }

    public void Sobel() {
        int threshold = 0;
        /* sobel operators */
        int[][] sobelX = {
                {1, 0, -1},
                {2, 0, -2},
                {1, 0, -1}
        };
        int[][] sobelY = {
                {1, 2, 1},
                {0, 0, 0},
                {-1, -2, -1}
        };

        int[][] result = new int[height][width];
        int i, j;
        for (i = 1; i < height - 1; i++) {
            for (j = 1; j < width - 1; j++) {
                int x = image[i - 1][j - 1] * sobelX[0][0] + image[i][j - 1] * sobelX[0][1] + image[i - 1][j + 1] * sobelX[0][2] +
                        image[i][j - 1] * sobelX[1][0] + image[i][j] * sobelX[1][1] + image[i][j + 1] * sobelX[1][2] +
                        image[i + 1][j - 1] * sobelX[2][0] + image[i][j + 1] * sobelX[2][1] + image[i + 1][j + 1] * sobelX[2][2];

                int y = image[i - 1][j - 1] * sobelY[0][0] + image[i][j - 1] * sobelY[0][1] + image[i - 1][j + 1] * sobelY[0][2] +
                        image[i][j - 1] * sobelY[1][0] + image[i][j] * sobelY[1][1] + image[i][j + 1] * sobelY[1][2] +
                        image[i + 1][j - 1] * sobelY[2][0] + image[i][j + 1] * sobelY[2][1] + image[i + 1][j + 1] * sobelY[2][2];

                matrixGx[i][j] = x;
                matrixGy[i][j] = y;

                int mag = (int) Math.sqrt(x * x + y * y);
                result[i][j] = mag;

                if (result[i][j] > 255) {
                    result[i][j] = 255;
                }

                // swap white and black
                result[i][j] = 255 - result[i][j];

                if (result[i][j] > 255 - threshold) {
                    result[i][j] = 255;
                }

                /*
                if (result[i][j] < threshold) {
                    result[i][j] = 255;
                } else {
                    result[i][j] = 0;
                } */

            }
        }


        for (i = 0; i < height; i++) {
            result[i][0] = 255;
            result[i][width - 1] = 255;
        }
        for (i = 0; i < width; i++) {
            result[0][i] = 255;
            result[height - 1][i] = 255;
        }


        String output = "./OutputImages/" + id + "_Sobel.jpg";
        drawImage(result, output);

        image = result.clone();
        nmsImage = result.clone();
    }

    /* non-maximum suppression implementation */
    public void NMS() {
        // int[][] matrixSector = new int[height][width];
        // nmsImage = image.clone();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (image[i][j] != 255) {
                    // atan2(double y, double x)
                    // y - the ordinate coordinate ("up" direction)
                    // x - the abscissa coordinate ("right" direction)
                    // so y need to convert the sign
                    // return value is radian form, [-pi, pi]
                    matrixGAngle[i][j] = Math.atan2(-matrixGy[i][j], matrixGx[i][j]);
                    int secNum = matchSector(matrixGAngle[i][j]);
                    if (!isValidEdge(secNum, i, j)) {
                        nmsImage[i][j] = 255;
                    }
                }
            }
        }



        image = nmsImage.clone();

        String output = "./OutputImages/" + id + "_nms.jpg";
        drawImage(nmsImage, output);

        System.out.println("Non-maximum suppression end for " + id + ".");
    }

    /* threshold the image, use double threshold */
    public void threshold(int t2) {
        // 8-neighbours
        int t1 = t2 / 2;
        int[][] interPixels = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                t1Image[i][j] = image[i][j];
                t2Image[i][j] = image[i][j];
                if (image[i][j] + t1 > 255) {
                    t1Image[i][j] = 255;
                }
                if (image[i][j] + t2 > 255) {
                    t2Image[i][j] = 255;
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (t2Image[i][j] != 255) {
                    checkNeighbors(interPixels, i, j);
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (interPixels[i][j] != 0 && t2Image[i][j] == 255) {
                    t2Image[i][j] = interPixels[i][j];
                }
            }
        }

        // clean the boundary of the image
        cleanBoundary(t1Image, 5);
        cleanBoundary(t2Image, 5);

        String interFile = "./OutputImages/" + id + "_interT.jpg";
        drawImage(interPixels, interFile);

        String t1File = "./OutputImages/" + id + "_t1.jpg";
        String t2File = "./OutputImages/" + id + "_t2.jpg";
        drawImage(t1Image, t1File);
        drawImage(t2Image, t2File);

        image = t2Image.clone();
        System.out.println("Threshold end for " + id + ".");
    }


    public void houghTransform() {
        fillTable();
        List<Line> lineList = extractLines(houghTable, 30);
        drawLines(lineList, grayImage, "./OutputImages/" + id +"_Lines.jpg");

        System.out.println("Hough Transform end for " + id + ".");
    }

    public int[][] getImage() {
        return image;
    }

    public int[][] getHoughTable() {
        return houghTable;
    }

    public int[][] getGrayImage() {
        return grayImage;
    }





    /* --------------------- private methods ----------------------- */

    /* ---- For GaussianBlur() --- */
    private double gaussian(int x, int y, int sigma) {
        double exponent = -(x * x + y * y) / 2 * sigma * sigma;
        double multiplier = 1 / (2 * Math.PI * sigma * sigma);

        return multiplier * Math.pow(Math.E, exponent);
    }

    // scale should be odd number
    private void gaussianMask(double[][] mask, int scale, int sigma) {
        // start at index 0
        int center = scale / 2;
        for (int i = 0; i < scale; i++) {
            for (int j = 0; j < scale; j++) {
                double cur = gaussian(i - center, j - center, sigma);
                mask[i][j] = cur;
            }
        }

    }

    private int doGaussian(double[][] mask, int scale, int i, int j) {
        int result = 0;
        int cornerI = i - (scale / 2);
        int cornerJ = j - (scale / 2);
        for (int m = 0; m < scale; m++) {
            for (int n = 0; n < scale; n++) {
                result += (int)(image[cornerI + m][cornerJ + n] * mask[m][n]);
            }
        }
        return result;
    }

    /* ---- For NMS() ---- */
    private int matchSector(double angle) {
        // angle - > [-PI, PI]
        int sectorNum = -1;
        if (angle < 0) {
            // cast to [0, 2*PI]
            angle += 2*Math.PI;
        }
        int div = (int)(angle / (Math.PI / 8));
        switch (div) {
            case 0: case 7:
            case 8: case 15: case 16:
                sectorNum = 0;
                break;
            case 1: case 2:
            case 9: case 10:
                sectorNum = 1;
                break;
            case 3: case 4:
            case 11: case 12:
                sectorNum = 2;
                break;
            case 5: case 6:
            case 13: case 14:
                sectorNum = 3;
                break;
            default:
                break;
        }

        return sectorNum;
    }

    /* judge whether a point should be suppressed */
    private boolean isValidEdge(int secNum, int i, int j) {
        // System.out.println("sec num in valid edge: " + secNum);
        switch (secNum) {
            case 0:
                return (image[i][j] <= image[i][j - 1] && image[i][j] <= image[i][j + 1]);
            case 1:
                return (image[i][j] <= image[i + 1][j - 1] && image[i][j] <= image[i - 1][j + 1]);
            case 2:
                return (image[i][j] <= image[i - 1][j] && image[i][j] <= image[i + 1][j]);
            case 3:
                return (image[i][j] <= image[i - 1][j - 1] && image[i][j] <= image[i + 1][j + 1]);
            case 4:
                return true;
        }
        return false;
    }

    /* ---- For threshold() ---- */
    private void checkNeighbors(int[][] inter, int i, int j) {
        // add connected points to the t2Image from t1Image
        if (i < 2 || j < 2 || i > width - 2 || j > height - 2) {
            return;
        }
        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 3; n++) {
                if (t2Image[i - 1 + m][j - 1 + n] == 255 && inter[i - 1 + m][j - 1 + n] == 0
                        && t1Image[i - 1 + m][j - 1 + m] != 255) {
                    inter[i - 1 + m][j - 1 + n] = t1Image[i - 1 + m][j - 1 + n];
                }
            }
        }

    }

    /* ---- For houghTransform() ---- */

    /*
        i-j coordinate system.
        theta -> [0, pi]
        p -> [0, matrix diagonal]
     */
    private void fillTable() {
        /* radian representation */
        double maxTheta = 2 * Math.PI;
        double thetaStride = maxTheta / thetaStrides;
        // double pStride = maxP / pStrides;

        for (int i = 0; i < thetaStrides; i++) {
            /* use the middle theta value for the cell in hough table. */
            double curTheta = thetaStride / 2 + i * thetaStride;
            sinArray[i] = Math.sin(curTheta);
            cosArray[i] = Math.cos(curTheta);
        }


        int pointCount = 0;
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                // 255: white    0: black
                if (image[i][j] != 255) {
                    addPoint(i, j);
                    pointCount++;
                }
            }
        }

    }


    /* do hough transform to one pixel of original picture and accumulate
       the values int the hough theta-p table.
     */
    private void addPoint(int i, int j) {
        double maxP = Math.sqrt(image.length * image.length + image[0].length * image[0].length);
        double pStride = maxP / pStrides;
        /* p = i * cos(theta) + j * sin(theta) */
        for (int m = 0; m < thetaStrides; m++) {
            double curP = i * cosArray[m] + j * sinArray[m];
            if (curP < 0 || curP > maxP) {
                continue;
            }
            houghTable[m][(int) (curP / pStride)] += 1;
        }
    }

    /* deprecated method
       use for test demonstration now
       return a list of Line by dot number on the line as priority order
    */
    public static List<Line> extractLines(int[][] table, int k) {
        // int k = 200;
        int accumulatorThreshold = 0;
        int maxValue = 0;

        List<Line> lineList = new LinkedList<>();
        PriorityQueue<Line> minHeap = new PriorityQueue<>(k, new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                if (o1.accumulator == o2.accumulator) {
                    return 0;
                }
                return o1.accumulator < o2.accumulator ? -1 : 1;
            }
        });

        int lineCount = 0;

        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                if (table[i][j] > maxValue) {
                    maxValue = table[i][j];
                }
                if (table[i][j] > accumulatorThreshold) {

                    lineCount += 1;


                    Line curLine = new Line (i, j, table[i][j]);
                    if (minHeap.size() < k) {
                        minHeap.offer(curLine);
                    } else {
                        if (curLine.accumulator > minHeap.peek().accumulator) {
                            minHeap.poll();
                            minHeap.offer(curLine);
                        }
                    }
                }
            }
        }


        int s = minHeap.size();
        for (int i = s - 1; i > -1; i--) {
            lineList.add(0, minHeap.poll());
        }

        for (Line l : lineList) {
            // System.out.print(" ****** " + l.pIndex + " " + l.thetaIndex);
        }
        // System.out.println();

        System.out.println("The total line number is : " + lineCount);

        return lineList;
    }

    /* extract all lines detected, except for some line with too few point numbers */
    public static List<Line> extractAllLines(int[][] table, int threshold) {
        List<Line> lineList = new LinkedList<>();
        int lineCounter = 0;
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                if (table[i][j] > threshold) {
                    Line curLine = new Line(i, j, table[i][j]);
                    lineList.add(curLine);
                    lineCounter += 1;
                }
            }
        }

        System.out.println("The reduced total line number is: " + lineCounter);

        return lineList;
    }



    /* ---- draw functions ---- */


    public void drawLines(List<Line> lineList, int[][] m, String path) {

        // System.out.println("Line list length : " + lineList.size());
        for (Line l : lineList) {
            drawLine(l, m);
        }

        drawImage(m, path);
    }

    private void drawLine(Line line, int[][] m) {
        int pIndex = line.pIndex;
        int thetaIndex = line.thetaIndex;
        double maxP = Math.sqrt(image.length * image.length + image[0].length * image[0].length);
        double pStride = maxP / pStrides;
        double maxTheta = 2 * Math.PI;
        double thetaStride = maxTheta / thetaStrides;
        double p = pStride / 2 + pIndex * pStride;
        double theta = thetaStride / 2 + thetaIndex * thetaStride;

//        System.out.println("Theta : " + theta);
//        System.out.println("P : " + p);

        /*
            p = i * cos(theta) + j * sin(theta)
            p / cos(theta) = i + j * tan(theta)
            i = p / cos(theta) - j * tan(theta)
         */
        for (int j = 0; j < image[0].length; j++) {
            int i = (int)( (p / Math.cos(theta)) - j * Math.tan(theta));
            if (i < 0 || i > image.length) {
                continue;
            }
            drawDot(i, j , m,1);
        }

    }

    private void drawDot(int i, int j, int[][] m, int radius) {
        int iCor = i - radius;
        int jCor = j - radius;
        int length = 2 * radius + 1;
        if (iCor < 0 || jCor < 0 || i + radius > m.length - 1 || j + radius > m[0].length - 1) {

            return;
        }
        for (int k = 0; k < length; k++) {
            for (int l = 0; l < length; l++) {
                m[iCor + k][jCor + l] = 255;
            }
        }

    }


    /* generate image from the pixel matrix */
    private void drawImage(int[][] matrix, String path) {
        /* draw from bufferedImage */
        File newImgFile = new File(path);
        int width = matrix[0].length;
        int height = matrix.length;
        try {
            BufferedImage image = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_3BYTE_BGR);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    Color c = new Color(matrix[j][i], matrix[j][i], matrix[j][i]);
                    image.setRGB(i, j, c.getRGB());
                }
            }
            ImageIO.write(image, "jpg", newImgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* get rid of the boundary caused by convolution */
    private void cleanBoundary(int[][] matrix, int mag) {
        for (int i = 0; i < mag; i++) {
            for (int j = 0; j < height; j++) {
                // gaussianImage[j][i] = image[j][i];
                matrix[j][i] = 255;
            }
        }
        for (int i = width - mag; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // gaussianImage[j][i] = image[j][i];
                matrix[j][i] = 255;
            }
        }
        for (int i = 0; i < mag; i++) {
            for (int j = 0; j < width; j++) {
                // gaussianImage[i][j] = image[i][j];
                matrix[i][j] = 255;
            }
        }
        for (int i = height - mag; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // gaussianImage[i][j] = image[i][j];
                matrix[i][j] = 255;
            }
        }
    }
}





/* ---- utility class ---- */

class Line {
    public int pIndex;
    public int thetaIndex;
    public int accumulator;
    Line(int thetaIndex, int pIndex, int accumulator) {
        this.pIndex = pIndex;
        this.thetaIndex = thetaIndex;
        this.accumulator = accumulator;
    }
}


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
 */
public class ImageProcessor {
    private String id;
    private int[][] image;
    private int[][] grayImage;
    private int width;
    private int height;
    /* for hough transform */
    private int thetaStrides;
    private int pStrides;
    /* Save sin and cos values for future calculation. */
    private double sinArray[];
    private double cosArray[];
    private int[][] houghTable;

    public ImageProcessor(int[][] matrix, String id, int thetaStrides, int pStrides) {
        this.id = id;
        this.image = matrix;
        this.width = matrix[0].length;
        this.height = matrix.length;
        this.thetaStrides = thetaStrides;
        this.pStrides = pStrides;
        sinArray = new double[this.thetaStrides];
        cosArray = new double[this.thetaStrides];
        houghTable = new int[thetaStrides][pStrides];
        grayImage = image.clone();
    }

    public void Sobel() {
        int threshold = 30;
        /* sobel operators */
        int[][] sobelX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };
        int[][] sobelY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
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

                int mag = (int) Math.sqrt(x * x + y * y);
                result[i][j] = mag;

//                if (result[i][j] < 0) {
                if (result[i][j] < threshold) {
                    result[i][j] = 255;
                } else {
                    result[i][j] = 0;
                }

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
    }


    public void houghTransform() {
        fillTable();
        List<Line> lineList = extractLines();
        drawLines(lineList, grayImage, "./OutputImages/" + id +"_Lines.jpg");

        System.out.println("Yahoooooo!");
    }


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


    private List<Line> extractLines() {
        int k = 20;
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

        for (int i = 0; i < houghTable.length; i++) {
            for (int j = 0; j < houghTable[0].length; j++) {
                if (houghTable[i][j] > maxValue) {
                    maxValue = houghTable[i][j];

                }

                if (houghTable[i][j] > accumulatorThreshold) {
                    Line curLine = new Line (i, j, houghTable[i][j]);
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
            System.out.print(" ****** " + l.pIndex + " " + l.thetaIndex);
        }
        System.out.println();

        return lineList;
    }

    private void drawLines(List<Line> lineList, int[][] m, String path) {

        System.out.println("Line list length : " + lineList.size());
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

        System.out.println("Theta : " + theta);
        System.out.println("P : " + p);

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
        // System.out.println(" " + width + " " + height);
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

}

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


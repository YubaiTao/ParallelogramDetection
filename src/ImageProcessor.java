import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * project: ParallelogramDetection
 *
 * @author YubaiTao on 29/10/2017.
 */
public class ImageProcessor {
    private int[][] image;
    private int width;
    private int height;
    /* for hough transform */
    private int thetaStrides;
    private int pStrides;
    /* Save sin and cos values for future calculation. */
    private double sinArray[];
    private double cosArray[];
    private double[][] houghTable;

    public ImageProcessor(int[][] matrix, int thetaStrides, int pStrides) {
        this.image = matrix;
        this.width = matrix[0].length;
        this.height = matrix.length;
        this.thetaStrides = thetaStrides;
        this.pStrides = pStrides;
        sinArray = new double[this.thetaStrides];
        cosArray = new double[this.thetaStrides];
        houghTable = new double[thetaStrides][pStrides];
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
//                if (result[i][j] > 255) {
//                if(result[i][j] >= threshold) {
//                    result[i][j] = 200;
//                }
            }
        }
        String output = "./OutputImages/Sobel.jpg";
        drawImage(result, output);
    }


    public void houghTransform() {
        fillTable();
        extractLines();
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

        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                // 255: white    0: black
                if (image[i][j] != 255) {
                    addPoint(i, j);
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
            double curP = Math.sqrt(i * sinArray[m] + j * cosArray[m]);
            if (curP < 0 || curP > maxP) {
                continue;
            }
            houghTable[m][(int) (curP / pStride)] += 1;
        }
    }


    private void extractLines() {
        System.out.println("----");
        int k;

    }


    /* generate image from the pixel matrix */
    private void drawImage(int[][] matrix, String path) {
        /* draw from bufferedImage */
        File newImgFile = new File(path);
        int width = matrix[0].length;
        int height = matrix.length;
        // System.out.println(" " + width + " " + height);
        try {
            BufferedImage image = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_BYTE_GRAY);
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

    /*
    private void drawImage(double[][] matrix, String path) {
        // draw from bufferedImage
        File newImgFile = new File(path);
        int width = matrix[0].length;
        int height = matrix.length;
        double aver = 256 / (image.length * image[0].length);
        System.out.println("aver: " + aver);

        try {
            BufferedImage image = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_BYTE_GRAY);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int value = (int)(aver * matrix[j][i]);
                    if (value > 255) {
                        value = 255;
                    }
                    if (value < 0) {
                        value = 0;
                    }
                    Color c = new Color(value, value, value);
                    image.setRGB(i, j, c.getRGB());
                }
            }
            ImageIO.write(image, "jpg", newImgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

*/

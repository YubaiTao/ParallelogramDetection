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
    public ImageProcessor(int[][] matrix) {
        this.image = matrix;
        this.width = matrix[0].length;
        this.height = matrix.length;
    }

    public void Sobel() {
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
        // int[][] mask = { {0, 1, 0}, {1, -4, 1}, {0, 1, 0} };
        int[][] result = new int[height][width];
        int i, j;
        for (i = 1; i < height - 1; i++) {
            for (j = 1; j < width - 1; j++) {
                int x = image[i - 1][j - 1] * sobelX[0][0] + image[i][j - 1] * sobelX[0][1] + image[i - 1][j + 1] * sobelX[0][2] +
                        image[i][j - 1] * sobelX[1][0] + image[i][j] * sobelX[1][1] + image[i][j + 1] * sobelX[1][2] +
                        image[i + 1][j - 1] * sobelX[2][0] + image[i][j + 1] * sobelX[2][1] + image[i + 1][j + 1] * sobelX[2][2] ;

                int y = image[i - 1][j - 1] * sobelY[0][0] + image[i][j - 1] * sobelY[0][1] + image[i - 1][j + 1] * sobelY[0][2] +
                        image[i][j - 1] * sobelY[1][0] + image[i][j] * sobelY[1][1] + image[i][j + 1] * sobelY[1][2] +
                        image[i + 1][j - 1] * sobelY[2][0] + image[i][j + 1] * sobelY[2][1] + image[i + 1][j + 1] * sobelY[2][2];

                int mag = (int)Math.sqrt(x * x + y * y);
                result[i][j] = mag;

                if (result[i][j] < 0) {
                    result[i][j] = 0;
                }
                if (result[i][j] > 255) {
                    result[i][j] = 255;
                }
            }
        }
        String output = "./OutputImages/Sobel.jpg";
        drawImage(result, output);
    }

    private void drawImage(int[][] matrix, String path) {
        // draw from bufferedImage
        File newImgFile = new File(path);
        int width = matrix[0].length;
        int height = matrix.length;
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

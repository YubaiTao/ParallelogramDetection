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

    private void Sobel() {
        // int[][] mask = { {0, 1, 0}, {1, -4, 1}, {0, 1, 0} };
        int[][] result = new int[height][width];
        int i, j;
        for (i = 1; i < height - 2; i++) {
            for (j = 1; j < width - 2; j++) {
                result[i][j] = image[i - 1][j] + image[i][j - 1] +
                               image[i + 1][j] + image[i][j + 1] - 4 * image[i][j];
                if (result[i][j] < 0) {
                    result[i][j] = 0;
                }
                if (result[i][j] > 255) {
                    result[i][j] = 255;
                }
            }
        }
    }

    private void drawImage(int[][] matrix, String path) {
        // draw from bufferedImage
    }
}

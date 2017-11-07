import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * project: ParallelogramDetection
 *
 * @author YubaiTao on 29/10/2017.
 *
 *
 *
 *
 */
public class ImageLoader {
    private String imagePath;
    private String grayScalePath;
    public String ID;
    public ImageLoader(String path) {
        this.imagePath = path;
        ID = getFileID(path);
        grayScalePath = grayScale();
    }

    private String grayScale() {
        File imgFile = new File(imagePath);
        int width;
        int height;
        try {
            BufferedImage image = ImageIO.read(imgFile);
            width = image.getWidth();
            height = image.getHeight();

            for(int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    Color c = new Color(image.getRGB(j, i));
                    int red = (int) (c.getRed() * 0.30);
                    int green = (int) (c.getGreen() * 0.59);
                    int blue = (int) (c.getBlue() * 0.11);
                    Color newColor = new Color(red + green + blue, red + green + blue, red + green + blue);
                    image.setRGB(j, i, newColor.getRGB());
                }
            }
//            Color c = new Color(image.getRGB(100, 20));
//            System.out.print(" " + c.getRed() + " " + c.getBlue() + " " + c.getGreen());
//            System.out.print("\nWidth: " + width + "    Height: " + height);
            String grayScaleImage = "./OutputImages/" + ID +"_grayscale.jpg";
            File output = new File(grayScaleImage);
            ImageIO.write(image, "jpg", output);
            return grayScaleImage;
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        }

    }

    public int[][] getMatrix() {
        if (grayScalePath == null) {
            return null;
        }
        File imgFile = new File(grayScalePath);
        int width;
        int height;
        int[][] matrix;
        try {
            BufferedImage image = ImageIO.read(imgFile);
            width = image.getWidth();
            height = image.getHeight();
            matrix = new int[height][width];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    Color c = new Color(image.getRGB(j, i));
                    // red; blue; green are all same now.
                    int value = c.getRed();
                    matrix[i][j] = value;
                }
            }
            return matrix;
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        }
    }

    private String getFileID(String path) {
        char[] array = path.toCharArray();
        int start = 0, end = 0;
        for (int i = array.length - 1; i > -1; i--) {
            if (array[i] == '.') {
                end = i;
                break;
            }
        }
        for (int i = array.length - 1; i > -1; i--) {
            if (array[i] == '/') {
                start = i + 1;
                break;
            }
        }
        if (start >= end) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(array[i]);
        }

        return sb.toString();
    }
}

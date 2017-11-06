/**
 * project: ParallelogramDetection
 *
 * @author YubaiTao on 29/10/2017.
 *
 *
 * Project description: Parallelograms appear frequently in images that contain man-made objects.
 * They often correspond to the projections of rectangular surfaces when viewed at an angle that is not perpendicular to the surfaces.
 * In this project, you are to design and implement a program that can detect parallelograms of all sizes in an image.
 * Your program will consist of three steps:
 *     (1) detect edges using the Sobel’s operator,
 *     (2) detect straight line segments using the Hough Transform, and
 *     (3) detect parallelograms from the straight-line segments detected in step (2).
 *         In step (1), compute edge magnitude using the formula below and
 *         then normalize the magnitude values to lie within the range [0,255].
 *         Next, manually choose a threshold value to produce a binary edge map.
 *
 *
 *
 */
public class ParallelogramDetection {
    public static void main(String[] args) {
        ImageLoader imgLoader = new ImageLoader("./TestImages/TestImage1c.jpg");
        ImageProcessor imgProcessor = new ImageProcessor(imgLoader.getMatrix(), 90, 100);
        imgProcessor.Sobel();
        imgProcessor.houghTransform();

    }
}

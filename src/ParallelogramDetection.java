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
        System.out.println("******************* First Image *******************");
        ImageLoader imgLoader = new ImageLoader("./TestImages/TestImage1c.jpg");
        ImageProcessor imgProcessor = new ImageProcessor(imgLoader.getMatrix(), imgLoader.ID, 60, 1000);
        imgProcessor.GaussianBlur(1, 5);
        imgProcessor.Sobel();
        imgProcessor.NMS();
        imgProcessor.threshold(60);
        imgProcessor.houghTransform();
        ParallelogramDetector pd = new ParallelogramDetector(imgProcessor);
        pd.lineDistinction(30, 40);
        pd.setUpParallelograms();
        // pd.test();
        System.out.println();


        System.out.println("******************* Second Image *******************");
        ImageLoader imgLoader_2 = new ImageLoader("./TestImages/TestImage2c.jpg");
        ImageProcessor imgProcessor_2 = new ImageProcessor(imgLoader_2.getMatrix(), imgLoader_2.ID, 180, 1000);
        imgProcessor_2.GaussianBlur(1, 3);
        imgProcessor_2.Sobel();
        imgProcessor_2.NMS();
        imgProcessor_2.threshold(40);
        imgProcessor_2.houghTransform();
        ParallelogramDetector pd_2 = new ParallelogramDetector(imgProcessor_2);
        pd_2.lineDistinction(10, 40);
        pd_2.setUpParallelograms();
        System.out.println();

        System.out.println("******************* Third Image *******************");
        ImageLoader imgLoader_3 = new ImageLoader("./TestImages/TestImage3.jpg");
        ImageProcessor imgProcessor_3 = new ImageProcessor(imgLoader_3.getMatrix(), imgLoader_3.ID, 180, 1000);
        imgProcessor_3.GaussianBlur(1, 3);
        imgProcessor_3.Sobel();
        imgProcessor_3.NMS();
        imgProcessor_3.threshold(30);
        imgProcessor_3.houghTransform();
        ParallelogramDetector pd_3 = new ParallelogramDetector(imgProcessor_3);
        pd_3.lineDistinction(10, 40);
        pd_3.setUpParallelograms();
        System.out.println();




    }
}

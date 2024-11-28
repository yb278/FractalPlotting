import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import javax.swing.*;

public class Main extends JPanel {
    private int width = 800;
    private int height = 800;

    // Complex plane bounds
    private double minReal = -2.0;
    private double maxReal = 1.0;
    private double minImaginary = -1.5;
    private double maxImaginary = 1.5;

    private double zoomFactor = 0.8; // Zoom scale
    private BufferedImage image; // Stores the Mandelbrot image
    private ExecutorService executor; // Thread pool for computation
    private int maxIterBase = 500; // Base maximum iteration count
    private boolean isRendering = false; // Flag to prevent overlapping renders

    // Panning variables
    private int lastMouseX, lastMouseY;
    private double dragStartReal, dragStartImaginary;

    public Main() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Zoom on mouse click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double clickedReal = mapToReal(e.getX());
                double clickedImaginary = mapToImaginary(e.getY());

                if (SwingUtilities.isLeftMouseButton(e)) {
                    zoom(clickedReal, clickedImaginary, zoomFactor); // Zoom in
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    zoom(clickedReal, clickedImaginary, 1 / zoomFactor); // Zoom out
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                dragStartReal = minReal;
                dragStartImaginary = minImaginary;
            }
        });

        // Enable panning
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getX() - lastMouseX;
                int deltaY = e.getY() - lastMouseY;

                double realWidth = maxReal - minReal;
                double imaginaryHeight = maxImaginary - minImaginary;

                double realShift = deltaX * realWidth / width;
                double imaginaryShift = deltaY * imaginaryHeight / height;

                minReal = dragStartReal - realShift;
                maxReal = minReal + realWidth;
                minImaginary = dragStartImaginary - imaginaryShift;
                maxImaginary = minImaginary + imaginaryHeight;

                renderMandelbrot(true); // Render while panning
            }
        });

        // Initial rendering
        renderMandelbrot(false);
    }

    private void renderMandelbrot(boolean progressive) {
        if (isRendering) return; // Avoid overlapping renders
        isRendering = true;

        // Create a new BufferedImage for rendering
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Progressive rendering for fast feedback
        if (progressive) {
            int step = 4; // Render every 4th pixel initially
            for (int x = 0; x < width; x += step) {
                for (int y = 0; y < height; y += step) {
                    double real = mapToReal(x);
                    double imaginary = mapToImaginary(y);
                    int color = computeColor(real, imaginary);

                    // Fill blocks of pixels
                    for (int dx = 0; dx < step && x + dx < width; dx++) {
                        for (int dy = 0; dy < step && y + dy < height; dy++) {
                            image.setRGB(x + dx, y + dy, color);
                        }
                    }
                }
            }
            repaint(); // Show progressive render
        }

        // Multithreaded computation for full resolution
        int threadCount = Runtime.getRuntime().availableProcessors();
        int sliceHeight = height / threadCount;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int startY = i * sliceHeight;
            int endY = (i == threadCount - 1) ? height : startY + sliceHeight;

            executor.submit(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        double real = mapToReal(x);
                        double imaginary = mapToImaginary(y);
                        int color = computeColor(real, imaginary);
                        image.setRGB(x, y, color);
                    }
                }
                latch.countDown();
            });
        }

        new Thread(() -> {
            try {
                latch.await(); // Wait for all threads to finish
                repaint(); // Update the display
                isRendering = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void zoom(double centerReal, double centerImaginary, double zoomFactor) {
        double realWidth = maxReal - minReal;
        double imaginaryHeight = maxImaginary - minImaginary;

        double newRealWidth = realWidth * zoomFactor;
        double newImaginaryHeight = imaginaryHeight * zoomFactor;

        minReal = centerReal - newRealWidth / 2;
        maxReal = centerReal + newRealWidth / 2;
        minImaginary = centerImaginary - newImaginaryHeight / 2;
        maxImaginary = centerImaginary + newImaginaryHeight / 2;

        renderMandelbrot(false);
    }

    private double mapToReal(int x) {
        return minReal + x * (maxReal - minReal) / width;
    }

    private double mapToImaginary(int y) {
        return minImaginary + y * (maxImaginary - minImaginary) / height;
    }

    private int computeColor(double real, double imaginary) {
        int maxIter = getDynamicMaxIter();
        double zr = 0, zi = 0;
        int iter = 0;

        while (zr * zr + zi * zi < 4 && iter < maxIter) {
            double temp = zr * zr - zi * zi + real;
            zi = 2 * zr * zi + imaginary;
            zr = temp;
            iter++;
        }

        return iter == maxIter ? 0 : Color.HSBtoRGB((float) iter / maxIter, 1, 1);
    }

    private int getDynamicMaxIter() {
        double zoomLevel = maxReal - minReal;
        return Math.max(maxIterBase, (int) (maxIterBase / zoomLevel));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Optimized Mandelbrot Explorer");
        Main panel = new Main();
        frame.add(panel);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

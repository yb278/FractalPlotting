import java.awt.*;
import javax.swing.*;

public class Main extends JPanel {
    private final int width = 800;
    private final int height = 800;
    private final int maxIter = 1000;

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double real = mapToReal(x, width, -2.0, 1.0);
                double imaginary = mapToImaginary(y, height, -1.5, 1.5);
                int color = computeColor(real, imaginary);
                g2d.setColor(new Color(color));
                g2d.drawLine(x, y, x, y); // Draw pixel
            }
        }
    }

    private double mapToReal(int x, int width, double minR, double maxR) {
        return minR + x * (maxR - minR) / width;
    }

    private double mapToImaginary(int y, int height, double minI, double maxI) {
        return minI + y * (maxI - minI) / height;
    }

    private int computeColor(double real, double imaginary) {
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Mandelbrot Set");
        Main panel = new Main();
        frame.add(panel);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

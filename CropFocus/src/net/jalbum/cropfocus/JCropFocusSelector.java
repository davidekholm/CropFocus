package net.jalbum.cropfocus;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import se.datadosen.component.JBackgroundPanel;

/**
 *
 * @author david
 */
public class JCropFocusSelector extends JBackgroundPanel {

    private float xWeight = 0.5f;
    private float yWeight = 0.2f;
    private float ratio = 1;
    private Rectangle cb = new Rectangle();

    public JCropFocusSelector() {
        init();
    }

    public float getxWeight() {
        return xWeight;
    }

    public void setxWeight(float xWeight) {
        this.xWeight = capValue(xWeight, 0, 1);
        repaint();
    }

    public float getyWeight() {
        return yWeight;
    }

    public void setyWeight(float yWeight) {
        this.yWeight = capValue(yWeight, 0, 1);
        repaint();
    }

    public float getRatio() {
        return ratio;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
        repaint();
    }

    public Rectangle getCropBounds() {
        return new Rectangle(cb);
    }

    private static float capValue(float value, float min, float max) {
        value = Math.max(value, min);
        value = Math.min(value, max);
        return value;
    }

    private static Dimension calcDimension(int width, int height, Dimension boundingBox) {
        // Make image fit within bounding box without changing aspect ratio
        // Do not enlarge image
        if ((width <= boundingBox.width) && (height <= boundingBox.height)) {
            return new Dimension(width, height);
        }

        double widthScale = (double) width / boundingBox.width;
        double heightScale = (double) height / boundingBox.height;
        double maxScale = Math.max(widthScale, heightScale);
        return new Dimension((int) (width / maxScale + 0.5), (int) (height / maxScale + 0.5));
    }

    @Override
    protected void paintComponent(Graphics g2d) {
        Graphics2D g = (Graphics2D) g2d;
        super.paintComponent(g);

        if (bg != null) {
            Rectangle bounds = getImageBounds();
            Dimension sDim = bounds.getSize();
            Dimension d = new Dimension(sDim.width, (int) (sDim.width / ratio));
            Dimension dDim = calcDimension(d.width, d.height, sDim);
            int cropX = (int) ((sDim.width - dDim.width) * xWeight);
            int cropY = (int) ((sDim.height - dDim.height) * yWeight);
            cb.x = cropX;
            cb.y = cropY;
            cb.width = dDim.width;
            cb.height = dDim.height;

            AffineTransform backup = g.getTransform();
            g.translate(bounds.x, bounds.y);
            BufferedImage bi = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D big = bi.createGraphics();
            Color dimmed = new Color(255, 255, 255, 180);
            big.setColor(dimmed);
            big.fillRect(0, 0, bi.getWidth(), bi.getHeight());
            big.setComposite(AlphaComposite.Clear);
            big.fillRect(cb.x, cb.y, cb.width, cb.height);
            g.drawImage(bi, 0, 0, this);
//        g.fillRect(cropX, cropY, dDim.width, dDim.height);
            g.setTransform(backup);
        }
    }

    private Point plantPoint;
    private Rectangle plantCropBounds;

    private void init() {

        MouseAdapter ma = new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                e.translatePoint(-plantPoint.x, -plantPoint.y);
                Point p = e.getPoint();

                Rectangle bounds = getImageBounds();
                Dimension sDim = bounds.getSize();
                Dimension d = new Dimension(sDim.width, (int) (sDim.width / ratio));
                Dimension dDim = calcDimension(d.width, d.height, sDim);
                int cropX = plantCropBounds.x + p.x;
                int cropY = plantCropBounds.y + p.y;

                int wdiff = sDim.width - dDim.width;
                if (wdiff > 0) {
                    setxWeight((float) cropX / wdiff);
                }
                int hdiff = sDim.height - dDim.height;
                if (hdiff > 0) {
                    setyWeight((float) cropY / hdiff);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                plantPoint = e.getPoint();
                plantCropBounds = getCropBounds();
            }

        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

}

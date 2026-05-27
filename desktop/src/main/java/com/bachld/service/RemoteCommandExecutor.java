package com.bachld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public class RemoteCommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommandExecutor.class);
    private static final int MAX_WIDTH = 1280;
    private static final float JPEG_QUALITY = 0.65f;

    public byte[] captureScreenshotJpeg() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Không thể chụp màn hình trong môi trường headless");
        }

        Rectangle bounds = getAllScreenBounds();
        BufferedImage capture = new Robot().createScreenCapture(bounds);
        BufferedImage rgbImage = toRgb(resizeIfNeeded(capture));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeJpeg(rgbImage, output);
        log.debug("Captured screenshot: {} bytes", output.size());
        return output.toByteArray();
    }

    private BufferedImage resizeIfNeeded(BufferedImage image) {
        if (image.getWidth() <= MAX_WIDTH) {
            return image;
        }

        int targetHeight = Math.max(1, Math.round(image.getHeight() * (MAX_WIDTH / (float) image.getWidth())));
        BufferedImage resized = new BufferedImage(MAX_WIDTH, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, MAX_WIDTH, targetHeight, null);
        graphics.dispose();
        return resized;
    }

    private BufferedImage toRgb(BufferedImage image) {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }

    private void writeJpeg(BufferedImage image, ByteArrayOutputStream output) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", output);
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private Rectangle getAllScreenBounds() {
        Rectangle bounds = new Rectangle();
        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();

        for (GraphicsDevice device : devices) {
            bounds = bounds.union(device.getDefaultConfiguration().getBounds());
        }

        return bounds;
    }
}

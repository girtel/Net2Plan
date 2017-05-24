/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/


package com.net2plan.utils;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Auxiliary functions to work with images.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class ImageUtils
{
	public static Color TRANSPARENT = new Color (0,0,0,0);
	/**
	 * Image type
	 * 
	 */
	public enum ImageType
	{
		/**
		 * Bitmap file.
		 * 
		 */
		BMP,
		
		/**
		 * JPG file.
		 * 
		 */
		JPG,
		
		/**
		 * PNG file.
		 * 
		 */
		PNG
	};
	
	private static boolean hasAlpha(Image image)
	{
        if (image instanceof BufferedImage) return ((BufferedImage) image).getColorModel().hasAlpha();
 
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try { pg.grabPixels(); } catch (InterruptedException e) { }
 
        return pg.getColorModel().hasAlpha();
    }	

	/**
	 * Converts an {@code Image} to a {@code BufferedImage}.
	 *
	 * @param image {@code Image}
	 * @return A {@code BufferedImage}
	 */
	public static BufferedImage imageToBufferedImage(Image image)
	{
		if (image instanceof BufferedImage) return (BufferedImage) image;
 
        image = new ImageIcon(image).getImage();
 
        boolean hasAlpha = hasAlpha(image);
        BufferedImage bi = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try
		{
            int transparency = Transparency.OPAQUE;
            if (hasAlpha == true) transparency = Transparency.BITMASK;
 
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bi = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        }
		catch (HeadlessException e)
		{
        }
 
        if (bi == null)
		{
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha == true) type = BufferedImage.TYPE_INT_ARGB;
			
            bi = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
 
        Graphics g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
 
        return bi;
	}

	/**
	 * Reads an image from a file.
	 *
	 * @param file File
	 * @return A {@code BufferedImage}
	 */
	public static BufferedImage readImageFromFile(File file)
	{
		try { return readImageFromURL(file.toURI().toURL()); }
		catch (MalformedURLException e) { throw new RuntimeException(e); }
	}

	/**
	 * Reads an image from an URL.
	 *
	 * @param url URL
	 * @return A {@code BufferedImage}
	 */
	public static BufferedImage readImageFromURL(URL url)
	{
		try { return ImageIO.read(url); }
		catch (IOException e) { throw new RuntimeException(e); }
	}
	
	/**
	 * Resizes an image.
	 * 
	 * @param image Source image to scale
	 * @param width Desired width
	 * @param height Desired height
	 * @return Resized image
	 */
	public static BufferedImage resize(BufferedImage image, int width, int height)
	{
		int type = image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage resizedImage = new BufferedImage(width, height,type);

		Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
     }
	
	/**
	 * Takes an snapshot of the current state of a component.
	 * 
	 * @param component Source component
	 * @return {@code BufferedImage}
	 */
	public static BufferedImage takeSnapshot(JComponent component)
	{
		Dimension currentSize = component.getSize();
		BufferedImage bi = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphic = bi.createGraphics();
		graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphic.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		boolean db = component.isDoubleBuffered();
		component.setDoubleBuffered(false);
		component.paint(graphic);
		graphic.dispose();
		component.setDoubleBuffered(db);
		
		return bi;
	}
	
	/**
	 * Removes the white borders of a {@code BufferedImage}.
	 *
	 * @param img Input image
	 * @return The input image without white borders
	 */
    public static BufferedImage trim(BufferedImage img)
	{
        final int width = img.getWidth();
        final int height = img.getHeight();
		if (width == 0 || height == 0) return img;
		
		int topY = Integer.MAX_VALUE;
		int topX = Integer.MAX_VALUE;
		int bottomY = Integer.MIN_VALUE;
		int bottomX = Integer.MIN_VALUE;
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				if (img.getRGB(x, y) != Color.WHITE.getRGB())
				{
				   if (x < topX) topX = x;
				   if (y < topY) topY = y;
				   if (x > bottomX) bottomX = x;
				   if (y > bottomY) bottomY = y;
				}
			}
		}
		
		bottomX = Math.min(bottomX + 1, width - 1);
		bottomY = Math.min(bottomY + 1, height - 1);
		
		BufferedImage newImg = new BufferedImage(bottomX - topX + 1, bottomY - topY + 1, BufferedImage.TYPE_INT_ARGB);
		Graphics g = newImg.createGraphics();
        g.drawImage(img, 0, 0, newImg.getWidth(), newImg.getHeight(), topX, topY, bottomX, bottomY, null);
		g.dispose();		
		
		return newImg;
    }
	
	/**
	 * Writes an image to a file.
	 *
	 * @param file Output file
	 * @param bufferedImage Image to save
	 * @param imageType Image type (bmp, jpg, png)
	 */
	public static void writeImageToFile(File file, BufferedImage bufferedImage, ImageType imageType)
	{
		try
		{
			switch (imageType)
			{
				case BMP:
					ImageIO.write(bufferedImage, "bmp", file);
					break;

				case JPG:
					ImageIO.write(bufferedImage, "jpg", file);
					break;

				case PNG:
					ImageIO.write(bufferedImage, "png", file);
					break;

				default:
					throw new UnsupportedOperationException("Not implemented yet");
			}
		}
		catch (IOException | UnsupportedOperationException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
    /** Creates a new image that adds a border of the given thickness and color to the image (so the image size grows) 
     * @param source the source image
     * @param borderSizePixels the thickness in pixels
     * @param borderColor the border color
     * @return the new image
     */
    public static BufferedImage addBorder (BufferedImage source , int borderSizePixels , Color borderColor)
    {
    	final int sourceWidth = source.getWidth();
    	final int sourceHeight = source.getHeight();
    	final int borderedImageWidth = sourceWidth + (borderSizePixels * 2);
    	final int borderedImageHeight = sourceHeight + (borderSizePixels * 2);
    	BufferedImage img = new BufferedImage(borderedImageWidth, borderedImageHeight, BufferedImage.TYPE_INT_ARGB);
    	img.createGraphics();
    	Graphics2D g = (Graphics2D) img.getGraphics();
    	g.drawImage(source, borderSizePixels, borderSizePixels, sourceWidth + borderSizePixels , sourceHeight + borderSizePixels , 0, 0, sourceWidth, sourceHeight, TRANSPARENT ,  null);
    	g.setColor(borderColor);
    	for (int lineItem = 0 ; lineItem < borderSizePixels ; lineItem ++)
    		g.drawRect(0 + lineItem , 0 + lineItem , borderedImageWidth - 1 - 2*lineItem, borderedImageHeight - 1 - 2*lineItem);
    	g.dispose();
    	return img;
    }

    /** Creates an image from the given shape
     * @param s the shape
     * @return the image
     */
    public static BufferedImage createImageFromShape (Shape s) 
    {
        Rectangle r = s.getBounds();
        BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D gr = image.createGraphics();
        // move the shape in the region of the image
        gr.translate(-r.x, -r.y);
        gr.draw(s);
        gr.dispose();
        return image;
    }

    /** Creates an image of a filled circle of the given color, the rest is transparent
     * @param iconHeight the height and width of the created image, and the diameter of the circle 
     * @param fillColor the color to fill the circle
     * @return the image
     */
    public static BufferedImage createCircle (int iconHeight , Color fillColor) 
    {
    	BufferedImage img = new BufferedImage(iconHeight, iconHeight, BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g = img.createGraphics();
    	g.setColor(fillColor);
    	g.fillOval(0, 0 , iconHeight , iconHeight);
    	g.dispose();
        return img;
    }

}

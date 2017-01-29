package com.net2plan.gui.utils.topologyPane;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.net2plan.utils.Pair;

public class IconCreator {

	public static void main(String[] args) throws Exception 
	{
		//Pair<Image,Shape> pair = new IconCreator ().getImage ("src/main/resources/resources/icons/router.jpg" , 50);
//		Pair<Image,Shape> pair = new IconCreator ().getImage ("src/main/resources/resources/icons/googlelogo_color_120x44dp.png" , 50);

		BufferedImage img = ImageIO.read(new File("src/main/resources/resources/icons/googlelogo_color_120x44dp.png")); 
		
		
		JFrame frame = new JFrame("Example");
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(img)));
//		frame.getContentPane().add(new JLabel(new ImageIcon(img2)));
//		frame.getContentPane().add(new JLabel(new ImageIcon(img3)));
		frame.pack();
		frame.setVisible(true);JPanel panel = new ImagePanel(img);
//		panel.repaint();
//	    frame.add(panel);
//	    frame.pack();
//	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	    frame.setVisible(true);   
//		// get Image

	}
	
	public Pair<Image,Shape> getImage (String file , int heightPixels) throws Exception
	{
		System.out.println(new File (file).exists());
		System.out.println(new File (file).getAbsolutePath());
		BufferedImage img = ImageIO.read(new File(file)); 
		System.out.println("here");
        return Pair.of(img,null);
	}

	public static class ImagePanel extends JPanel 
	{

		BufferedImage image;
		Dimension size;

		public ImagePanel(BufferedImage image) 
		{
		    this.image = image;
		    this.size = new Dimension();
		    System.out.println(image.getWidth() + " , " + image.getHeight());
		    size.setSize(image.getWidth(), image.getHeight());
//		    this.setBackground(Color.WHITE);
//		    this.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
		}
		@Override
		public Dimension getPreferredSize() { return size; }

	}

}
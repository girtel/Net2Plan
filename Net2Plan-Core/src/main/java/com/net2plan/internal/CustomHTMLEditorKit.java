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




 





package com.net2plan.internal;

import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Map;

/**
 * Custom version of {@code HTMLEditorKit} with an image cache.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
@SuppressWarnings("unchecked")
public class CustomHTMLEditorKit extends HTMLEditorKit
{
	private static final long serialVersionUID = 1L;
	private final Map<String, Image> imageCache;
	private final ViewFactory viewFactory;

	/**
	 * Default constructor
	 *
	 * @since 0.3.0
	 */
	public CustomHTMLEditorKit()
	{
		super();
		imageCache = new LinkedHashMap<String, Image>();
		viewFactory = new HTMLFactoryX();
	}

	@Override
	public ViewFactory getViewFactory()
	{
		return viewFactory;
	}

	@Override
	public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException
	{
		if (doc instanceof HTMLDocument)
		{
			FixedHTMLWriter w = new FixedHTMLWriter(out, (HTMLDocument) doc, pos, len);
			w.write();
		}
		else if (doc instanceof StyledDocument)
		{
			MinimalHTMLWriter w = new MinimalHTMLWriter(out, (StyledDocument) doc, pos, len);
			w.write();
		}
		else
		{
			super.write(out, doc, pos, len);
		}
	}

	/**
	 * Returns the image cache
	 *
	 * @return Map containing the image cache
	 * @since 0.3.0
	 */
	public Map<String, Image> getImages()
	{
		return imageCache;
	}
	
	/**
	 * Includes a Net2Plan header to a HTML string. It is an internal function, 
	 * and users do not need to use it.
	 * 
	 * @param html HTML content
	 * @return A new HTML content including the Net2Plan header before the contents
	 * @since 0.3.0
	 */
	public static String includeNet2PlanHeader(String html)
	{
            	org.jsoup.nodes.Document doc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
		org.jsoup.nodes.Element title = doc.head().getElementsByTag("title").first();
		String reportTitle = title != null && title.hasText() ? title.text() : "Net2Plan Report";

		String header;
		try { header = HTMLUtils.getHTMLFromURL(HTMLUtils.class.getResource("/resources/common/reportHeader.html").toURI().toURL()); }
		catch (URISyntaxException | MalformedURLException ex) { throw new RuntimeException(ex);	}
		
		header = header.replaceFirst("#ReportTitle#", reportTitle);
		html = html.replaceAll("<body>", "<body>" + header);
		return html;
	}
        
        public static String includeStyle(String html)
        {
                StringBuilder style = new StringBuilder();
                style.append("<style>");
                style.append("body {font-family: Tahoma, Verdana, Segoe, sans-serif; font-style: normal; "
                        + "font-variant: normal; font-size: 12px; padding: 0px 30px 15px 10px; text-align: justify;}");    
                style.append("h1, h2, h3 {font-weight: 500;}");
                style.append("h1 {font-size: 24px; line-height: 26.4px;}");
                style.append("h2 {font-size: 18px; line-height: 20.4px;}");
                style.append("h3 {font-size: 14px; line-height: 15.4px;}");
                style.append("table, td {font-size: 11px; text-align: justify;}");
                style.append("table, tr, th {border: 0px;}");
                style.append("td {border: 2px solid #ccccff;}");
                style.append("th {background-color: #ccccff; text-align: center; height: 20px; padding: 10px; border-bottom-left-radius: 5px; border-bottom-right-radius: 5px;}");
                style.append("caption {caption-side: bottom; padding-bottom: 10px; font-weight: bold;}");
                style.append("</style>");
            
                html = html.replaceAll("</head>", style.toString() + "</head>");
                return html;
        }
	
	/**
	 * <p>Saves an HTML content to a given file.</p>
	 * 
	 * <p><b>Important</b>: This method is for internal use, users should use the 
	 * other {@link com.net2plan.utils.HTMLUtils#saveToFile(java.io.File, java.lang.String) saveToFile} method.</p>
	 *
	 * @param file A valid file
	 * @param html HTML content
	 * @param images Map containing the image cache (URL -> Image)
	 * @since 0.3.1
	 */
	public static void saveToFile(File file, String html, Map<String, Image> images)
	{
		try
		{
			File dir = file.getAbsoluteFile().getParentFile();
			
			for (Map.Entry<String, Image> entry : images.entrySet())
			{
				File imgName = File.createTempFile("tmp_", ".png", dir);
				BufferedImage bi;
				try	{ bi = ImageUtils.imageToBufferedImage(entry.getValue()); }
				catch(Throwable e ) { continue; }
				
				if (bi != null)
				{
					ImageUtils.writeImageToFile(imgName, bi, ImageUtils.ImageType.PNG);
					html = html.replace(entry.getKey(), imgName.getName());
				}
			}
			
			String filePath = file.getPath();
			if (!filePath.toLowerCase(Locale.getDefault()).endsWith(".html")) file = new File(filePath + ".html");
			
			StringUtils.saveToFile(html, file);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Custom {@code JEditorPane}
	 * 
	 * @since 0.3.0
	 */
	public static class CustomJEditorPane extends JEditorPane
	{
		@Override
		public void setText(String t)
		{
			//Document doc = getDocument();
			//doc.putProperty("IgnoreCharsetDirective", true);
			t = t.replaceAll("^-(?=0(.0*)?$)", ""); /* Remove negative zeros */

			super.setText(t);
		}
	}
	
	/**
	 * This class overrides {@code HTMLWriter} so that CSS styles are not removed.
	 * 
	 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
	 * @since 0.2.3
	 * @see <a href="http://stackoverflow.com/questions/5939732/jeditorpane-is-removing-css-font-styles"></a>
	 */
	private static class FixedHTMLWriter extends HTMLWriter
	{
		private final List tags = new ArrayList(10);
		private final List tagValues = new ArrayList(10);
		private final List tagsToRemove = new ArrayList(10);
		private final MutableAttributeSet convAttr = new SimpleAttributeSet();
		private final MutableAttributeSet oConvAttr = new SimpleAttributeSet();

		/**
		 * Default constructor.
		 * 
		 * @param w   Writer
		 * @param doc HTMLDocument
		 * @param pos Initial position within {@code doc}
		 * @param len Text length
		 * @since 0.2.3
		 */
		public FixedHTMLWriter(Writer w, HTMLDocument doc, int pos, int len)
		{
			super(w, doc, pos, len);
		}

		private AttributeSet convertToHTML(AttributeSet from, MutableAttributeSet to)
		{
			if (to == null) to = convAttr;

			to.removeAttributes(to);
			if (from != null)
			{
				Enumeration keys = from.getAttributeNames();
				StringBuilder value = new StringBuilder();
				while (keys.hasMoreElements())
				{
					Object key = keys.nextElement();
					if (key instanceof CSS.Attribute)
					{
						value.append(key).append(": ").append(from.getAttribute(key)).append(";");
						if (keys.hasMoreElements())
						{
							value.append(" ");
						}
					}
					else
					{
						to.addAttribute(key, from.getAttribute(key));
					}
				}

				if (value.length() > 0)
				{
					to.addAttribute(HTML.Attribute.STYLE, value.toString());
				}
			}

			return to;
		}

		@Override
		protected void closeOutUnwantedEmbeddedTags(AttributeSet attr) throws IOException
		{
			tagsToRemove.clear();

			/* translate css attributes to html */
			attr = convertToHTML(attr, null);
			HTML.Tag t;
			Object tValue;
			int firstIndex = -1;
			int size = tags.size();

			/* First, find all the tags that need to be removed. */
			for (int i = size - 1; i >= 0; i--)
			{
				t = (HTML.Tag) tags.get(i);
				tValue = tagValues.get(i);
				if (attr == null || noMatchForTagInAttributes(attr, t, tValue))
				{
					firstIndex = i;
					tagsToRemove.add(t);
				}
			}

			if (firstIndex != -1)
			{
				/* Then close them out */
				boolean removeAll = ((size - firstIndex) == tagsToRemove.size());
				for (int i = size - 1; i >= firstIndex; i--)
				{
					t = (HTML.Tag) tags.get(i);
					if (removeAll || tagsToRemove.contains(t))
					{
						tags.remove(i);
						tagValues.remove(i);
					}

					write('<');
					write('/');
					write(t.toString());
					write('>');
				}
				/* Have to output any tags after firstIndex that still remaing, as we closed them out, but they should remain open */
				size = tags.size();
				for (int i = firstIndex; i < size; i++)
				{
					t = (HTML.Tag) tags.get(i);
					write('<');
					write(t.toString());
					Object o = tagValues.get(i);
					if (o != null && o instanceof AttributeSet)
					{
						writeAttributes((AttributeSet) o);
					}
					write('>');
				}
			}
		}

		private boolean noMatchForTagInAttributes(AttributeSet attr, HTML.Tag t, Object tagValue)
		{
			if (attr != null && attr.isDefined(t))
			{
				Object newValue = attr.getAttribute(t);
				if ((tagValue == null) ? (newValue == null) : (newValue != null && tagValue.equals(newValue)))
				{
					return false;
				}
			}
			return true;
		}

		@Override
		protected void writeEmbeddedTags(AttributeSet attr) throws IOException
		{
			/* translate css attributes to html */
			attr = convertToHTML(attr, oConvAttr);
			Enumeration names = attr.getAttributeNames();
			while (names.hasMoreElements())
			{
				Object name = names.nextElement();
				if (name instanceof HTML.Tag)
				{
					HTML.Tag tag = (HTML.Tag) name;
					if (tag == HTML.Tag.FORM || tags.contains(tag)) continue;

					write('<');
					write(tag.toString());
					Object o = attr.getAttribute(tag);
					if (o != null && o instanceof AttributeSet) writeAttributes((AttributeSet) o);

					write('>');
					tags.add(tag);
					tagValues.add(o);
				}
			}
		}

		@Override
		protected void writeAttributes(AttributeSet attr) throws IOException
		{
			convAttr.removeAttributes(convAttr);
			convertToHTML(attr, convAttr);
			Enumeration names = convAttr.getAttributeNames();
			while (names.hasMoreElements())
			{
				Object name = names.nextElement();
				if (name instanceof HTML.Tag || name instanceof StyleConstants || name == HTML.Attribute.ENDTAG) continue;

				write(" " + name + "=\"" + convAttr.getAttribute(name) + "\"");
			}
		}
	}

	private class HTMLFactoryX extends HTMLEditorKit.HTMLFactory
	{
		@Override
		public View create(Element elem)
		{
			Object obj = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
			if (obj instanceof HTML.Tag && (HTML.Tag) obj == HTML.Tag.IMG)
			{
				String src = (String) elem.getAttributes().getAttribute(HTML.Attribute.SRC);
				ImageView imageView = new ImageView(elem);
				imageCache.put(src, imageView.getImage());
				return imageView;
			}
			
			return super.create(elem);
		}
	}
}

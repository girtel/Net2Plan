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

import com.net2plan.internal.CustomHTMLEditorKit;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Auxiliary functions to work with HTML. The intended use is for {@link com.net2plan.interfaces.networkDesign.IReport reports}.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class HTMLUtils
{
	private HTMLUtils() { }
	
	/**
	 * Opens a browser for the given {@code URI}. It is supposed to avoid issues with KDE systems.
	 *
	 * @param uri URI to browse
	 */
	public static void browse(URI uri)
	{
		try
		{
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			{
				Desktop.getDesktop().browse(uri);
				return;
			}
			else if (SystemUtils.IS_OS_UNIX && (new File("/usr/bin/xdg-open").exists() || new File("/usr/local/bin/xdg-open").exists()))
			{
				new ProcessBuilder("xdg-open", uri.toString()).start();
				return;
			}
			
			throw new UnsupportedOperationException("Your operating system does not support launching browser from Net2Plan");
		}
		catch (IOException | UnsupportedOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>Returns the HTML text from a given file. It is a wrapper method for {@link #getHTMLFromURL getHTMLFromURL()}.</p>
	 *
	 * @param file A valid file
	 * @return A HTML String
	 */
	public static String getHTMLFromFile(File file)
	{
		try
		{
			URL url = file.toURI().toURL();
			return getHTMLFromURL(url);
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>Returns the HTML text from a given URL.</p>
	 *
	 * <p>This method is intended for HTML files enclosed into the same JAR file as that enclosing the calling class. Therefore, the URL must point the location of the HTML file within that JAR file.</p>
	 *
	 * <p>For example, assuming that a file named "example.html" is in the path "/aux-files/html" within the JAR file, then the calling would be as follows:</p>
	 *
	 * {@code String html = HTMLUtils.getHTMLFromURL(getClass().getResource("/aux-files/html/examples.html").toURI().toURL());}
	 *
	 * <p><b>Important</b>: Image paths are converted to absolute paths.</p>
	 *
	 * @param url A valid URL
	 * @return A HTML String
	 */
	public static String getHTMLFromURL(URL url)
	{
		try
		{
			StringBuilder content = new StringBuilder();
			URLConnection urlConnection = url.openConnection();
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = bufferedReader.readLine()) != null)
					content.append(line).append(StringUtils.getLineSeparator());
			}
			
			return prepareImagePath(content.toString(), url);
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}		
	}

	/**
	 * Converts an XML file to a formatted HTML output via an XSLT definition.
	 * 
	 * @param xml String containing an XML file
	 * @param xsl File containing an XSLT definition
	 * @return Formatted HTML output
	 */
	public static String getHTMLFromXML(String xml, File xsl)
	{
		try
		{
			URL url = xsl.toURI().toURL();
			return getHTMLFromXML(xml, url);
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Converts an XML file to a formatted HTML output via an XSLT definition.
	 * 
	 * @param xml String containing an XML file
	 * @param xsl URL containing an XSLT definition
	 * @return Formatted HTML output
	 */
	public static String getHTMLFromXML(String xml, URL xsl)
	{
        try
        {
            Source xmlDoc = new StreamSource(new StringReader(xml));
            Source xslDoc = new StreamSource(xsl.openStream());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(xslDoc);
            transformer.transform(xmlDoc, new StreamResult(baos));
			String html = baos.toString(StandardCharsets.UTF_8.name());
			html = prepareImagePath(html, xsl);
			return html;
        }
        catch(IOException | TransformerFactoryConfigurationError | TransformerException e)
        {
			throw new RuntimeException(e);
		}		
	}
	
	private static String prepareImagePath(String html, URL url)
	{
		final Set<String> list = new TreeSet<String>();
		final ParserDelegator parserDelegator = new ParserDelegator();
		final HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback()
		{
			@Override
			public void handleText(final char[] data, final int pos)
			{
			}

			@Override
			public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos)
			{
				if (tag == HTML.Tag.IMG)
				{
					String address = (String) attribute.getAttribute(HTML.Attribute.SRC);
					list.add(address);
				}
			}

			@Override
			public void handleEndTag(HTML.Tag t, final int pos)
			{
			}

			@Override
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, final int pos)
			{
				if (t == HTML.Tag.IMG)
				{
					String address = (String) a.getAttribute(HTML.Attribute.SRC);
					list.add(address);
				}
			}

			@Override
			public void handleComment(final char[] data, final int pos)
			{
			}

			@Override
			public void handleError(final String errMsg, final int pos)
			{
			}
		};

		final Reader reader = new StringReader(html);
		try	{ parserDelegator.parse(reader, parserCallback, true); }
		catch (IOException e) { throw new RuntimeException(e); }

		for (String item : list)
		{
			try
			{
				URL newURL = new URL(url, item);
				html = html.replace(item, newURL.toExternalForm());
			}
			catch (Throwable e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return html;
	}

	/**
	 * <p>Saves an HTML content to a given file. Plain text is automatically 
	 * wrapped into HTML.</p>
	 *
	 * @param file A valid file
	 * @param html HTML content
	 */
	public static void saveToFile(File file, String html)
	{
		CustomHTMLEditorKit kit = new CustomHTMLEditorKit();
		JEditorPane editor = new CustomHTMLEditorKit.CustomJEditorPane();
		editor.setEditorKit(kit);
		editor.setContentType("text/html");
		editor.setText(html);
		editor.setText(CustomHTMLEditorKit.includeNet2PlanHeader(editor.getText()));
		CustomHTMLEditorKit.saveToFile(file, editor.getText(), kit.getImages());
	}
}

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


package com.net2plan.documentation;


import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.io.File;
import java.util.Map;
// Taglet API
// Doclet API
// Used in register(Map)
@SuppressWarnings("unchecked")
public class Taglet_Description implements Taglet
{

	private static final String NAME = "net2plan.description";

  /**
   * Return the name of this custom tag.
   */
  public String getName() {
      return NAME;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in field documentation.
   * @return true since <code>@todo</code>
   * can be used in field documentation and false
   * otherwise.
   */
  public boolean inField() {
      return true;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in constructor documentation.
   * @return true since <code>@todo</code>
   * can be used in constructor documentation and false
   * otherwise.
   */
  public boolean inConstructor() {
      return true;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in method documentation.
   * @return true since <code>@todo</code>
   * can be used in method documentation and false
   * otherwise.
   */
  public boolean inMethod() {
      return true;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in method documentation.
   * @return true since <code>@todo</code>
   * can be used in overview documentation and false
   * otherwise.
   */
  public boolean inOverview() {
      return true;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in package documentation.
   * @return true since <code>@todo</code>
   * can be used in package documentation and false
   * otherwise.
   */
  public boolean inPackage() {
      return true;
  }

  /**
   * Will return true since <code>@todo</code>
   * can be used in type documentation (classes or interfaces).
   * @return true since <code>@todo</code>
   * can be used in type documentation and false
   * otherwise.
   */
  public boolean inType() {
      return true;
  }

  /**
   * Will return false since <code>@todo</code>
   * is not an inline tag.
   * @return false since <code>@todo</code>
   * is not an inline tag.
   */

  public boolean isInlineTag() {
      return false;
  }

  /**
   * Register this Taglet.
   * @param tagletMap  the map to register this tag to.
   */
  public static void register(Map tagletMap) {
     Taglet_Description tag = new Taglet_Description();
     Taglet t = (Taglet) tagletMap.get(tag.getName());
     if (t != null) {
         tagletMap.remove(tag.getName());
     }
     tagletMap.put(tag.getName(), tag);
  }

  /**
   * Given the <code>Tag</code> representation of this custom
   * tag, return its string representation.
   * @param tag   the <code>Tag</code> representation of this custom tag.
   */
  public String toString(Tag tag) 
  {
  	return toString (new Tag [] {tag});
  }

  /**
   * Given an array of <code>Tag</code>s representing this custom
   * tag, return its string representation.
   * @param tags  the array of <code>Tag</code>s representing of this custom tag.
   */
  public String toString(Tag[] tags) {
      if (tags.length == 0) {
          return null;
      }
      //String result = "\n<DT><B>" + HEADER + "</B></DT>";
      String result = "";
      result += "<div class=\"block\"><table cellpadding=2 cellspacing=0><tr><td>";
      for (int i = 0; i < tags.length; i++) {
          if (i > 0) {
              result += "<p></p>";
          }
          result += getAlgorithmAssociatedToTag (tags[i]).getDescription();
      }
      return result + "</td></tr></table></div>\n";
  }

  private IAlgorithm getAlgorithmAssociatedToTag (Tag tag)
  {
  	File javaFileObject = tag.holder ().position ().file();
  	String theClassName = ""; 
  	while (!javaFileObject.getName ().equals("net2plan"))
  	{
  		theClassName = javaFileObject.getName () + "." + theClassName;
  		if (javaFileObject.getParentFile() == null) break; else javaFileObject = javaFileObject.getParentFile();
  	}
  	theClassName = "com.net2plan." + theClassName;
  	theClassName = theClassName.substring(0 , theClassName.length ()-6); //without .java
  	
  	IAlgorithm alg = null;
    try 
    { 
      Class algorithmClass = Taglet_Description.class.getClassLoader().loadClass(theClassName); 
    	if (!IAlgorithm.class.isAssignableFrom(algorithmClass)) return null; // not an algorithm
    	alg = (IAlgorithm) algorithmClass.getConstructor().newInstance();
    } catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException ("Unexpected exception trying to load class of '" + theClassName + "'");
		}
    return alg;
  }
  
	private static String printInformation (IAlgorithm alg)
	{
		StringBuilder st = new StringBuilder();
		final String NEWLINE =  System.lineSeparator();
		st.append("<ul>" + NEWLINE);
		for (Triple<String, String, String> p : alg.getParameters())
		{
			final String name = p.getFirst().trim ();
			final String defaultString = p.getSecond ().trim ();
			final String description = p.getThird().trim ();
			st.append("<li>");
			st.append("<code>" + name + "</code>: ");
			if (defaultString.startsWith("#boolean#")) 
			{
				st.append ("<i>Boolean type (default: " + defaultString.substring ("#boolean#".length()) + ").</i> ");
			} else if (defaultString.startsWith("#select#")) 
			{
				st.append ("<i>Value to select within {");
				for (String value : StringUtils.split(defaultString.substring ("#select#".length()))) st.append (value + ", ");
				st.setLength(st.length() - 2); // deletes the last space and comma
				st.append ("}</i>");
			}
			else
			{
				st.append ("<i>Default: " + defaultString + "</i>");
			}
			st.append (" " + description);
			st.append("</li>" + NEWLINE);
		}
		
		st.append("</ul>" + NEWLINE);

		return st.toString();
	}

  
}

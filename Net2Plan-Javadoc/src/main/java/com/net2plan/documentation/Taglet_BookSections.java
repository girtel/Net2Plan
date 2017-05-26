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


import com.net2plan.utils.StringUtils;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;
// Taglet API
// Doclet API
// Used in register(Map)
@SuppressWarnings("unchecked")
public class Taglet_BookSections implements Taglet
{

	private static final String NAME = "net2plan.ocnbooksections";

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
     Taglet_BookSections tag = new Taglet_BookSections();
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
  public String toString(Tag tag) {
  	return toString (new Tag [] {tag});
  }

  /**
   * Given an array of <code>Tag</code>s representing this custom
   * tag, return its string representation.
   * @param tags  the array of <code>Tag</code>s representing of this custom tag.
   */
  public String toString(Tag[] tags) 
  {
      if (tags.length == 0) {
          return null;
      }
  		final String NEWLINE =  System.lineSeparator();
  		String result = "\n<DT><B>For more information see: "+ "</B>";
  		result += "</DT>";
  		result += "<div class=\"block\"><ul>" + NEWLINE;
  		result += "<li>";
  		int counter = 0;
  		for (int i = 0; i < tags.length; i++) 
  		{ 
  			for (String tag : StringUtils.split (tags[i].text() , ","))
  			{
  				String anchor = null;
  				tag = tag.trim (); 
  				if (tag.startsWith("Exercise")) { anchor = "Exercise" + tag.substring("Exercise".length ()).trim (); }
  				else if (tag.startsWith("Section")) { anchor = "Section" + tag.substring("Section".length ()).trim (); }
  				else throw new RuntimeException ("Unknow section id: " + tag);
  				if (counter > 0) result += ", ";
//  				result += "<b><a href=\"../../ocnbookSections.html#" + anchor + "\">" + tag + "</a></b>"; 
  				result += "<b><a href=\"../../../../../ocnbookSections.html#" + anchor + "\">" + tag + "</a></b>"; 
  				counter ++;
  			}
  			
  		}
//      result += "<table cellpadding=2 cellspacing=0><tr><td>";
      result += ", of the book: <a href=\"http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html\">P. Pavon-Marino, <cite>Optimization of computer networks. Modeling and algorithms. A hands-on approach</cite>, Wiley 2016.</a></p>" + NEWLINE;
//      result += "</td></tr></table>\n";
      result += "</li>" + NEWLINE;
      result += "</ul></div>" + NEWLINE;
      return result;
  }
  
}

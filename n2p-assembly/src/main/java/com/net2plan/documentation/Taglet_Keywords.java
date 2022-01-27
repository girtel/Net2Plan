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


import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;
// Taglet API
// Doclet API
// Used in register(Map)
@SuppressWarnings("unchecked")
public class Taglet_Keywords implements Taglet
{

	private static final String NAME = "net2plan.keywords";
  private static final String HEADER = "Keywords:";

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
     Taglet_Keywords tag = new Taglet_Keywords();
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
  public String toString(Tag[] tags) 
  {
		final String NEWLINE =  System.lineSeparator();
		if (CreateHTMLKeywords.keywordDescriptionMap == null) CreateHTMLKeywords.initializeKeywordDescriptionMap();

    if (tags.length == 0)return null;
    String result = "\n<DT><B>" + HEADER + "</B></DT>";
		result += "<div class=\"block\"><ul>" + NEWLINE;
		result += "<li>";
		int counter = 0;
    for (int i = 0; i < tags.length; i++) 
    {
        for (String tag : StringUtils.split(tags [i].text () , ","))
        {
          if (counter > 0) result += ",  ";
        	tag = tag.trim ();
          Pair<String,String> pair = CreateHTMLKeywords.keywordDescriptionMap.get (tag);
          result += "<a href=\"../../../../../keyword_" + pair.getSecond() + ".html\">" + tag  + "</a>";
          counter ++;
        }
    }
    result += "</li>" + NEWLINE;
    result += "</ul></div>" + NEWLINE;
    return result;
  }
  
}

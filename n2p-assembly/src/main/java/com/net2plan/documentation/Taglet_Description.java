package com.elighthouse.assembler.codeAndDocumentation;

import static com.elighthouse.sdk.j2html.TagCreator.b;
import static com.elighthouse.sdk.j2html.TagCreator.div;
import static com.elighthouse.sdk.j2html.TagCreator.each;
import static com.elighthouse.sdk.j2html.TagCreator.i;
import static com.elighthouse.sdk.j2html.TagCreator.iff;
import static com.elighthouse.sdk.j2html.TagCreator.join;
import static com.elighthouse.sdk.j2html.TagCreator.li;
import static com.elighthouse.sdk.j2html.TagCreator.p;
import static com.elighthouse.sdk.j2html.TagCreator.ul;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonStructure;
import javax.json.JsonValue.ValueType;
import javax.lang.model.element.Element;

import org.jgrapht.Graph;

import com.elighthouse.sdk.j2html.tags.DomContent;
import com.elighthouse.sdk.persist.AbstractPersistentElement;
import com.elighthouse.sdk.pub.mtn.Mtn;
import com.elighthouse.sdk.pub.mtn._HelpInformationObject;
import com.elighthouse.sdk.pub.utils.Pair;
//import com.sun.tools.doclets.Taglet;
import com.sun.source.doctree.DocTree;

import jdk.javadoc.doclet.Taglet;


// Doclet API
// Used in register(Map)
@SuppressWarnings("unchecked")
public class Taglet_Description implements Taglet
{
	private static final File currentSystemDirectory = new File (System.getProperty("user.dir")); 
	static final File parentFolderOfAllModules = currentSystemDirectory.getAbsoluteFile().getParentFile();

	private static final String NAME = "enp.description";
	private static boolean classLoaderAlreadyUpdated = false;

	/* Classes created to make the system class loader have them, then we have no problems with the tag classloader */
//	private static Graph aux1 = null;
//	private static JsonStructure aux2 = new JsonStructure() {
//		@Override
//		public ValueType getValueType() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}; 
	
	
  /**
   * Return the name of this custom tag.
   */
  @Override
  public String getName() { 
      return NAME;
  }

  /**
   * Will return false since <code>@todo</code>
   * is not an inline tag.
   * @return false since <code>@todo</code>
   * is not an inline tag.
   */

  @Override
public boolean isInlineTag() {
      return false;
  }

  private Class<?> getClassOfElement (Element tag)
  {
	  	try
	  	{
	  	  	final Class<?> res = Mtn.class.getClassLoader().loadClass(tag.toString());
	  	  	return res;
	  	} catch (Exception e)
	  	{
	  	  e.printStackTrace();
	  	  throw new RuntimeException();
	  	}
  }
  
//  private static void addPathToCurrentClassloader (File rootFolderForJavaClasses)
//  {
//	  //-Djava.system.class.loader=org.processmining.framework.util.ProMClassLoader
//	  try
//	  {
//		  final URI u = rootFolderForJavaClasses.toURI();
//		  final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//		  ClassLoader cl = systemClassLoader;
//		  System.out.println("Start: " + cl + ", is URLCLassLoader: " + URLClassLoader.class.isAssignableFrom(cl.getClass()));
//		  while (cl.getParent() != null) { cl = cl.getParent(); System.out.println(" " + cl + ", is URLCLassLoader: " + URLClassLoader.class.isAssignableFrom(cl.getClass())); }
//		  
//		  if (!(systemClassLoader instanceof URLClassLoader)) 
//		  { 
//			  System.out.println("The system class loader is not an URLClassLoader in Java 9 and after. We need an URL Class loader for this");
//		  }
//		  final URLClassLoader urlClassLoader = (URLClassLoader) systemClassLoader;
//		  final Class<URLClassLoader> urlClass = URLClassLoader.class;
//		  final Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
//		    method.setAccessible(true);
//		    method.invoke(urlClassLoader, new Object[]{u.toURL()});
//	  } catch (Exception e) { e.printStackTrace(); throw new RuntimeException (); }
////	  final ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
////	  ClassLoader urlCl = URLClassLoader.newInstance(new URL[]{new URL(url)}, prevCl);
//  }

	@Override
	public Set<Location> getAllowedLocations() 
	{
		System.out.println("Here...");
		return new HashSet<> (Arrays.asList(Taglet.Location.METHOD , Taglet.Location.TYPE , Taglet.Location.CONSTRUCTOR , 
				Taglet.Location.FIELD , Taglet.Location.OVERVIEW));
	
	}
	
	@Override
	public String toString(List<? extends DocTree> tags, Element element) 
	{
//		if (!classloaderalreadyupdated)
//		{
//			classloaderalreadyupdated = true;
//			system.out.println("registering the class path...");
//			try
//			{
////				  system.out.println("add this class path since this library is used inside getexternaldependencies_enpcore ");
//				  addpathtocurrentclassloader (new file ("c:\\users\\pablo\\.m2\\repository\\commons-io\\commons-io\\2.5\\commons-io-2.5.jar"));
//				  final list<file> deps = dependenciescutandpastes.getexternaldependencies_enpsdk();
//				  for (file f : deps)
//				  {
//					  system.out.println("trying to add in classloader : " + f);
//				  		addpathtocurrentclassloader (f);
//				  }
//			} catch(throwable e)
//			{
//				e.printstacktrace();
//			}
//		}
		
	      if (tags.size() == 0) return null;
	      if (tags.size() > 1) throw new RuntimeException ();

	      
	      Class<?> classMtnNe = getClassOfElement(element);
	      
//		  System.out.println("In tag...: " + classMtnNe.getSimpleName() );
//		  System.out.println("Classpath: " + System.getProperty("java.class.path"));

	      final List<Pair<Class , List<AbstractPersistentElement>>> elements = new ArrayList<> ();

	      _HelpInformationObject helpObject = null;
	      try
	      {
	    	  final Field fieldHelpInformation = classMtnNe.getDeclaredField("helpInformation");
	    	  fieldHelpInformation.setAccessible(true);
	          helpObject = (_HelpInformationObject) fieldHelpInformation.get(null);
	      } catch (Throwable eee) 
	      {
	    	  eee.printStackTrace();
	    	  System.out.println("Help object not found in class: " + classMtnNe.getName());
//	    	  Assembler_v1.log("Help object not found in class: " + classMtnNe.getName());
	    	  System.exit(-1);
	    	  return null;
	      }

	      try
	      {
	    	  Class<?> currentClass = classMtnNe;
	    	  
	    	  while (currentClass.getPackage().getName().startsWith("com.net2plan"))
	    	  {
	    		  final Field [] fields = currentClass.getDeclaredFields();
	    		  elements.add(Pair.of(currentClass, new ArrayList<> ()));
	    		  for (Field f : fields)
	    		  {
	    			  f.setAccessible(true);
	    			  if (!AbstractPersistentElement.class.isAssignableFrom(f.getType())) continue;
	    			  final AbstractPersistentElement<?,?> fieldObj = (AbstractPersistentElement<?,?>) f.get(null);
	    			  final boolean includeInHelp = fieldObj.isShownInJavadocForGivenClass(classMtnNe);
	    			  if (includeInHelp)
	    				  elements.get(elements.size()-1).getSecond().add(fieldObj);
	    		  }
	    		  currentClass = currentClass.getSuperclass();
	    		  if (currentClass == null) break;
	    	  }

	    	  /* Convert them into HTML */
	    	  final List<DomContent> infoPerType = new ArrayList<> ();
	    	  for (int c = 0; c < elements.size() ; c ++)
	    	  {
	    		  final Class<?> cl = elements.get(c).getFirst();
	    		  final List<AbstractPersistentElement> fields = elements.get(c).getSecond();
	    		  if (fields.isEmpty()) continue;
	    		  infoPerType.add(
	    				  join(
	    				  p(join ("Properties as an element of the class " ,i(cl.getSimpleName()))),
	    				  		ul
	    				  		(
	    				  				each(fields , f->
	    				  				{
	    				  					return li(join (b(f.getShortDescription() + ": ") , f.getLongDescription()));
	    				  				})
	    				  )));
	    	  }
	    	  
	    	  final DomContent res = div (
	    				div (helpObject.getMtnNeDescription ()),
	    				iff (!infoPerType.isEmpty() , 
	    						div
	    						(
	    				    			  p (join ("Below, you can find an enumeration of the different properties that characterize the elements of this type (" , i(classMtnNe.getSimpleName()) , ")")),
	    				    			  each (infoPerType , i->i)
	    						)
	    						)
	    			  );
//	    	  System.out.println("End tag...: " + classMtnNe.getSimpleName() );

	    	  return res.render();
	      } catch (Throwable e)
	      {
	    	  e.printStackTrace();
	    	  AssemblerCodeAndDocumentationMain.log("End tag...: " + classMtnNe.getSimpleName()  + " with Exception: " + e.getMessage());
	    	  System.out.println("End tag...: " + classMtnNe.getSimpleName()  + " with Exception");
	    	  throw new RuntimeException ();
	      }
	}
 

}

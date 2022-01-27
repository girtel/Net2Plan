package com.elighthouse.assembler.codeAndDocumentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.SystemOutHandler;

import com.elighthouse.assembler.codeAndDocumentation.AssemblerCodeAndDocumentationMain.ClientPlatform;
import com.elighthouse.assembler.constants.Constants;
import com.elighthouse.assembler.utils.Wget;
import com.elighthouse.assembler.utils.Wget.WgetStatus;
import com.elighthouse.sdk.pub.mtn.EnpException;
import com.elighthouse.sdk.pub.mtn.Mtn;
import com.elighthouse.sdk.pub.utils.Pair;


/**
 * Each of these dependencies should be copy pasted from Eclipse "Package Explorer"
 * enp-sdk: under enp-sdk - "Maven Dependencies" folder, copy paste the names (Copy Quualified name)
 * enp-gui: under enp-gui - "Maven Dependencies" folder, copy paste the names (Copy Quualified name)
 * enp-server: under enp-server - "Maven Dependencies" folder, copy paste the names (Copy Quualified name)
 *
 */
public class DependenciesCutAndPastes 
{
	//private final static File mavenRepositoryUsedInEclipse = new File ("C:\\Users\\Pablo\\.m2\\repository"); // not used

	private final static Map<File , List<File>> cache_dependenciesFromPom = new HashMap<> ();
	
	private final static String external_dep_enp_sdk = "C:\\Users\\Pablo\\.m2\\repository\\org\\assertj\\assertj-core\\3.7.0\\assertj-core-3.7.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-all\\1.13\\batik-all-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-anim\\1.13\\batik-anim-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-awt-util\\1.13\\batik-awt-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-bridge\\1.13\\batik-bridge-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-codec\\1.13\\batik-codec-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-constants\\1.13\\batik-constants-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-css\\1.13\\batik-css-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-dom\\1.13\\batik-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ext\\1.13\\batik-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-extension\\1.13\\batik-extension-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gui-util\\1.13\\batik-gui-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gvt\\1.13\\batik-gvt-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-i18n\\1.13\\batik-i18n-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-parser\\1.13\\batik-parser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer\\1.13\\batik-rasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer-ext\\1.13\\batik-rasterizer-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-script\\1.13\\batik-script-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-shared-resources\\1.13\\batik-shared-resources-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-slideshow\\1.13\\batik-slideshow-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle\\1.13\\batik-squiggle-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle-ext\\1.13\\batik-squiggle-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgbrowser\\1.13\\batik-svgbrowser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svg-dom\\1.13\\batik-svg-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svggen\\1.13\\batik-svggen-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgpp\\1.13\\batik-svgpp-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgrasterizer\\1.13\\batik-svgrasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-swing\\1.13\\batik-swing-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-transcoder\\1.13\\batik-transcoder-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ttf2svg\\1.13\\batik-ttf2svg-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-util\\1.13\\batik-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-xml\\1.13\\batik-xml-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcpkix-jdk15on\\1.68\\bcpkix-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcprov-jdk15on\\1.68\\bcprov-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bctls-jdk15on\\1.68\\bctls-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-codec\\commons-codec\\1.11\\commons-codec-1.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-collections4\\4.4\\commons-collections4-4.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-compress\\1.20\\commons-compress-1.20.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-io\\commons-io\\2.5\\commons-io-2.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-math3\\3.5\\commons-math3-3.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\virtuald\\curvesapi\\1.06\\curvesapi-1.06.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\fontbox\\2.0.22\\fontbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\de\\rototor\\pdfbox\\graphics2d\\0.30\\graphics2d-0.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\guava\\guava\\21.0\\guava-21.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\seancfoley\\ipaddress\\5.3.3\\ipaddress-5.3.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\activation\\jakarta.activation-api\\1.2.1\\jakarta.activation-api-1.2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\xml\\bind\\jakarta.xml.bind-api\\2.3.2\\jakarta.xml.bind-api-2.3.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\javassist\\javassist\\3.21.0-GA\\javassist-3.21.0-GA.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\glassfish\\javax.json\\1.1.2\\javax.json-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\javax\\json\\javax.json-api\\1.1.2\\javax.json-api-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.30\\jcl-over-slf4j-1.7.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jgrapht\\jgrapht-core\\1.5.1\\jgrapht-core-1.5.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jheaps\\jheaps\\0.13\\jheaps-0.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\junit\\junit\\4.13\\junit-4.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\pl\\pragmatists\\JUnitParams\\1.1.0\\JUnitParams-1.1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\pdfbox\\2.0.22\\pdfbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi\\5.0.0\\poi-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml\\5.0.0\\poi-ooxml-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml-lite\\5.0.0\\poi-ooxml-lite-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\reflections\\reflections\\0.9.11\\reflections-0.9.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\serializer\\2.7.2\\serializer-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.7.32\\slf4j-api-1.7.32.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\zaxxer\\SparseBitSet\\1.2\\SparseBitSet-1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\codehaus\\woodstox\\stax2-api\\4.0.0\\stax2-api-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\woodstox\\woodstox-core\\5.0.2\\woodstox-core-5.0.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\xalan\\2.7.2\\xalan-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis\\1.4.01\\xml-apis-1.4.01.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis-ext\\1.3.04\\xml-apis-ext-1.3.04.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlbeans\\xmlbeans\\4.0.0\\xmlbeans-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\xmlgraphics-commons\\2.4\\xmlgraphics-commons-2.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\santuario\\xmlsec\\2.2.1\\xmlsec-2.2.1.jar";

	
//	/home/jmmartinez/.m2/repository/junit/junit/4.13/junit-4.13.jar
//	/home/jmmartinez/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
//	/home/jmmartinez/.m2/repository/org/postgresql/postgresql/42.3.1/postgresql-42.3.1.jar
//	/home/jmmartinez/.m2/repository/org/checkerframework/checker-qual/3.5.0/checker-qual-3.5.0.jar
	
	private final static String external_dep_enp_gui = "C:\\Users\\Pablo\\.m2\\repository\\org\\assertj\\assertj-core\\3.7.0\\assertj-core-3.7.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-all\\1.13\\batik-all-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-anim\\1.13\\batik-anim-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-awt-util\\1.13\\batik-awt-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-bridge\\1.13\\batik-bridge-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-codec\\1.13\\batik-codec-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-constants\\1.13\\batik-constants-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-css\\1.13\\batik-css-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-dom\\1.13\\batik-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ext\\1.13\\batik-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-extension\\1.13\\batik-extension-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gui-util\\1.13\\batik-gui-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gvt\\1.13\\batik-gvt-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-i18n\\1.13\\batik-i18n-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-parser\\1.13\\batik-parser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer\\1.13\\batik-rasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer-ext\\1.13\\batik-rasterizer-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-script\\1.13\\batik-script-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-shared-resources\\1.13\\batik-shared-resources-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-slideshow\\1.13\\batik-slideshow-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle\\1.13\\batik-squiggle-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle-ext\\1.13\\batik-squiggle-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgbrowser\\1.13\\batik-svgbrowser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svg-dom\\1.13\\batik-svg-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svggen\\1.13\\batik-svggen-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgpp\\1.13\\batik-svgpp-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgrasterizer\\1.13\\batik-svgrasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-swing\\1.13\\batik-swing-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-transcoder\\1.13\\batik-transcoder-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ttf2svg\\1.13\\batik-ttf2svg-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-util\\1.13\\batik-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-xml\\1.13\\batik-xml-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcpkix-jdk15on\\1.68\\bcpkix-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcprov-jdk15on\\1.68\\bcprov-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bctls-jdk15on\\1.68\\bctls-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\collections\\collections-generic\\4.01\\collections-generic-4.01.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\colt\\colt\\1.2.0\\colt-1.2.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-codec\\commons-codec\\1.11\\commons-codec-1.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-collections4\\4.4\\commons-collections4-4.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-compress\\1.20\\commons-compress-1.20.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-io\\commons-io\\2.5\\commons-io-2.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.4\\commons-lang3-3.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-logging\\commons-logging\\1.1.1\\commons-logging-1.1.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-math3\\3.5\\commons-math3-3.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\concurrent\\concurrent\\1.3.4\\concurrent-1.3.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\virtuald\\curvesapi\\1.06\\curvesapi-1.06.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\jhlabs\\filters\\2.0.235\\filters-2.0.235.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\fontbox\\2.0.22\\fontbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\de\\rototor\\pdfbox\\graphics2d\\0.30\\graphics2d-0.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\guava\\guava\\21.0\\guava-21.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\seancfoley\\ipaddress\\5.3.3\\ipaddress-5.3.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\java3d\\j3d-core\\1.3.1\\j3d-core-1.3.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.8.0\\jackson-annotations-2.8.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.8.8\\jackson-core-2.8.8.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.8.8.1\\jackson-databind-2.8.8.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\activation\\jakarta.activation-api\\1.2.1\\jakarta.activation-api-1.2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\xml\\bind\\jakarta.xml.bind-api\\2.3.2\\jakarta.xml.bind-api-2.3.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-base\\17-ea+9\\javafx-base-17-ea+9.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-base\\17-ea+9\\javafx-base-17-ea+9-linux.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-base\\17-ea+9\\javafx-base-17-ea+9-mac.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-base\\17-ea+9\\javafx-base-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-controls\\15.0.1\\javafx-controls-15.0.1-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-controls\\17-ea+9\\javafx-controls-17-ea+9.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-fxml\\17-ea+9\\javafx-fxml-17-ea+9-linux.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-fxml\\17-ea+9\\javafx-fxml-17-ea+9-mac.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-fxml\\17-ea+9\\javafx-fxml-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-graphics\\17-ea+9\\javafx-graphics-17-ea+9.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-graphics\\17-ea+9\\javafx-graphics-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-media\\17-ea+9\\javafx-media-17-ea+9.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-media\\17-ea+9\\javafx-media-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-swing\\17-ea+9\\javafx-swing-17-ea+9-linux.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-swing\\17-ea+9\\javafx-swing-17-ea+9-mac.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-swing\\17-ea+9\\javafx-swing-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-web\\17-ea+9\\javafx-web-17-ea+9-linux.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-web\\17-ea+9\\javafx-web-17-ea+9-mac.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\openjfx\\javafx-web\\17-ea+9\\javafx-web-17-ea+9-win.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\javassist\\javassist\\3.21.0-GA\\javassist-3.21.0-GA.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\glassfish\\javax.json\\1.1.2\\javax.json-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\javax\\json\\javax.json-api\\1.1.2\\javax.json-api-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.30\\jcl-over-slf4j-1.7.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jfree\\jfreechart\\1.5.0\\jfreechart-1.5.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jgrapht\\jgrapht-core\\1.5.1\\jgrapht-core-1.5.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jheaps\\jheaps\\0.13\\jheaps-0.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-3d\\2.0\\jung-3d-2.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-algorithms\\2.0.1\\jung-algorithms-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-api\\2.0.1\\jung-api-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-graph-impl\\2.0.1\\jung-graph-impl-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-io\\2.1\\jung-io-2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-visualization\\2.0.1\\jung-visualization-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\junit\\junit\\4.13\\junit-4.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\pl\\pragmatists\\JUnitParams\\1.1.0\\JUnitParams-1.1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jxmapviewer\\jxmapviewer2\\2.4\\jxmapviewer2-2.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\miglayout\\miglayout-core\\5.0\\miglayout-core-5.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\miglayout\\miglayout-swing\\5.0\\miglayout-swing-5.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\pdfbox\\2.0.22\\pdfbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi\\5.0.0\\poi-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml\\5.0.0\\poi-ooxml-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml-lite\\5.0.0\\poi-ooxml-lite-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\reflections\\reflections\\0.9.11\\reflections-0.9.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\serializer\\2.7.2\\serializer-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.7.32\\slf4j-api-1.7.32.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\zaxxer\\SparseBitSet\\1.2\\SparseBitSet-1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\codehaus\\woodstox\\stax2-api\\4.0.0\\stax2-api-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\swinglabs\\swing-worker\\1.1\\swing-worker-1.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\swinglabs\\swingx\\1.6.1\\swingx-1.6.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\java3d\\vecmath\\1.3.1\\vecmath-1.3.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\woodstox\\woodstox-core\\5.0.2\\woodstox-core-5.0.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\xalan\\2.7.2\\xalan-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis\\1.4.01\\xml-apis-1.4.01.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis-ext\\1.3.04\\xml-apis-ext-1.3.04.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlbeans\\xmlbeans\\4.0.0\\xmlbeans-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\xmlgraphics-commons\\2.4\\xmlgraphics-commons-2.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\santuario\\xmlsec\\2.2.1\\xmlsec-2.2.1.jar"; 

	private final static String external_dep_enp_server = "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\rwl\\AMDJ\\1.0.1\\AMDJ-1.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\codehaus\\mojo\\animal-sniffer-annotations\\1.20\\animal-sniffer-annotations-1.20.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\android\\annotations\\4.1.1.4\\annotations-4.1.1.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\api-common\\1.10.1\\api-common-1.10.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\f2j\\arpack_combined_all\\0.1\\arpack_combined_all-0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\assertj\\assertj-core\\3.7.0\\assertj-core-3.7.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\auto\\value\\auto-value-annotations\\1.7.4\\auto-value-annotations-1.7.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-all\\1.13\\batik-all-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-anim\\1.13\\batik-anim-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-awt-util\\1.13\\batik-awt-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-bridge\\1.13\\batik-bridge-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-codec\\1.13\\batik-codec-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-constants\\1.13\\batik-constants-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-css\\1.13\\batik-css-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-dom\\1.13\\batik-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ext\\1.13\\batik-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-extension\\1.13\\batik-extension-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gui-util\\1.13\\batik-gui-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-gvt\\1.13\\batik-gvt-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-i18n\\1.13\\batik-i18n-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-parser\\1.13\\batik-parser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer\\1.13\\batik-rasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-rasterizer-ext\\1.13\\batik-rasterizer-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-script\\1.13\\batik-script-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-shared-resources\\1.13\\batik-shared-resources-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-slideshow\\1.13\\batik-slideshow-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle\\1.13\\batik-squiggle-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-squiggle-ext\\1.13\\batik-squiggle-ext-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgbrowser\\1.13\\batik-svgbrowser-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svg-dom\\1.13\\batik-svg-dom-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svggen\\1.13\\batik-svggen-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgpp\\1.13\\batik-svgpp-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-svgrasterizer\\1.13\\batik-svgrasterizer-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-swing\\1.13\\batik-swing-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-transcoder\\1.13\\batik-transcoder-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-ttf2svg\\1.13\\batik-ttf2svg-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-util\\1.13\\batik-util-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\batik-xml\\1.13\\batik-xml-1.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcpkix-jdk15on\\1.68\\bcpkix-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bcprov-jdk15on\\1.68\\bcprov-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\bouncycastle\\bctls-jdk15on\\1.68\\bctls-jdk15on-1.68.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\rwl\\BTFJ\\1.0.1\\BTFJ-1.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\checkerframework\\checker-compat-qual\\2.5.5\\checker-compat-qual-2.5.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\checkerframework\\checker-qual\\3.5.0\\checker-qual-3.5.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\rwl\\COLAMDJ\\1.0.1\\COLAMDJ-1.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\collections\\collections-generic\\4.01\\collections-generic-4.01.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\colt\\colt\\1.2.0\\colt-1.2.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-beanutils\\commons-beanutils\\1.9.3\\commons-beanutils-1.9.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-codec\\commons-codec\\1.11\\commons-codec-1.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-collections\\commons-collections\\3.2.2\\commons-collections-3.2.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-collections4\\4.4\\commons-collections4-4.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-compress\\1.20\\commons-compress-1.20.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-io\\commons-io\\2.5\\commons-io-2.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.4\\commons-lang3-3.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-math3\\3.5\\commons-math3-3.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\commons\\commons-text\\1.1\\commons-text-1.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\concurrent\\concurrent\\1.3.4\\concurrent-1.3.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\conscrypt\\conscrypt-openjdk-uber\\2.5.1\\conscrypt-openjdk-uber-2.5.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\jplasma\\core-lapack\\0.1\\core-lapack-0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\csparsej\\csparsej\\1.1.1\\csparsej-1.1.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\virtuald\\curvesapi\\1.06\\curvesapi-1.06.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\datastore\\datastore-v1-proto-client\\1.6.3\\datastore-v1-proto-client-1.6.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\lmax\\disruptor\\3.4.4\\disruptor-3.4.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\errorprone\\error_prone_annotations\\2.6.0\\error_prone_annotations-2.6.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\guava\\failureaccess\\1.0.1\\failureaccess-1.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\fontbox\\2.0.22\\fontbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\gax\\1.63.0\\gax-1.63.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\gax-grpc\\1.63.0\\gax-grpc-1.63.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\gax-httpjson\\0.80.0\\gax-httpjson-0.80.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api-client\\google-api-client\\1.31.3\\google-api-client-1.31.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\auth\\google-auth-library-credentials\\0.25.2\\google-auth-library-credentials-0.25.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\auth\\google-auth-library-oauth2-http\\0.25.2\\google-auth-library-oauth2-http-0.25.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\google-cloud-core\\1.94.5\\google-cloud-core-1.94.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\google-cloud-core-grpc\\1.94.5\\google-cloud-core-grpc-1.94.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\google-cloud-core-http\\1.94.5\\google-cloud-core-http-1.94.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\google-cloud-datastore\\1.106.1\\google-cloud-datastore-1.106.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\cloud\\google-cloud-logging\\2.2.1\\google-cloud-logging-2.2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client\\1.39.2\\google-http-client-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client-apache-v2\\1.39.2\\google-http-client-apache-v2-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client-appengine\\1.39.2\\google-http-client-appengine-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client-gson\\1.39.2\\google-http-client-gson-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client-jackson2\\1.39.2\\google-http-client-jackson2-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\http-client\\google-http-client-protobuf\\1.39.2\\google-http-client-protobuf-1.39.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\oauth-client\\google-oauth-client\\1.31.5\\google-oauth-client-1.31.5.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\de\\rototor\\pdfbox\\graphics2d\\0.30\\graphics2d-0.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-alts\\1.37.0\\grpc-alts-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-api\\1.37.0\\grpc-api-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-auth\\1.37.0\\grpc-auth-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-context\\1.37.0\\grpc-context-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-core\\1.37.0\\grpc-core-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-grpclb\\1.37.0\\grpc-grpclb-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-netty-shaded\\1.37.0\\grpc-netty-shaded-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-protobuf\\1.37.0\\grpc-protobuf-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-protobuf-lite\\1.37.0\\grpc-protobuf-lite-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\grpc\\grpc-stub\\1.37.0\\grpc-stub-1.37.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.8.6\\gson-2.8.6.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\guava\\guava\\21.0\\guava-21.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.5.13\\httpclient-4.5.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.4.14\\httpcore-4.4.14.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\seancfoley\\ipaddress\\5.3.3\\ipaddress-5.3.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\j2objc\\j2objc-annotations\\1.3\\j2objc-annotations-1.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\java3d\\j3d-core\\1.3.1\\j3d-core-1.3.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.12.2\\jackson-core-2.12.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\activation\\jakarta.activation-api\\1.2.1\\jakarta.activation-api-1.2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\jakarta\\xml\\bind\\jakarta.xml.bind-api\\2.3.2\\jakarta.xml.bind-api-2.3.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\javassist\\javassist\\3.21.0-GA\\javassist-3.21.0-GA.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\javax\\annotation\\javax.annotation-api\\1.3.2\\javax.annotation-api-1.3.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\glassfish\\javax.json\\1.1.2\\javax.json-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\javax\\json\\javax.json-api\\1.1.2\\javax.json-api-1.1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.30\\jcl-over-slf4j-1.7.30.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jgrapht\\jgrapht-core\\1.5.1\\jgrapht-core-1.5.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\jheaps\\jheaps\\0.13\\jheaps-0.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\github\\rwl\\JKLU\\1.0.0\\JKLU-1.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\java\\dev\\jna\\jna\\4.1.0\\jna-4.1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\jplasma\\jplasma\\1.2.0\\jplasma-1.2.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\jtransforms\\jtransforms\\2.4.0\\jtransforms-2.4.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-3d\\2.0\\jung-3d-2.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-algorithms\\2.0.1\\jung-algorithms-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-api\\2.0.1\\jung-api-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-graph-impl\\2.0.1\\jung-graph-impl-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-io\\2.1\\jung-io-2.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sf\\jung\\jung-visualization\\2.0.1\\jung-visualization-2.0.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\junit\\junit\\4.13\\junit-4.13.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\pl\\pragmatists\\JUnitParams\\1.1.0\\JUnitParams-1.1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\guava\\listenablefuture\\9999.0-empty-to-avoid-conflict-with-guava\\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-api\\2.14.1\\log4j-api-2.14.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-core\\2.14.1\\log4j-core-2.14.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-slf4j-impl\\2.14.1\\log4j-slf4j-impl-2.14.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\googlecode\\netlib-java\\netlib-java\\0.9.3\\netlib-java-0.9.3.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\opencensus\\opencensus-api\\0.28.0\\opencensus-api-0.28.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\opencensus\\opencensus-contrib-http-util\\0.28.0\\opencensus-contrib-http-util-0.28.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\opencsv\\opencsv\\4.0\\opencsv-4.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\parallelcolt\\optimization\\1.0\\optimization-1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\net\\sourceforge\\parallelcolt\\parallelcolt\\0.10.1\\parallelcolt-0.10.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\pdfbox\\pdfbox\\2.0.22\\pdfbox-2.0.22.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\io\\perfmark\\perfmark-api\\0.23.0\\perfmark-api-0.23.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi\\5.0.0\\poi-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml\\5.0.0\\poi-ooxml-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\poi\\poi-ooxml-lite\\5.0.0\\poi-ooxml-lite-5.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\postgresql\\postgresql\\42.3.1\\postgresql-42.3.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\protobuf\\protobuf-java\\3.15.8\\protobuf-java-3.15.8.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\protobuf\\protobuf-java-util\\3.15.8\\protobuf-java-util-3.15.8.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\grpc\\proto-google-cloud-datastore-v1\\0.89.1\\proto-google-cloud-datastore-v1-0.89.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\grpc\\proto-google-cloud-logging-v2\\0.87.1\\proto-google-cloud-logging-v2-0.87.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\grpc\\proto-google-common-protos\\2.1.0\\proto-google-common-protos-2.1.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\google\\api\\grpc\\proto-google-iam-v1\\1.0.11\\proto-google-iam-v1-1.0.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\reflections\\reflections\\0.9.11\\reflections-0.9.11.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\serializer\\2.7.2\\serializer-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.7.32\\slf4j-api-1.7.32.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\zaxxer\\SparseBitSet\\1.2\\SparseBitSet-1.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\codehaus\\woodstox\\stax2-api\\4.0.0\\stax2-api-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\threeten\\threetenbp\\1.5.0\\threetenbp-1.5.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\java3d\\vecmath\\1.3.1\\vecmath-1.3.1.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\com\\fasterxml\\woodstox\\woodstox-core\\5.0.2\\woodstox-core-5.0.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xalan\\xalan\\2.7.2\\xalan-2.7.2.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis\\1.4.01\\xml-apis-1.4.01.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\xml-apis\\xml-apis-ext\\1.3.04\\xml-apis-ext-1.3.04.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlbeans\\xmlbeans\\4.0.0\\xmlbeans-4.0.0.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\xmlgraphics\\xmlgraphics-commons\\2.4\\xmlgraphics-commons-2.4.jar\r\n"
			+ "C:\\Users\\Pablo\\.m2\\repository\\org\\apache\\santuario\\xmlsec\\2.2.1\\xmlsec-2.2.1.jar"; 

	public static List<File> getExternalDependenciesNotAutomatic_enpSdk ()  { String s = external_dep_enp_sdk; return getListFiles(s); }
	public static List<File> getExternalDependenciesNotAutomatic_enpGui ()  { String s = external_dep_enp_gui; return getListFiles(s); }
	public static List<File> getExternalDependenciesNotAutomatic_enpServer () { String s = external_dep_enp_server; return getListFiles(s); } 
	
	public static List<File> getExternalDependencies_enpSdk ()  { return getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_enpSdk);  }
	public static List<File> getExternalDependencies_enpServer () { 
		
		final List<File> depsIncludingEnpSdk = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_enpSdk);
		final List<File> depsIncludingEnpServer = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_enpServer);
		for (File f: depsIncludingEnpSdk) if (!depsIncludingEnpServer.contains(f)) depsIncludingEnpServer.add(f);
	
		return depsIncludingEnpServer;
	} 
	public static List<File> getExternalDependencies_enpGui ()  
	{ 
		final List<File> depsIncludingEnpSdk = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_enpSdk);
		final List<File> depsIncludingEnpGui = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_enpGui);
		for (File f: depsIncludingEnpSdk) if (!depsIncludingEnpGui.contains(f)) depsIncludingEnpGui.add(f);
		
		final SortedSet<String> fileNamesInLowerCase = depsIncludingEnpGui.stream().map(e->e.getName().toLowerCase()).collect(Collectors.toCollection(TreeSet::new));
		
		/* Check all multiplatform information is available */
		final List<String> allClassifierEnds = Arrays.asList("-win.jar", "-linux.jar" , "-mac.jar");
		final File temporaryDirectory = new File (System.getProperty("java.io.tmpdir"));
		if (!temporaryDirectory.exists()) Mtn.exRt();
		for (File f : new ArrayList<> (depsIncludingEnpGui))
		{
			for (String classifierEnd : allClassifierEnds)
			{
				if (!f.getName().toLowerCase().endsWith(classifierEnd)) continue;
				final String name = f.getName().substring(0, f.getName().length() - classifierEnd.length());
				assert (name + classifierEnd).equals(f.getName());
				for (String otherClassifier : allClassifierEnds)
				{
					if (otherClassifier.equals(classifierEnd)) continue;
					final String nameWithOtherPlatform = (name + otherClassifier);
					final String nameWithOtherPlatformLowerCase = (name + otherClassifier).toLowerCase();
					
					if (!fileNamesInLowerCase.contains(nameWithOtherPlatformLowerCase)) 
					{
						final String versionName = f.getParentFile().getName();
						final String arhtypeName3 = f.getParentFile().getParentFile().getName();
						final String arhtypeName2 = f.getParentFile().getParentFile().getParentFile().getName();
						final String arhtypeName1 = f.getParentFile().getParentFile().getParentFile().getParentFile().getName();
						final String url = "https://repo1.maven.org/maven2/" + arhtypeName1 + "/" + arhtypeName2 + "/" + arhtypeName3 + "/" + versionName + "/" + nameWithOtherPlatformLowerCase;
						final File outputFileTemporal = new File (temporaryDirectory , nameWithOtherPlatformLowerCase);
						depsIncludingEnpGui.add(outputFileTemporal);
						if (!outputFileTemporal.isFile())
						{
							try
							{
								/* file not exists also in temporary folder */
								System.out.println("Platform-dependent jar needed. Downloading into the temporary folder: " + outputFileTemporal);
								final WgetStatus res = Wget.wGet(url , outputFileTemporal);
								if (res != WgetStatus.Success) throw new RuntimeException ("Error accessing the url: " + url + ". Res: " + res);
							} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Error accessing the file"); }
						}
						else System.out.println("Platform-dependent jar needed. Already found in the temporary folder: " + outputFileTemporal);
							
						if (!outputFileTemporal.isFile()) Mtn.exRt();
					}
				}
			}
		}		
		
		
		final List<File> filesWithoutSdkAndWithPlatformDependentJars = new ArrayList<> ();
		final String myClassfierEnd = ".jar";
		final List<String> otherClassifierEnds = allClassifierEnds.stream().filter(e->!e.equals(myClassfierEnd)).collect(Collectors.toList());
		for (File f : depsIncludingEnpGui)
		{
			assert f.exists();
			if (f.getName().contains("enp-sdk")) continue;
			if (otherClassifierEnds.stream().anyMatch(e->f.getName().toLowerCase().endsWith(e))) continue;
			filesWithoutSdkAndWithPlatformDependentJars.add(f);
		}
		return filesWithoutSdkAndWithPlatformDependentJars;
//		return depsIncludingEnpSdk.stream().filter(e->!e.getName().contains("enp-sdk")).collect(Collectors.toList()); 
	}

	
	private static List<File> getListFiles (String s) 
	{
		try (BufferedReader targetReader = new BufferedReader(new CharSequenceReader(s)))
		{
			final List<File> res = new ArrayList<> ();
			while (true)
			{
				final String l = targetReader.readLine();
				if (l == null) break;
				//System.out.println("Line: " + l);
				final File f = new File (l);
				if (!f.isFile()) System.out.println("File not found: " + f);
				res.add(f);
			}
			targetReader.close();
			final List<File> resSorted = res.stream().sorted((f1,f2)->f1.getAbsolutePath().compareTo(f2.getAbsolutePath())).collect(Collectors.toCollection(ArrayList::new));
			return resSorted;
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException (e.getMessage());
		}
	}

		public static void main (String[] args) throws Throwable
		{
			final Pair<List<File>,List<File>> sdk_ok_aut = Pair.of (getExternalDependenciesNotAutomatic_enpSdk (),getExternalDependencies_enpSdk ());
			final Pair<List<File>,List<File>> gui_ok_aut = Pair.of (getExternalDependenciesNotAutomatic_enpGui (),getExternalDependencies_enpGui (ClientPlatform.Linux));
			final Pair<List<File>,List<File>> server_ok_aut = Pair.of (getExternalDependenciesNotAutomatic_enpServer (),getExternalDependencies_enpServer ());
			for (int cont= 0; cont < 3 ; cont ++)
			{
				if (cont == 0) System.out.println("Comparing SDK"); else if (cont == 1) System.out.println("Comparing GUI"); else System.out.println("Comparing server");
				final Pair<List<File>,List<File>> p = Arrays.asList(sdk_ok_aut , gui_ok_aut , server_ok_aut).get(cont);
				final List<File> okNotIncluded = p.getFirst().stream().filter(e->!p.getSecond().contains(e)).collect(Collectors.toList());
				final List<File> includedNotOk = p.getSecond().stream().filter(e->!p.getFirst().contains(e)).collect(Collectors.toList());
				System.out.println(" - Ok not included: (" + okNotIncluded.size() + "): " + okNotIncluded);
				System.out.println(" - Included not ok: (" + includedNotOk.size() + "): " + includedNotOk);
			}
		}

	    private static List<File> getDependenciesOfMavenModuleAllFilesExistOrError (File pomFile) 
	    {
	    	if (cache_dependenciesFromPom.containsKey(pomFile)) return cache_dependenciesFromPom.get(pomFile);
	    	try
	    	{
		    	final String dependenciesfileName = "dependencies.txt";
		    	final File dependenciesFile = new File (pomFile.getParentFile() , dependenciesfileName);
		    	
		    	final InvocationRequest request = new DefaultInvocationRequest();
		    	request.setPomFile(pomFile);
		    	request.setGoals(Arrays.asList("dependency:list"));
		    	final Properties properties = new Properties();
		    	properties.setProperty("outputFile", dependenciesfileName); // redirect output to a file
		    	properties.setProperty("outputAbsoluteArtifactFilename", "true"); // with paths
		    	properties.setProperty("includeScope", "runtime"); // only runtime (scope compile + runtime). if only interested in scope runtime, you may replace with excludeScope = compile
		    	properties.setProperty("appendOutput", "true"); // in multi module, if not the last module overwrites
		    	
		    	request.setProperties(properties);
//		    	request.setLocalRepositoryDirectory(mavenRepositoryUsedInEclipse);
		    	
		    	final Invoker invoker = new DefaultInvoker();
		    	// the Maven home can be omitted if the "maven.home" system property is set
		    	invoker.setMavenHome(Constants.computerThisIsRunning.folderOfMavenInstallation);
		    	//invoker.setOutputHandler(null); // not interested in Maven output itself
		    	invoker.setOutputHandler(new SystemOutHandler(true)); // null = not interested in Maven output itself
		    	
		    	final InvocationResult result = invoker.execute(request);
		    	final int exitCode = result.getExitCode();
		    	if (exitCode != 0) 
		    	    throw new IllegalStateException("Build failed. Exit code: " + exitCode);
		    	
		    	final List<File> dependencyFiles = new ArrayList<> ();
		    	Pattern pattern = Pattern.compile("(?:compile|runtime):(.*)");
		    	
		    	try (BufferedReader reader = Files.newBufferedReader(dependenciesFile.toPath())) 
		    	{
		    	    while (!"The following files have been resolved:".equals(reader.readLine()));
		    	    String line;
		    	    while ((line = reader.readLine()) != null && !line.isEmpty()) 
		    	    {
		    	        Matcher matcher = pattern.matcher(line);
		    	        if (matcher.find()) 
		    	        {
		    	            // group 1 contains the path to the file
		    	            //System.out.println(matcher.group(1));
		    	            final File f = new File (matcher.group(1));
		    	            if (!f.isFile()) throw new EnpException("The dependency file " + f + " should exist but it does not");
		    	            dependencyFiles.add(f);
		    	        }
		    	    }
		    	}
		    	FileUtils.deleteQuietly(dependenciesFile);
		    	if (dependenciesFile.exists()) throw new RuntimeException ("Could not delete de file: " + dependenciesFile);
		    	
				final List<File> resSorted = dependencyFiles.stream().sorted((f1,f2)->f1.getAbsolutePath().compareTo(f2.getAbsolutePath())).collect(Collectors.toCollection(ArrayList::new));
				
		    	cache_dependenciesFromPom.put(pomFile , resSorted);
				return resSorted;
	    	} catch (Throwable e) 
	    	{
	    		e.printStackTrace();
	    		throw new RuntimeException(e.getMessage());
	    	}
	    }

	    
	    /* Coming from: https://stackoverflow.com/questions/39638138/find-all-direct-dependencies-of-an-artifact-on-maven-central/39641359#39641359
	     * 
	     * What this does is creating a Aether repository system and telling it to read the artifact descriptor of a given artifact. 
	     * 
	     * 
	     * artifactToCheckDependencies in form: groupId:artifactId:version
	     */
//	    private static void getDependencies_aether (String artifactToCheckDependencies) throws Throwable
//	    {
//	        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
//	        RepositorySystem system = newRepositorySystem(locator);
//	        RepositorySystemSession session = newSession(system);
//
//	        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
//	        RemoteRepository localMaven = new RemoteRepository.Builder("localMaven", "default", "http://repo1.maven.org/maven2/").build();
//
//	        Artifact artifact = new DefaultArtifact(artifactToCheckDependencies);
//	        
//	        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), Arrays.asList(central));
//	        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
//	        DependencyRequest request = new DependencyRequest(collectRequest, filter);
//	        DependencyResult result = system.resolveDependencies(session, request);
//
//	        for (ArtifactResult artifactResult : result.getArtifactResults()) {
//	            System.out.println(artifactResult.getArtifact().getFile());
//	        }
//	        
//	        
////	        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(artifact, Arrays.asList(central), null);
////	        ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);
//	//
////	        for (Dependency dependency : result.getDependencies()) {
////	            System.out.println(dependency);
////	        }
//	    	
//	    }
//	    
//	    private static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) 
//	    {
//	        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
//	        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
//	        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
//	        return locator.getService(RepositorySystem.class);
//	    }
//
//	    private static RepositorySystemSession newSession(RepositorySystem system) {
//	        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
//	        LocalRepository localRepo = new LocalRepository("target/local-repo");
//	        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
//	        // set possible proxies and mirrors
//	        session.setProxySelector(new DefaultProxySelector().add(new Proxy(Proxy.TYPE_HTTP, "host", 3625), Arrays.asList("localhost", "127.0.0.1")));
//	        session.setMirrorSelector(new DefaultMirrorSelector().add("my-mirror", "http://mirror", "default", false, "external:*", null));
//	        return session;
//	    }

		
}


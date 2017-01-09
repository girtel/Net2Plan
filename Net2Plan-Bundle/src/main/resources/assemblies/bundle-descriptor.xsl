<assembly xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3"
          xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3 http://maven.apache.org/xsd/assembly-1.1.3.xsd">

    <id>bundle-descriptor</id>

    <formats>
        <format>zip</format>
    </formats>

    <includeBaseDirectory>false</includeBaseDirectory>

    <fileSets>
        <!-- Add external resources -->
        <fileSet>
            <directory>src/main/resources/data</directory>
            <outputDirectory>workspace/data</outputDirectory>
            <useDefaultExcludes>true</useDefaultExcludes>
        </fileSet>
        <fileSet>
            <directory>src/main/resources/help</directory>
            <outputDirectory>doc/help</outputDirectory>
            <useDefaultExcludes>true</useDefaultExcludes>
        </fileSet>
    </fileSets>

</assembly>
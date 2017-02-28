<assembly>
    <id>javadoc</id> <!-- Classifier -->

    <formats>
        <format>zip</format>
    </formats>

    <includeBaseDirectory>false</includeBaseDirectory>

    <fileSets>
        <fileSet>
            <directory>${project.build.directory}/apidocs</directory>
            <outputDirectory>.</outputDirectory>
            <useDefaultExcludes>true</useDefaultExcludes>
        </fileSet>
        <!-- Book cover -->
        <fileSet>
            <directory>${project.basedir}/src/main/resources/resources/documentation</directory>
            <outputDirectory>api/doc-files</outputDirectory>
            <useDefaultExcludes>true</useDefaultExcludes>
        </fileSet>
        <fileSet>
            <directory>${project.basedir}/src/main/resources/resources/documentation</directory>
            <outputDirectory>examples/doc-files</outputDirectory>
            <useDefaultExcludes>true</useDefaultExcludes>
        </fileSet>
    </fileSets>
</assembly>
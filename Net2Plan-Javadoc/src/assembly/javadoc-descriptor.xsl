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
    </fileSets>
</assembly>
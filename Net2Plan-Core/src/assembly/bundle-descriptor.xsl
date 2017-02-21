<assembly>
    <id>bundle</id> <!-- Classifier -->

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
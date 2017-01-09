<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3">

    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <dependencySets>
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>true</unpack>
            <includes>
                <include>*:zip:bundle</include>
            </includes>
        </dependencySet>
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>*:*:jar</include>
            </includes>
        </dependencySet>
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>true</useTransitiveDependencies>
            <unpack>false</unpack>
            <outputDirectory>lib</outputDirectory>
            <excludes>
                <exclude>*:zip:bundle</exclude>
            </excludes>
        </dependencySet>
    </dependencySets>
</assembly>
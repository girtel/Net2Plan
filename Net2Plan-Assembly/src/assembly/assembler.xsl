<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3">

    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <dependencySets>
        <!--GUI Plugin-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}:net2plan-plugins:*</include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultGUITools.jar</outputFileNameMapping>
        </dependencySet>
        <!--CLI Plugin-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-core.net2plan-cli:net2plan-cli-plugins:*</include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultCLITools.jar</outputFileNameMapping>
        </dependencySet>
        <!--IO Plugin-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}:net2plan-io:*</include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultIO.jar</outputFileNameMapping>
        </dependencySet>
        <!--Examples-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}:net2plan-examples:*</include>
            </includes>
            <outputDirectory>workspace</outputDirectory>
            <outputFileNameMapping>BuiltInExamples.jar</outputFileNameMapping>
        </dependencySet>
        <!--NetPlan Lib-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-core:net2plan-lib:jar</include>
            </includes>
            <outputDirectory>lib</outputDirectory>
            <outputFileNameMapping>NetPlan-lib.jar</outputFileNameMapping>
        </dependencySet>
        <!--CLI Main-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-core.net2plan-cli:net2plan-cli-exec:*</include>
            </includes>
            <outputDirectory/>
            <outputFileNameMapping>Net2Plan-CLI.jar</outputFileNameMapping>
        </dependencySet>
        <!--GUI Main-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-core:net2plan-gui:*</include>
            </includes>
            <outputDirectory/>
            <outputFileNameMapping>Net2Plan.jar</outputFileNameMapping>
        </dependencySet>
        <!--External resources-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>true</unpack>
            <includes>
                <include>${project.groupId}.net2plan-core:net2plan-lib:zip:bundle</include>
            </includes>
            <outputDirectory/>
        </dependencySet>
        <!--Dependencies-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>true</useTransitiveDependencies>
            <unpack>false</unpack>
            <outputDirectory>lib</outputDirectory>
            <excludes>
                <exclude>${project.groupId}:*:*</exclude>
                <exclude>${project.groupId}.net2plan-core:*:*</exclude>
                <exclude>${project.groupId}.net2plan-core.net2plan-cli:*:*</exclude>
            </excludes>
        </dependencySet>
    </dependencySets>

    <fileSets>
        <fileSet>
            <directory>${project.parent.basedir}</directory>
            <outputDirectory/>
            <includes>
                <include>README*</include>
                <include>LICENSE*</include>
                <include>NOTICE*</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
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
                <include>${project.groupId}:net2plan-gui:*</include>
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
                <include>${project.groupId}.net2plan-cli:net2plan-cli-plugin:*</include>
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
                <include>${project.groupId}.net2plan-core:net2plan-core-lib:jar</include>
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
                <include>${project.groupId}.net2plan-cli:net2plan-cli-core:*</include>
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
                <include>${project.groupId}.net2plan-core:net2plan-core-main:*</include>
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
                <include>${project.groupId}.net2plan-core:net2plan-core-lib:zip:bundle</include>
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
                <exclude>${project.groupId}.net2plan-cli:*:*</exclude>
                <exclude>${project.groupId}.net2plan-core:*:*</exclude>
            </excludes>
        </dependencySet>
    </dependencySets>
</assembly>
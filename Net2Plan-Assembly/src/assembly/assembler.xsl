<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.0.0 http://maven.apache.org/xsd/assembly-2.0.0.xsd">

    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <fileSets>
        <!-- Add info files -->
        <fileSet>
            <directory>${project.parent.basedir}</directory>
            <outputDirectory/>
            <includes>
                <include>README*</include>
                <include>LICENSE*</include>
                <include>NOTICE*</include>
                <include>CHANGELOG*</include>
            </includes>
        </fileSet>
        <!-- Add licenses -->
        <fileSet>
            <directory>${project.build.directory}/generated-resources</directory>
            <outputDirectory>lib/</outputDirectory>
            <includes>
                <include>**</include>
            </includes>
        </fileSet>
    </fileSets>

    <dependencySets>
        <!--GUI Plugin-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-gui.net2plan-gui-plugins:net2plan-gui-plugins-networkDesign:*
                </include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultNetworkDesign.jar</outputFileNameMapping>
        </dependencySet>
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-gui.net2plan-gui-plugins:net2plan-gui-plugins-trafficDesign:*
                </include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultTrafficDesign.jar</outputFileNameMapping>
        </dependencySet>
        <!--CLI Plugin-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-cli:net2plan-cli-plugins:*</include>
            </includes>
            <outputDirectory>plugins</outputDirectory>
            <outputFileNameMapping>defaultCLITools.jar</outputFileNameMapping>
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
        <!--NetPlan Core-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}:net2plan-core:jar</include>
            </includes>
            <outputDirectory>lib</outputDirectory>
            <outputFileNameMapping>net2plan-core.jar</outputFileNameMapping>
        </dependencySet>
        <!--CLI Main-->
        <dependencySet>
            <useProjectArtifact>false</useProjectArtifact>
            <useTransitiveDependencies>false</useTransitiveDependencies>
            <unpack>false</unpack>
            <includes>
                <include>${project.groupId}.net2plan-cli:net2plan-cli-exec:*</include>
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
                <include>${project.groupId}.net2plan-gui:net2plan-gui-exec:*</include>
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
                <include>${project.groupId}:net2plan-core:zip:bundle</include>
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
                <exclude>${project.groupId}.net2plan-gui:*:*</exclude>
                <exclude>${project.groupId}.net2plan-cli:*:*</exclude>
                <exclude>${project.groupId}.net2plan-gui.net2plan-gui-plugins:*:*</exclude>
            </excludes>
        </dependencySet>
    </dependencySets>

    <!-- Javadoc -->
    <moduleSets>
        <moduleSet>
            <useAllReactorProjects>true</useAllReactorProjects>
            <includes>
                <include>${project.groupId}:net2plan-javadoc</include>
            </includes>
            <binaries>
                <unpack>true</unpack>
                <attachmentClassifier>javadoc</attachmentClassifier>
                <outputDirectory>doc/javadoc</outputDirectory>
                <includeDependencies>false</includeDependencies>
            </binaries>
        </moduleSet>
    </moduleSets>
</assembly>
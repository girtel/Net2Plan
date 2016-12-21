<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project default="build-jar">
    <property name="build.dir" value="${basedir}"/>

    <target name="build-jar">
        <jar destfile = "${build.dir}"
             basedir = "${build.dir}/classes"
             includes = "${build.dir}/classes/com/net2plan/prooves/**">


        </jar>
    </target>
</project>
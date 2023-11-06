package com.net2plan.internal;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class ClassLoad
{

    // New JDK9+ class loading, Runtime Agent
    private static Instrumentation agent = null;
    public static void agentmain(final String a, final Instrumentation instrumentation) { agent = instrumentation; }
    public static void premain(final String a, final Instrumentation instrumentation) { agent = instrumentation; }

    public static void loadClass(File f)
    {
        try
        {
            agent.appendToSystemClassLoaderSearch(new JarFile(f));
        } catch (IOException e)
        {
            System.out.println("File could not be opened");
            throw new RuntimeException(e);
        }
    }
}

package com.net2plan.cli.plugins.utils;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * Created by Jorge San Emeterio on 20/03/17.
 */
public final class ClassUtils
{
    private ClassUtils() {}

    public static IAlgorithm findAlgorithm(final String algorithmName, final String packageName)
    {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAlgorithm>> algorithms = reflections.getSubTypesOf(IAlgorithm.class);

        IAlgorithm algorithm = null;
        try
        {
            for (Class<?> algorithmClass : algorithms)
            {
                if (algorithmClass.getSimpleName().equals(algorithmName))
                {
                    final Class<?> classDefinition = Class.forName(algorithmClass.getName());
                    final Constructor<?> constructor = classDefinition.getConstructor();
                    final Object object = constructor.newInstance();

                    if (object instanceof IAlgorithm)
                    {
                        algorithm = (IAlgorithm) object;
                        break;
                    }
                }
            }

            if (algorithm == null) throw new Exception("Algorithm not found: " + algorithmName + " at " + packageName);

            return algorithm;
        } catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}

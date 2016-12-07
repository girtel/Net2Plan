package com.net2plan.internal

import org.reflections.Reflections
import com.net2plan.interfaces.networkDesign.IAlgorithm

/**
 * @author Jorge San Emeterio
 * @date 05-Dec-16
 */
class GroovyAlgorithmUtils
{
    static Set<Class<IExternal>> getScriptAlgorithms()
    {
        return new Reflections('com.net2plan.examples.groovy').getSubTypesOf(IAlgorithm)
    }
}

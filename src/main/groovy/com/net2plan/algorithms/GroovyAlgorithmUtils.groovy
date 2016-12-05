package com.net2plan.algorithms

import org.reflections.Reflections
import com.net2plan.interfaces.networkDesign.IAlgorithm

/**
 * @author Jorge San Emeterio
 * @date 05-Dec-16
 */
class GroovyAlgorithmUtils
{
    static Set<String> getScriptAlgorithms()
    {
        Set<String> algorithmList = new HashSet<>()

        new Reflections('com.net2plan.examples.groovy').getSubTypesOf(IAlgorithm).each {it -> algorithmList.add(it.name)}

        return algorithmList
    }
}

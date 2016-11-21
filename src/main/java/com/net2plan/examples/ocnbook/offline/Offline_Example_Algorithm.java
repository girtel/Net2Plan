package com.net2plan.examples.ocnbook.offline;

import com.google.common.base.Splitter;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 21-Nov-16
 */
public class Offline_Example_Algorithm implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        final String simpleParameter = algorithmParameters.get("Simple parameter");
        final String choiceParameter = algorithmParameters.get("Choice parameter");
        final boolean booleanParameter = Boolean.parseBoolean(algorithmParameters.get("Boolean parameter"));
        final String fileParameter = algorithmParameters.get("File parameter");
        final String multipleFileParameter = algorithmParameters.get("Multiple file parameter");

        System.out.println("Simple parameter: " + simpleParameter);
        System.out.println("Choice parameter: " + choiceParameter);
        System.out.println("Boolean parameter: " + booleanParameter);
        System.out.println("File parameter: " + fileParameter);

        System.out.println("Multiple file parameter: ");
        final Iterable<String> multipleFilePaths = Splitter.on("<>").split(multipleFileParameter);

        for (String multipleFilePath : multipleFilePaths)
        {
            System.out.println(multipleFilePath);
        }

        return "DONE!";
    }

    @Override
    public String getDescription()
    {
        return "Example algorithm showing the use of the algorithm interface and its components.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithm = new ArrayList<>();

        algorithm.add(Triple.of("Simple parameter", "", "The user may enter the desired value in a string format."));
        algorithm.add(Triple.of("Choice parameter", "#select# First Second Third", "Allows the user to choose from a given array of choices."));
        algorithm.add(Triple.of("Boolean parameter", "#boolean#", "Represents a true/false parameter through the use of a checkbox."));
        algorithm.add(Triple.of("File parameter", "#file#", "Brings up a file selector to choose one single file."));
        algorithm.add(Triple.of("Multiple file parameter", "#files#", "Brings up a file selector to choose multiple files. The files' paths are separated with the string '||'."));
        return algorithm;
    }
}

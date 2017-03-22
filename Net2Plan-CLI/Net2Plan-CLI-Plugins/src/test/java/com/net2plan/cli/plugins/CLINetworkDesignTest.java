package com.net2plan.cli.plugins;

import com.net2plan.utils.StringUtils;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 22/03/17.
 */
public class CLINetworkDesignTest
{
    private final static String DEFAULT_PACKAGE = "com.net2plan.examples";
    private final static String DEFAULT_ALGORITHM = "Offline_Example_Algorithm";
    private final static String DEFAULT_OUTPUT = "out.n2p";

    private final static CLINetworkDesign networkDesign = new CLINetworkDesign();

    @Test(expected = ParseException.class)
    public void launchNoOptionParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--class-name", DEFAULT_ALGORITHM);
        paramMap.put("--output-file", DEFAULT_OUTPUT);

        final List<String> args = new LinkedList<>();
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        networkDesign.executeFromCommandLine(StringUtils.toArray(args));
    }

    @Test(expected = ParseException.class)
    public void launchNoClassNameParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--package-name", DEFAULT_PACKAGE);
        paramMap.put("--output-file", DEFAULT_OUTPUT);

        final List<String> args = new LinkedList<>();
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        networkDesign.executeFromCommandLine(StringUtils.toArray(args));
    }

    @Test(expected = ParseException.class)
    public void launchNoOutputFileParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--package-name", DEFAULT_PACKAGE);
        paramMap.put("--class-name", DEFAULT_ALGORITHM);

        final List<String> args = new LinkedList<>();
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        networkDesign.executeFromCommandLine(StringUtils.toArray(args));
    }

    @Test(expected = RuntimeException.class)
    public void launchBothOptionParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--class-file", "null");
        paramMap.put("--package-name", DEFAULT_PACKAGE);
        paramMap.put("--class-name", DEFAULT_ALGORITHM);
        paramMap.put("--output-file", DEFAULT_OUTPUT);

        final List<String> args = new LinkedList<>();
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        networkDesign.executeFromCommandLine(StringUtils.toArray(args));
    }
}

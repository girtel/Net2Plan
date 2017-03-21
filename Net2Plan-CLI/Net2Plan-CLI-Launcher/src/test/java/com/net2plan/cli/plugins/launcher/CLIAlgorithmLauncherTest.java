package com.net2plan.cli.plugins.launcher;

import com.net2plan.utils.StringUtils;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 21/03/17.
 */
public class CLIAlgorithmLauncherTest
{
    private final static String DEFAULT_FILE = "internal-algorithm";
    private final static String DEFAULT_PACKAGE = "com.net2plan.examples";
    private final static String DEFAULT_ALGORITHM = "Offline_Example_Algorithm";
    private final static String DEFAULT_OUTPUT = "out.n2p";

    @Test(expected = ParseException.class)
    public void launchNoClassFileParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--package-search", DEFAULT_PACKAGE);
        paramMap.put("--class-name", DEFAULT_ALGORITHM);
        paramMap.put("--output-file", DEFAULT_OUTPUT);

        final List<String> args = new LinkedList<>();
        args.add("--internal-search");
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        CLIAlgorithmLauncher.main(StringUtils.toArray(args));
    }

    @Test(expected = ParseException.class)
    public void launchNoClassNameParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--class-file", DEFAULT_FILE);
        paramMap.put("--package-search", DEFAULT_PACKAGE);
        paramMap.put("--output-file", DEFAULT_OUTPUT);

        final List<String> args = new LinkedList<>();
        args.add("--internal-search");
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        CLIAlgorithmLauncher.main(StringUtils.toArray(args));
    }

    @Test(expected = ParseException.class)
    public void launchNoOutputFileParam() throws ParseException
    {
        final Map<String, String> paramMap = new HashMap<>();

        paramMap.put("--class-file", DEFAULT_FILE);
        paramMap.put("--package-search", DEFAULT_PACKAGE);
        paramMap.put("--class-name", DEFAULT_ALGORITHM);

        final List<String> args = new LinkedList<>();
        args.add("--internal-search");
        for (Map.Entry<String, String> entry : paramMap.entrySet())
        {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }

        CLIAlgorithmLauncher.main(StringUtils.toArray(args));
    }
}

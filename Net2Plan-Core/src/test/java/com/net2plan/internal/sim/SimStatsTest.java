package com.net2plan.internal.sim;

import com.net2plan.utils.HTMLUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

public class SimStatsTest
{

	/**
	 * The SimStat XLS is found by the class
	 */
	@Test
	public void testFindXLS()
	{
		File xlsFile = FileUtils.toFile(SimStats.class.getResource("/sim/SimStats.xsl"));

		assertThat(xlsFile).isNotNull();
	}
}
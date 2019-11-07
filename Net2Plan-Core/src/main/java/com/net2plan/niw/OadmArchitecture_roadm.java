package com.net2plan.niw;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

public class OadmArchitecture_roadm implements IOadmArchitecture
{
	private WNode node;
	public OadmArchitecture_roadm ()
	{
	}
	
	@Override
	public WNode getHostNode () { return this.node; }


}

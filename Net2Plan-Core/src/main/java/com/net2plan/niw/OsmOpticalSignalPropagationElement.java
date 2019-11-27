package com.net2plan.niw;

import java.util.Comparator;

import com.net2plan.utils.Pair;

public class OsmOpticalSignalPropagationElement implements Comparator<OsmOpticalSignalPropagationElement>
{
	private WFiber fiber = null;
	private Pair<WNode,Integer> dirlessAddModule = null, dirlessDropModule = null;
	private WFiber dirfulAddModule = null, dirfulDropModule = null;
	public static OsmOpticalSignalPropagationElement asFiber (WFiber e) { OsmOpticalSignalPropagationElement res = new OsmOpticalSignalPropagationElement (); res.fiber = e; return res; }
	public static OsmOpticalSignalPropagationElement asAddDirless (Pair<WNode,Integer> e) { OsmOpticalSignalPropagationElement res = new OsmOpticalSignalPropagationElement (); res.dirlessAddModule = e; return res;  }
	public static OsmOpticalSignalPropagationElement asDropDirless (Pair<WNode,Integer> e) { OsmOpticalSignalPropagationElement res = new OsmOpticalSignalPropagationElement (); res.dirlessDropModule = e; return res;  }
	public static OsmOpticalSignalPropagationElement asAddDirful(WFiber outFiber) { OsmOpticalSignalPropagationElement res = new OsmOpticalSignalPropagationElement (); res.dirfulAddModule = outFiber; return res; }
	public static OsmOpticalSignalPropagationElement asDropDirful(WFiber inFiber) { OsmOpticalSignalPropagationElement res = new OsmOpticalSignalPropagationElement (); res.dirfulDropModule = inFiber; return res; }
	
	public WFiber getFiber() { return fiber; }
	public Pair<WNode, Integer> getDirlessAddModule() { return dirlessAddModule; }
	public Pair<WNode, Integer> getDirlessDropModule() { return dirlessDropModule; }
	public boolean isDirfulAdd() { return dirfulAddModule != null; }
	public boolean isDirfulDrop() { return dirfulDropModule != null; }
	public boolean isDirlessAdd() { return dirlessAddModule != null; }
	public boolean isDirlessDrop() { return dirlessDropModule != null; }
	public boolean isFiber() { return fiber != null; }
	public WFiber getDirfulAddOutFiber () { return dirfulAddModule; }
	public WFiber getDirfulDropInFiber () { return dirfulDropModule; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dirfulAddModule == null) ? 0 : dirfulAddModule.hashCode());
		result = prime * result + ((dirfulDropModule == null) ? 0 : dirfulDropModule.hashCode());
		result = prime * result + ((dirlessAddModule == null) ? 0 : dirlessAddModule.hashCode());
		result = prime * result + ((dirlessDropModule == null) ? 0 : dirlessDropModule.hashCode());
		result = prime * result + ((fiber == null) ? 0 : fiber.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OsmOpticalSignalPropagationElement other = (OsmOpticalSignalPropagationElement) obj;
		if (dirfulAddModule == null) {
			if (other.dirfulAddModule != null)
				return false;
		} else if (!dirfulAddModule.equals(other.dirfulAddModule))
			return false;
		if (dirfulDropModule == null) {
			if (other.dirfulDropModule != null)
				return false;
		} else if (!dirfulDropModule.equals(other.dirfulDropModule))
			return false;
		if (dirlessAddModule == null) {
			if (other.dirlessAddModule != null)
				return false;
		} else if (!dirlessAddModule.equals(other.dirlessAddModule))
			return false;
		if (dirlessDropModule == null) {
			if (other.dirlessDropModule != null)
				return false;
		} else if (!dirlessDropModule.equals(other.dirlessDropModule))
			return false;
		if (fiber == null) {
			if (other.fiber != null)
				return false;
		} else if (!fiber.equals(other.fiber))
			return false;
		return true;
	}
	@Override
	public int compare(OsmOpticalSignalPropagationElement o1, OsmOpticalSignalPropagationElement o2) 
	{
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		int c;
		c = Boolean.compare(o1.isDirfulAdd(), o2.isDirfulAdd());
		if (c != 0) return c;
		if (o1.isDirfulAdd()) o1.getDirfulAddOutFiber().compareTo(o2.getDirfulAddOutFiber());
		c = Boolean.compare(o1.isDirfulDrop(), o2.isDirfulDrop());
		if (c != 0) return c;
		if (o1.isDirfulDrop()) o1.getDirfulDropInFiber().compareTo(o2.getDirfulDropInFiber());
		c = Boolean.compare(o1.isDirlessAdd(), o2.isDirlessAdd());
		if (c != 0) return c;
		if (o1.isDirlessAdd())
			return o1.getDirlessAddModule().compareTo(o2.getDirlessAddModule());
		c = Boolean.compare(o1.isDirlessDrop(), o2.isDirlessDrop());
		if (c != 0) return c;
		if (o1.isDirlessDrop())
			return o1.getDirlessDropModule().compareTo(o2.getDirlessDropModule());
		assert o1.isFiber() && o2.isFiber();
		return o1.fiber.compareTo(o2.fiber);
	}
	

}

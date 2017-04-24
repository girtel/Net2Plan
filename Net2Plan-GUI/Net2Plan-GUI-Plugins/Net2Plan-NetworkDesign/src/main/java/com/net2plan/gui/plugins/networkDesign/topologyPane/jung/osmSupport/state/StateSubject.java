package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
public abstract class StateSubject
{
    protected List<StateObserver> observers = new ArrayList<>();

    public abstract void setState(JUNGState state, Object... params);

    public abstract JUNGState getState();

    public void attach(StateObserver observer)
    {
        observers.add(observer);
    }

    public void notifyAllObservers()
    {
        for (StateObserver observer : observers)
            observer.update();
    }
}

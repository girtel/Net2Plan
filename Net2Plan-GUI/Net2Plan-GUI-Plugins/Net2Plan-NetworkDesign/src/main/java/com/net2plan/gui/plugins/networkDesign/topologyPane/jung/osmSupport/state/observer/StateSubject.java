package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state.observer;

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state.CanvasState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
public abstract class StateSubject
{
    protected List<StateObserver> observers = new ArrayList<>();

    public abstract void setState(CanvasState state, Object... params);

    public abstract CanvasState getState();

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

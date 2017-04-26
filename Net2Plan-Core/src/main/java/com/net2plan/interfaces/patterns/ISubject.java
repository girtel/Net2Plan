package com.net2plan.interfaces.patterns;

/**
 * @author Jorge San Emeterio
 * @date 26/04/17
 */
public interface ISubject
{
    void setState(IState state, Object... stateParams);
    IState getState();

    void attach(IObserver observer);
    void notifyAllObservers();
}

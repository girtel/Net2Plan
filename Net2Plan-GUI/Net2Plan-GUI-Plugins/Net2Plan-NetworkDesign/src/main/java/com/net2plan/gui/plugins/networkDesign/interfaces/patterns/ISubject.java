package com.net2plan.gui.plugins.networkDesign.interfaces.patterns;

/**
 * @author Jorge San Emeterio
 * @date 26/04/17
 */
public interface ISubject
{
    void addListener(IObserver observer);
    void notifyAllListeners();
}

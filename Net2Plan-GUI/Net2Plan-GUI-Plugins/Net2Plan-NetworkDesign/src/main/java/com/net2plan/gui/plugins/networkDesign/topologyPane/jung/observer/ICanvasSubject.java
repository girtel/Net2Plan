package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.observer;

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasState;
import com.net2plan.interfaces.patterns.ISubject;

/**
 * @author Jorge San Emeterio
 * @date 26/04/17
 */
public interface ICanvasSubject extends ISubject
{
    void setState(CanvasState state, Object... stateParams);
    CanvasState getState();
}

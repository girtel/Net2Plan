package com.net2plan.gui.plugins.networkDesign.topologyPane.jung;

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasStateOptions;
import com.net2plan.interfaces.patterns.ISubject;

/**
 * @author Jorge San Emeterio
 * @date 26/04/17
 */
public interface ICanvasSubject extends ISubject
{
    void setState(CanvasStateOptions state, Object... stateParams);
    CanvasStateOptions getState();
}

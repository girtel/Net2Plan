package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class AjtRcMenu
{
    private final boolean isSeparator;
    private final String menuMessage;
    private final Consumer<ActionEvent> actionListener;
    private final BiFunction<Integer,Integer,Boolean> isMenuEnabledFunction;
    private final List<AjtRcMenu> submenus;
    private final boolean isSlowAndNeedsInvokeLater;
    
    public static AjtRcMenu createMenuSeparator () { return new AjtRcMenu (); } 
    private AjtRcMenu()
    {
        super();
        this.menuMessage = null;
        this.actionListener = null;
        this.submenus = null;
        this.isSeparator = true;
        this.isMenuEnabledFunction = null;
        this.isSlowAndNeedsInvokeLater = false;
    }

    public AjtRcMenu(String menuMessage, Consumer<ActionEvent> actionListener,  
            BiFunction<Integer,Integer,Boolean> isMenuEnabledFunction, List<AjtRcMenu> submenus)
    {
        super();
        this.menuMessage = menuMessage;
        this.actionListener = actionListener;
        this.submenus = submenus == null? null : (submenus.isEmpty()? null : submenus);
        this.isSeparator = false;
        this.isMenuEnabledFunction = isMenuEnabledFunction;
        this.isSlowAndNeedsInvokeLater = false;

    }

    
    public boolean isSlowAndNeedsInvokeLater()
    {
        assert !isSeparator;
        return isSlowAndNeedsInvokeLater;
    }


    
    /** First argument is number of visible in the table, second is the number of selected
     * @return
     */
    public BiFunction<Integer, Integer, Boolean> getIsMenuEnabledFunction()
    {
        assert !isSeparator;
        return isMenuEnabledFunction;
    }

    public final boolean isSubmenu () 
    { 
        assert !isSeparator;
        return submenus != null; 
    }
    
    public String getMenuMessage()
    {
        assert !isSeparator;
        return menuMessage;
    }
    public Consumer<ActionEvent> getActionListener()
    {
        assert !isSeparator;
        return actionListener;
    }
    public boolean hasSubmenus () 
    { 
        assert !isSeparator;
        return submenus != null; 
    }
    
    public List<AjtRcMenu> getSubmenus()
    {
        assert !isSeparator;
        return submenus;
    }
    public boolean isSeparator()
    {
        return isSeparator;
    }

}

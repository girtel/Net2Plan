
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnDialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnInputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFSelectionBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFTagBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.PickManager;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.PickManager.PickStateInfo;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.gui.utils.NetworkElementOrFr;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Pair;


/**
 */
@SuppressWarnings("unchecked")
public abstract class AdvancedJTable_networkElement <T> extends AdvancedJTable_abstractElement<T>
{
    protected final AJTableType ajtType;
    protected final NetworkLayer layerThisTable;
    private boolean attributesAreCollapsedInOneColumn = true;
    
	public AdvancedJTable_networkElement(GUINetworkDesign networkViewer, AJTableType ajtType , NetworkLayer layerThisTable , boolean hasAggregationRow , Function<T,Color> coloringFunctionForTheFullRowIfNotSelected)
    {
        super(networkViewer, ajtType.getTabName() , 2 , hasAggregationRow , coloringFunctionForTheFullRowIfNotSelected);
        this.ajtType = ajtType;
        this.layerThisTable = layerThisTable;
        updateView();
    }

    public boolean isForwardingRulesTable () { return ajtType == AJTableType.FORWARDINGRULES; }
    
    @Override
    protected void addExtendedKeyboardActions()
    {
        final InputMap inputMap = this.getInputMap();
        final ActionMap actionMap = this.getActionMap();
        if (!isForwardingRulesTable())
        {
	        final AbstractAction pickElementAction = new AbstractAction()
	        {
	            @Override
	            public void actionPerformed(ActionEvent actionEvent)
	            {
	                final Set<T> selectedElements = AdvancedJTable_networkElement.this.getSelectedElements();
	                if (selectedElements.isEmpty()) return;
	                SwingUtilities.invokeLater(() -> pickSelection(selectedElements));
	            }
	        };
	        final AbstractAction removeElementsAction = new AbstractAction()
	        {
	            @Override
	            public void actionPerformed(ActionEvent actionEvent)
	            {
	                final Set<T> selectedElements = AdvancedJTable_networkElement.this.getSelectedElements();
	
	                if (selectedElements.isEmpty()) return;
	                SwingUtilities.invokeLater(() -> removeSelection(selectedElements));
	            }
	        };
        
	        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.VK_UNDEFINED), "pickElements");
	        actionMap.put("pickElements", pickElementAction);
	        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.VK_UNDEFINED), "removeElements");
	        actionMap.put("removeElements", removeElementsAction);
        }
    }


    @Override
    protected final List<T> getAllAbstractElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        return rf == null ? (List<T>) ITableRowFilter.getAllElements(callback.getDesign(), this.layerThisTable , ajtType) : (List<T>) rf.getVisibleElements(this.layerThisTable, ajtType);
    }
    
    public final AJTableType getAjType () { return ajtType; }
    
    private List<AjtRcMenu> getFilterMenuOption ()
    {
    	final NetPlan np = callback.getDesign();
    	final List<AjtRcMenu> res = new ArrayList<> ();
        for (boolean applyJustToThisLayer : new boolean [] {true , false})
        {
            final List<AjtRcMenu> submenenusAddElements = new ArrayList<> ();
            final List<AjtRcMenu> submenenusKeepElements = new ArrayList<> ();
        	if (!applyJustToThisLayer && !np.isMultilayer()) continue;
            for (boolean isAdd : new boolean [] {true , false })
            {
                final FilterCombinationType filterCombinationType = isAdd? FilterCombinationType.INCLUDEIF_OR : FilterCombinationType.INCLUDEIF_AND;
                final List<AjtRcMenu> submenuList = isAdd? submenenusAddElements : submenenusKeepElements;
                final AjtRcMenu areAffectedByThisElementMenu = new AjtRcMenu("Are affected by these elements", 
                        e-> 
                        {
                            TBFToFromCarriedTraffic filter = null;
                            for (NetworkElement element : this.getSelectedNetworkElementsNorFr())
                            {
                                if (filter == null)
                                    filter = TBFToFromCarriedTraffic.createTBTFilter(element, Optional.of(this.layerThisTable) , applyJustToThisLayer);
                                else
                                    filter.recomputeApplyingShowIf_ThisOrThat(TBFToFromCarriedTraffic.createTBTFilter(element, Optional.of(this.layerThisTable) , applyJustToThisLayer));
                            }
                            callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                        }, 
                        (a,b)->b>0, null); 
                final AjtRcMenu haveTagMenu = new AjtRcMenu("Have tag...", 
                        e-> 
                        {
                            JTextField txt_tagContains = new JTextField(30);
                            JTextField txt_tagDoesNotContain = new JTextField(30);
                            JPanel pane = new JPanel(new GridLayout(-1, 1));
                            pane.add(new JLabel("Has tag (could be empty): "));
                            pane.add(txt_tagContains);
                            pane.add(new JLabel("AND does not have tag (could be empty): "));
                            pane.add(txt_tagDoesNotContain);
                            while (true)
                            {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Filter elements by tag", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;
                                if (txt_tagContains.getText().isEmpty() && txt_tagDoesNotContain.getText().isEmpty())
                                {
                                    ErrorHandling.showErrorDialog("At least one input tag is required", "Invalid input");
                                    continue;
                                }

                                final ITableRowFilter filter = new TBFTagBased(np, this.layerThisTable , txt_tagContains.getText(), txt_tagDoesNotContain.getText());
                                callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                                callback.updateVisualizationJustTables();
                            }
                        }, 
                        (a,b)->b>0, null); 
                final AjtRcMenu keepSelectedMenu = !applyJustToThisLayer? null : new AjtRcMenu("Keep only selected elements in this table", 
                        e-> 
                        {
                            TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), new ElementSelection (this.getAjType().getNeType() , this.getSelectedElements()));
                            callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                        }, 
                        (a,b)->b>0, null); 
                final AjtRcMenu filterOutSelectedMenu = !applyJustToThisLayer? null : new AjtRcMenu("Filter-out selected elements in this table", 
                        e-> 
                        {
                            final Set<T> invertedSelection = AdvancedJTable_networkElement.this.invertSelection(this.getSelectedElements());
                            if (invertedSelection == null)
                                throw new Net2PlanException("Could not invert selection for the given elements.");
                            TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), new ElementSelection(this.getAjType().getNeType(), invertedSelection));
                            callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                        }, 
                        (a,b)->b>0, null); 
                submenuList.add(areAffectedByThisElementMenu);
                submenuList.add(haveTagMenu);
                if (applyJustToThisLayer) submenuList.add(keepSelectedMenu);
                if (applyJustToThisLayer) submenuList.add(filterOutSelectedMenu);
            }
            final AjtRcMenu thisOption = new AjtRcMenu(applyJustToThisLayer? "Filters: Apply to this layer" : "Filters: Apply to all layers", null, (a,b)->true, Arrays.asList(
                new AjtRcMenu("Add elements that...", null, (a,b)->true, submenenusAddElements) , 
                new AjtRcMenu("Keep elements that...", null, (a,b)->true, submenenusKeepElements) 
            ));
            res.add(thisOption);
        }
        return res;
    }
    
    @Override
    protected void pickSelection(Collection<T> selection)
    {
        assert selection != null;
        final PickManager pm = callback.getPickManager();
        final List<Pair<NetworkElementOrFr, NetworkLayer>> newState = new ArrayList<> ();
        for (T e : selection)
        {
        	final NetworkElementOrFr nefr;
        	if (this.getAjType() ==  AJTableType.FORWARDINGRULES)
        		nefr = new NetworkElementOrFr((Pair) e);
        	else
        		nefr = new NetworkElementOrFr((NetworkElement)e);
        	newState.add(Pair.of(nefr, nefr.getOrEstimateLayer()));
        }
        pm.pickElements(pm.new PickStateInfo(newState));
        callback.updateVisualizationAfterPick();
    }
    
    protected void removeSelection(Set<T> selection) 
	{
		assert selection != null;
		final NetPlan npCopy = this.callback.getDesign().copy();
		final NetPlan np = callback.getDesign();
		try 
		{
			for (T es : selection) 
			{
				if (es instanceof NetworkElement)
				{
					final NetworkElement e = (NetworkElement) es;
					if (e.wasRemoved()) continue;
					switch (e.getNeType())
					{
					case DEMAND: ((Demand) e).remove(); break; 
					case LAYER: np.removeNetworkLayer((NetworkLayer) e); break;
					case LINK: ((Link)e).remove(); break;
					case MULTICAST_DEMAND: ((MulticastDemand)e).remove(); break;
					case MULTICAST_TREE: ((MulticastTree)e).remove(); break;
					case NODE: ((Node)e).remove(); break;
					case RESOURCE: ((Resource)e).remove(); break;
					case ROUTE: ((Route)e).remove(); break;
					case SRG: ((SharedRiskGroup)e).remove(); break;
					default: throw new  RuntimeException();
					}
				} 
			}
			callback.updateVisualizationAfterNewTopology();
		} catch (Exception ex) 
		{
			
			ErrorHandling.showErrorDialog(ex.getMessage(), "Error");
			
			callback.setDesign(npCopy);
            callback.updateVisualizationAfterNewTopology();
		} 
    }

    @Override
    protected void showPopup(MouseEvent me)
    {
        // Debug mode removes the scroll bar so that tests do not have conflicts with it.
        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final Optional<AjtRcMenu> viewMenu = getViewMenuAndSubmenus();
        final List<T> visibleElementsInTable = this.getAllAbstractElementsInTable();
        final List<AjtRcMenu> allPopupMenusButView = new ArrayList<> ();
        if (viewMenu.isPresent()) allPopupMenusButView.add(viewMenu.get());
        if (!isForwardingRulesTable())
        {
	        allPopupMenusButView.add(new AjtRcMenu("Pick selection", e-> SwingUtilities.invokeLater(() -> pickSelection(this.getSelectedElements())), (a,b)->b>0, null));
	        allPopupMenusButView.addAll(getFilterMenuOption ());
	        if (ajtType.isElementThatCanBeHidenInGui())
	        {
	            allPopupMenusButView.add(new AjtRcMenu("Hide selected elements", e->
	            {
	                for (T ne : getSelectedElements())
	                    if (ne instanceof Node)
	                        callback.getVisualizationState().hideOnCanvas((Node)ne);
	                    else if (ne instanceof Link)
	                        callback.getVisualizationState().hideOnCanvas((Link)ne);
	            }, (a,b)->b>0, null));
	        allPopupMenusButView.add(new AjtRcMenu("Show selected elements", e->
	            {
	                for (T ne : getSelectedElements())
	                    if (ne instanceof Node)
	                        callback.getVisualizationState().showOnCanvas((Node)ne);
	                    else if (ne instanceof Link)
	                        callback.getVisualizationState().showOnCanvas((Link)ne);
	            }, (a,b)->b>0, null));
	        }
	        allPopupMenusButView.add(AjtRcMenu.createMenuSeparator());
        }
        allPopupMenusButView.addAll(getNonBasicRightClickMenusInfo());
        
        if (!isForwardingRulesTable())
        {
            allPopupMenusButView.add(AjtRcMenu.createMenuSeparator());
            allPopupMenusButView.add(getMenuAttributes());
            allPopupMenusButView.add(getMenuTags());
        }
        
        /*Adding menus*/
        for (AjtRcMenu popupInfo : allPopupMenusButView)
        {
        	if (popupInfo.isSeparator())  { popup.addSeparator(); continue; }
        	final JComponent menu = processMenu (popupInfo , visibleElementsInTable);            
        	popup.add(menu);
        }
        popup.show(me.getComponent(), me.getX(), me.getY());
    }

    
    public boolean isAttributesAreCollapsedInOneColumn()
    {
		return attributesAreCollapsedInOneColumn;
	}

	public void setAttributesAreCollapsedInOneColumn(boolean attributesAreCollapsedInOneColumn) 
	{
		this.attributesAreCollapsedInOneColumn = attributesAreCollapsedInOneColumn;
	}
	
    protected abstract List<AjtRcMenu> getNonBasicRightClickMenusInfo ();
    
    protected abstract List<AjtColumnInfo<T>> getNonBasicUserDefinedColumnsVisibleOrNot ();
    
    private final Set<T> invertSelection(Set<T> selectedElements)
    {
        // Check all elements belong to the same NetPlan
        final NetPlan netPlan = this.callback.getDesign();
        for (T networkElement : selectedElements)
        {
            if (AJTableType.getTypeOfElement(networkElement) != ajtType) return null;
        }
        final List<?> allElements = ITableRowFilter.getAllElements(netPlan , this.layerThisTable , ajtType);
        Set<T> invertedElements = new HashSet<> ((List<T>) allElements);
        invertedElements.removeAll(selectedElements);
        return invertedElements;
    }

    protected Double dialogGetDouble (String message , String title )
    {
      final String str = JOptionPane.showInputDialog(null, message , title , JOptionPane.QUESTION_MESSAGE);
      if (str == null) return null;
      return Double.parseDouble(str);
    }
    protected Integer dialogGetInteger (String message , String title )
    {
      final String str = JOptionPane.showInputDialog(null, message , title , JOptionPane.QUESTION_MESSAGE);
      if (str == null) return null;
      return Integer.parseInt(str);
    }
    protected String dialogGetString (String message , String title )
    {
      final String val = JOptionPane.showInputDialog(null, message , title , JOptionPane.QUESTION_MESSAGE);
      return val;
    }

    @Override
    protected final List<AjtColumnInfo<T>> getAllColumnsVisibleOrNot ()
    {
        final List<AjtColumnInfo<T>> completeListIncludingCommonColumns = new ArrayList<> ();
        if (!this.isForwardingRulesTable())
        {
            completeListIncludingCommonColumns.add(new AjtColumnInfo<T>(this, Long.class, null, "Id", "Unique identifier (never repeated in any other network element)", null , e->((NetworkElement)e).getId() , AGTYPE.NOAGGREGATION , null));
            completeListIncludingCommonColumns.add(new AjtColumnInfo<T>(this, Integer.class, null , "Index", "Index (consecutive integer starting in zero)", null , e->((NetworkElement)e).getIndex () , AGTYPE.NOAGGREGATION , null));
        }
        completeListIncludingCommonColumns.addAll(getNonBasicUserDefinedColumnsVisibleOrNot());

        if (!this.isForwardingRulesTable())
        {
        	completeListIncludingCommonColumns.add(new AjtColumnInfo<T>(this, String.class, null , "Tags", "User-defined tags associated to this element", null , e->((NetworkElement)e).getTags() , AGTYPE.NOAGGREGATION , null));
        	
        	if (isAttributesAreCollapsedInOneColumn())       	
        		completeListIncludingCommonColumns.add(new AjtColumnInfo<T>(this, String.class, null , "Attributes", "User-defined attributes associated to this element", null , e->((NetworkElement)e).getAttributes() , AGTYPE.NOAGGREGATION , null));
        	else
        	{
        		final Set<String> allAttributeKeys = new TreeSet<>();
        		this.getAllAbstractElementsInTable().forEach(e->allAttributeKeys.addAll(((NetworkElement)e).getAttributes().keySet()));
        		for (String attributeKey : allAttributeKeys)
        		{
            		completeListIncludingCommonColumns.add(new AjtColumnInfo<T>(this, String.class, null , "Att: " + attributeKey, "User-defined attribute '" + attributeKey + "' associated to this element", (e, o) -> 
            		{
            			final String value = (String) o;
            			if (!value.isEmpty()) ((NetworkElement)e).setAttribute(attributeKey, value);
            		}, e->((NetworkElement)e).getAttribute(attributeKey, "") , AGTYPE.NOAGGREGATION , null));
        		}
        	}
        }
        return completeListIncludingCommonColumns;
    }

    public NetworkLayer getTableNetworkLayer () { return layerThisTable; }

    private SortedSet<NetworkElement> getSelectedNetworkElementsNorFr ()
    {
    	return getSelectedElements().stream().filter(e->e instanceof NetworkElement).map(e->(NetworkElement)e).collect(Collectors.toCollection(TreeSet::new));
    }

    protected void reactToMouseSingleClickInTable (int rowModelIndexOfClickOrMinus1IfOut , int columnModelIndexOfClickOrMinus1IfOut)
    {
    	final PickManager pm = callback.getPickManager();
        final SortedSet<T> selectedElements = this.getSelectedElements();
        if (selectedElements.isEmpty()) callback.resetPickedStateAndUpdateView();
        if (rowModelIndexOfClickOrMinus1IfOut == -1) return;
        final Object value = getModel().getValueAt(rowModelIndexOfClickOrMinus1IfOut, columnModelIndexOfClickOrMinus1IfOut);
        if (value instanceof NetworkElement)
        {
        	pm.pickElements(pm.new PickStateInfo((NetworkElement) value , Optional.empty()));
            callback.updateVisualizationAfterPick();
        }
        else if (value instanceof Collection)
        {
            if (((Collection) value).isEmpty()) return;
            
			final Object firstElement = ((Collection) value).iterator().next();

			if (firstElement instanceof NetworkElement)
			{
				final List<NetworkElement> es = new ArrayList<>();
				es.addAll((Collection) value);
				final PickStateInfo pickState = pm.createPickStateFromListNe((Collection) value);
                pm.pickElements(pickState);
				callback.updateVisualizationAfterPick();
			}
        }
        else if (selectedElements.isEmpty()) callback.resetPickedStateAndUpdateView();
    }

    private AjtRcMenu getMenuAttributes ()
    {
        final Set<String> allAttributes = new TreeSet<>();
        getSelectedNetworkElementsNorFr().forEach(e->allAttributes.addAll(e.getAttributes().keySet()));
        final List<String> allAttributesList = allAttributes.stream().collect(Collectors.toList());
        
        final AjtRcMenu expandCollapseAttributesOption;
        if (attributesAreCollapsedInOneColumn)
        	expandCollapseAttributesOption = new AjtRcMenu("Expand all attributes (one column per attribute)", e->
            {
            	setAttributesAreCollapsedInOneColumn(false);
            }, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getAttributes().size()>0), null);
        else
        	expandCollapseAttributesOption = new AjtRcMenu("Collapse all attributes in one column", e->
            {
                this.setAttributesAreCollapsedInOneColumn(true);
            }, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getAttributes().size()>0), null);

        return new AjtRcMenu("Attributes...", null , (a,b)->true, Arrays.asList(
    		new AjtRcMenu("Add attributes to selected elements", e->
    		{
    			MtnDialogBuilder.launch("Add attributes","","",this, 
                    Arrays.asList(
                    		MtnInputForDialog.inputTfString("Attribute name", "The attribute name to be added to selected elements", 10, ""),
                    		MtnInputForDialog.inputTfString("Attribute value", "The attribute value to be added to selected elements", 10, "")),
                    (list)->
                    {
                    	final String key = (String) list.get(0).get();
                    	final String value = (String) list.get(1).get();
                    	if(key.isEmpty() || value.isEmpty()) return;
                    	for (NetworkElement ne : getSelectedNetworkElementsNorFr())
                    		ne.setAttribute(key, value);
                    });
    		}, (a,b)->b>0, null) , 
        
    		new AjtRcMenu("Edit attribute of selected element", e->
    		{
    			MtnDialogBuilder.launch("Edit attribute","","",this, 
                    Arrays.asList(
                            MtnInputForDialog.inputTfCombo("Attribute name to edit", "The attribute to be edited from selected element", 10, getSelectedNetworkElementsNorFr().iterator().next().getAttributes().keySet().stream().collect(Collectors.toList()).get(0), 
                            		getSelectedNetworkElementsNorFr().iterator().next().getAttributes().keySet().stream().collect(Collectors.toList()) , getSelectedNetworkElementsNorFr().iterator().next().getAttributes().keySet().stream().collect(Collectors.toList()) , null),
                    		MtnInputForDialog.inputTfString("New attribute value", "The attribute value to be changed in selected element", 10, "")),
                    (list)->
                    {
                    	final String key = (String) list.get(0).get();
                    	final String value = (String) list.get(1).get();
                    	if(key.isEmpty() || value.isEmpty()) return;
                    	for (NetworkElement ne : getSelectedNetworkElementsNorFr())
                    		ne.setAttribute(key, value);
                    });
    		}, (a,b)->b==1 && !getSelectedNetworkElementsNorFr().first().getAttributes().isEmpty(), null) ,
    		
    		new AjtRcMenu("Remove attribute of selected elements", e -> 
    		{
    			MtnDialogBuilder.launch("Remove attribute","","",this, 
                    Arrays.asList(
                            MtnInputForDialog.inputTfCombo("Attribute to remove", "The attribute to be removed from selected elements", 10, allAttributesList.get(0) , allAttributesList , allAttributesList , null)),
                    (list)->
                    {
                        for (NetworkElement ne : getSelectedNetworkElementsNorFr())
                        	ne.removeAttribute((String) list.get(0).get());
                    });
    		}, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getAttributes().size()>0), null) ,
        
    		new AjtRcMenu("Remove all attributes of selected elements", e->
    		{
    			for (NetworkElement ne : getSelectedNetworkElementsNorFr())
    				ne.getAttributes().keySet().forEach(a->ne.removeAttribute(a));
    		}, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getAttributes().size()>0), null) , 
    		expandCollapseAttributesOption));
    }

    private AjtRcMenu getMenuTags ()
    {
        final Set<String> allTags = new TreeSet<>();
        getSelectedNetworkElementsNorFr().forEach(e->allTags.addAll(e.getTags()));
        final List<String> allTagsList = allTags.stream().collect(Collectors.toList());
		return new AjtRcMenu("Tags...", null , (a,b)->true, Arrays.asList(
				new AjtRcMenu("Add tags to selected elements", e->
	            {
	            	MtnDialogBuilder.launch("Add tags","","",this, 
	                        Arrays.asList(MtnInputForDialog.inputTfString("Tag", "The tag to be added to selected elements", 10, "")),
	                        (list)->
	                        {
	                        	final String tag = (String) list.get(0).get();
	                        	if(tag.isEmpty()) return;
	                        	for (NetworkElement ne : getSelectedNetworkElementsNorFr())
	                        		ne.addTag(tag);
	                        });
	            }, (a,b)->b>0, null),
	            new AjtRcMenu("Remove tags of selected elements", e -> 
	            {
	                MtnDialogBuilder.launch("Remove tags","","",this, 
	                        Arrays.asList(
	                                MtnInputForDialog.inputTfCombo("Tag to remove", "The tag to be removed from selected elements", 10, allTagsList.get(0) , allTagsList , allTagsList , null)),
	                        (list)->
	                        {
	                            for (NetworkElement ne : getSelectedNetworkElementsNorFr())
	                            	ne.removeTag((String) list.get(0).get());
	                        });
	            }, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getTags().size()>0), null) ,

	            new AjtRcMenu("Remove all tags of selected elements", e->
	            {
	                for (NetworkElement ne : getSelectedNetworkElementsNorFr())
	                	ne.getTags().forEach(t->ne.removeTag(t));
	            }, (a,b)->b>0 && getSelectedNetworkElementsNorFr().stream().anyMatch(e->e.getTags().size()>0), null)
			));
    }
}

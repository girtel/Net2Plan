package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.net2plan.gui.plugins.GUINetworkDesignConstants;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelReader;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelWriter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IMonitorizableElement;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.libraries.TrafficMatrixForecastUtils;
import com.net2plan.libraries.TrafficPredictor;
import com.net2plan.libraries.TrafficPredictor.TRAFFICPREDICTORTYPE;
import com.net2plan.libraries.TrafficPredictor_manual_exponential;
import com.net2plan.libraries.TrafficPredictor_manual_linear;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.utils.SwingUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

public class MonitoringUtils
{
    private MonitoringUtils(){}

    private static final DateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static
    {
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static <T extends NetworkElement> AjtRcMenu getMenuAddSyntheticMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");

        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Add synthetic monitoring trace to selected elements", e->
        {
            DialogBuilder.launch(
                    "Add monitoring trace to selected " + elementName ,
                    "This option permits creating in selected elements, a trace of monitored values",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfString("Initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the initial date in the indicated format", 10, dateFormatGmt.format(new Date())),
                            InputForDialog.inputTfDouble("Interval between samples (seconds)", "Introduce intreval (in seconds) between consecutive smaples", 10, 3600.0),
                            InputForDialog.inputTfInt("Number of samples", "Introduce the number of monitoring samples to create", 10, 7*24),
                            InputForDialog.inputCheckBox(isLinkTable? "Use current carried traffic as initial traffic?" : "Use current offered traffic as initial traffic?", "If selected, the current traffic value is used as initial traffic", true , null),
                            InputForDialog.inputTfDouble("Starting traffic", "Introduce the initial traffic (in demand traffic units: '" + table.getTableNetworkLayer().getDemandTrafficUnits() + "')", 10, 1.0),
                            InputForDialog.inputTfCombo("Long-term growth type", "The growth traffic type. Exponential means that traffic at year T+1 is GF times the traffic at year T, where GF is the growth factor. If linear, traffic at year T+1 is the traffic at T plus GF", 20 , TrafficSeries.FITTINGTYPE.LINEAR , Arrays.asList(TrafficSeries.FITTINGTYPE.values()) , Arrays.asList(TrafficSeries.FITTINGTYPE.values()).stream().map(ee->ee.getName ()).collect (Collectors.toList()) , null),
                            InputForDialog.inputTfDouble("Long-term growth factor (per year)", "If exponential growth, this is the compound annual growth rate (CAGR) (adimensional value), if linear growth, this is the traffic growth per year in traffic units" + table.getTableNetworkLayer().getDemandTrafficUnits() + "')", 10, 1.0),
                            InputForDialog.inputTfDouble("Daily variation: busy-hour-to-valley factor (>= 1)", "In daily variations, the traffic outside the busy hours is shaped down. This is the ratio between the average traffic in the busy hour vs. the rest of the day. If < 1, no daily variation is applied", 10, 10.0),
                            InputForDialog.inputTfDouble("Daily variation: hour of the start of busy-hour period (0..24)", "In daily variations, the hour (as a double in [0 , 24) when peak traffic interval (busy hours) start. If not in [0,24), no daily variation is applied", 10, 12.0),
                            InputForDialog.inputTfDouble("Daily variation: duration in hours of the busy-hours period (0..24)", "In daily variations, the duration in hours (as a double in [0 , 24) of peak traffic interval (busy hours) start. If start time plus duration exceeds the hour 24, an error is raised", 10, 3.0),
                            InputForDialog.inputTfDouble("Noise coefficient of variation", "A normal traffic noise centered in 0 and typical deviation given by this value multiplied by the current traffic, is added to the estimation. Note that negative traffics are later truncated to zero", 10, 1.0),
                            InputForDialog.inputCheckBox("Remove previous monitoring values?", "If selected, the current monitored values are removed", true , null),
                            InputForDialog.inputTfInt("Seed of random number generator", "Introduce the seed to used for the generator of random numbers, in noise cretion", 10, 1)
                    ),
                    (list)->
                    {
                        final Date initialDate;
                        try { initialDate = localDateTimeToDate(dateToLocalDateTime(dateFormatGmt.parse((String) list.get(0).get()))); } catch (Exception exc) { throw new Net2PlanException("Wrong date format"); }
                        final long intervalBetweenSamplesInSeconds = ((Double) list.get(1).get()).longValue();
                        final int numberOfSamples = (Integer) list.get(2).get();
                        final boolean useCarriedTrafficAsInitial = (Boolean) list.get(3).get();
                        final double initialTrafficIfNotCurrent = (Double) list.get(4).get();
                        final TrafficSeries.FITTINGTYPE growthType = (TrafficSeries.FITTINGTYPE) list.get(5).get();
                        final double growthFactorPerYear = (Double) list.get(6).get();
                        final double dayVariation_peakFactor = (Double) list.get(7).get();
                        final double dayVariation_startHour = (Double) list.get(8).get();
                        final double dayVariation_durationHours = (Double) list.get(9).get();
                        final double noiseRelativeTypicalDeviationRespectToAverage = (Double) list.get(10).get();
                        final boolean removePreviousMonitValues = (Boolean) list.get(11).get();
                        final long rngSeed = ((Integer) list.get(12).get()).longValue();

                        final boolean dayVariationApplied = dayVariation_peakFactor >= 1 && dayVariation_startHour >= 0 && dayVariation_startHour < 24;
                        if (dayVariationApplied) if (dayVariation_startHour + dayVariation_durationHours >= 24) throw new Net2PlanException ("The busy hour start plus duration, exceeds the day limit");
                        
                        /* Remove previous */
                        if (removePreviousMonitValues) table.getSelectedElements().forEach(ee->
                                {
                                    if (isLinkTable) ((Link) ee).getMonitoredOrForecastedCarriedTraffic().removeAllValues();
                                    else if (isDemandTable) ((Demand) ee).getMonitoredOrForecastedOfferedTraffic().removeAllValues();
                                    else if (isMDemandTable) ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic().removeAllValues();
                                }
                        );

                        /* Add values */
                        final Random rng = new Random (rngSeed);
                        for (T ee: table.getSelectedElements())
                        {
                            Double initialTraffic = null;
                            if (!useCarriedTrafficAsInitial)
                                initialTraffic = initialTrafficIfNotCurrent;
                            else
                            {
                                if (isLinkTable) initialTraffic = ((Link) ee).getCarriedTraffic();
                                else if (isDemandTable) initialTraffic = ((Demand) ee).getOfferedTraffic();
                                else if (isMDemandTable) initialTraffic = ((MulticastDemand) ee).getOfferedTraffic();
                            }
                            assert initialTraffic != null;
                            TrafficSeries tm = null;
                            if (isLinkTable) tm = ((Link) ee).getMonitoredOrForecastedCarriedTraffic();
                            else if (isDemandTable) tm = ((Demand) ee).getMonitoredOrForecastedOfferedTraffic();
                            else if (isMDemandTable) tm = ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
                            assert tm != null;

                            tm.addSyntheticMonitoringTrace(growthType, initialDate, intervalBetweenSamplesInSeconds,
                                    numberOfSamples,
                                    initialTraffic,
                                    growthFactorPerYear, dayVariation_peakFactor , dayVariation_startHour , dayVariation_durationHours , noiseRelativeTypicalDeviationRespectToAverage , 
                                    rng);
                        }

                    }
            );
        } , (a,b)->b>0, null);
    }

    public static AjtRcMenu getMenuAddLinkMonitoringInfoSimulatingTrafficVariations (AdvancedJTable_networkElement<Link> table)
    {
        return new AjtRcMenu("Add link monitoring traces simulating effect of demand traces", e->
        {
        	final NetworkLayer layer = table.getTableNetworkLayer();
        	final NetPlan np = table.getTableNetworkLayer().getNetPlan();
        	if (np.getDemands (layer).stream().anyMatch(d->d.getMonitoredOrForecastedCarriedTraffic().getDatesWithValue().isEmpty())) throw new Net2PlanException ("Some demands do not have monitored information");

        	final SortedSet<Date> datesToApply = new TreeSet<> ();
        	for (Demand d : np.getDemands(layer)) datesToApply.addAll(d.getMonitoredOrForecastedCarriedTraffic().getDatesWithValue()); 
        	for (MulticastDemand d : np.getMulticastDemands(layer)) datesToApply.addAll(d.getMonitoredOrForecastedCarriedTraffic().getDatesWithValue()); 
        	if (datesToApply.isEmpty()) throw new Net2PlanException ("No monitoring information in any demand or multicast demand");
            DialogBuilder.launch(
                    "Add link monitoring traces simulating effect of demand traces" ,
                    "This option permits creates a trace of monitored values in the links. "
                    + "A sample is added to all the links, for any date for which at least one demand as a sample. "
                    + "Demands or multicast demands without monitoring information, are assumed to inject the offered traffic. " +
                    "If a demand has no sample for a date, its value for that date is interpolated from other monitoring samples" ,
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputCheckBox("Remove previous monitoring values?", "If selected, the current monitored values in all the links are removed", true , null)
                    ),
                    (list)->
                    {
                        final boolean removePreviousMonitValues = (Boolean) list.get(0).get();
                        
                        /* Remove previous */
                        if (removePreviousMonitValues) np.getLinks(layer).forEach(ee->ee.getMonitoredOrForecastedCarriedTraffic().removeAllValues());

                        /* Save current offered traffic */
                        final int D = np.getNumberOfDemands(layer);
                        final int MD = np.getNumberOfMulticastDemands(layer);
                        final DoubleMatrix1D current_hd = np.getVectorDemandOfferedTraffic(layer);
                        final DoubleMatrix1D current_hmd = np.getVectorMulticastDemandOfferedTraffic(layer);
                        
                        /* Add values */
                        for (Date date : datesToApply)
                        {
                        	final DoubleMatrix1D newTraf_d = DoubleFactory1D.dense.make(D);
                        	final DoubleMatrix1D newTraf_md = DoubleFactory1D.dense.make(MD);
                        	for (Demand d : np.getDemands(layer)) 
                        	{
                        		final Double newTraf = d.getMonitoredOrForecastedCarriedTraffic().getValueOrInterpolation(date);
                        		newTraf_d.set(d.getIndex(), newTraf == null? current_hd.get(d.getIndex()) : newTraf);
                        	}
                        	for (MulticastDemand d : np.getMulticastDemands(layer)) 
                        	{
                        		final Double newTraf = d.getMonitoredOrForecastedCarriedTraffic().getValueOrInterpolation(date);
                        		newTraf_md.set(d.getIndex(), newTraf == null? current_hmd.get(d.getIndex()) : newTraf);
                        	}
                    		np.setVectorDemandOfferedTraffic(newTraf_d, layer);
                    		np.setVectorMulticastDemandOfferedTraffic(newTraf_md, layer);
                    		for (Link link : np.getLinks(layer))
                    			link.getMonitoredOrForecastedCarriedTraffic().addValue(date, link.getCarriedTraffic());
                        }
                		np.setVectorDemandOfferedTraffic(current_hd, layer);
                		np.setVectorMulticastDemandOfferedTraffic(current_hmd, layer);
                    }
            );
        } , (a,b)->true, null);
    }

    
    public static <T extends NetworkElement>  AjtRcMenu getMenuExportMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Export monitoring/forecast values to Excel", e->
        {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);
            final int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);
                if (file.exists())
                {
                    int option = JOptionPane.showConfirmDialog(null, "File already exists.\nOverwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION)
                        file.delete();
                    else
                        return;
                }
                final int nLinks = table.getSelectedElements().size();
                final List<T> selectedElements = new ArrayList<>(table.getSelectedElements());
                final List<Date> allDates = new ArrayList<> (table.getSelectedElements().stream().map(ee->
                {
                    if (isLinkTable) return ((Link) ee).getMonitoredOrForecastedCarriedTraffic().getValues().keySet();
                    else if (isDemandTable) return ((Demand) ee).getMonitoredOrForecastedOfferedTraffic().getValues().keySet();
                    else if (isMDemandTable) return ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic().getValues().keySet();
                    return null;
                }).filter(ee->ee != null).flatMap(ee->ee.stream()).collect(Collectors.toCollection(TreeSet::new)));
                final int nDates = allDates.size();
                final Object[][] tableData = new Object[nDates + 1][nLinks + 1];
                /* */
                tableData [0][0] = "Link Id";
                for (int contDate = 0; contDate < nDates ; contDate ++)
                    tableData [1+contDate][0] = dateFormatGmt.format(allDates.get(contDate));
                for (int contLink = 0; contLink < nLinks ; contLink ++)
                {
                    final T element = selectedElements.get(contLink);
                    tableData [0][1+contLink] = element.getId();
                    for (int contDate = 0; contDate < nDates ; contDate ++)
                    {
                        final Date date = allDates.get(contDate);
                        Double monitInfo = null;
                        if (isLinkTable) monitInfo = ((Link) element).getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date);
                        else if (isDemandTable) monitInfo = ((Demand) element).getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date);
                        else if (isMDemandTable) monitInfo = ((MulticastDemand) element).getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date);
                        if (monitInfo != null) tableData [1+contDate][1+contLink] = monitInfo.doubleValue();
                    }
                }
                final String layerName = table.getTableNetworkLayer().getName().equals("")? "Layer " + table.getTableNetworkLayer().getIndex() : table.getTableNetworkLayer().getName();
                ExcelWriter.writeToFile(file, layerName + " - Link monit info", tableData);
            }
        } , (a,b)->b>0, null);

    }


    public static <T extends NetworkElement>  AjtRcMenu getMenuImportMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();

        return new AjtRcMenu("Import monitoring/forecast values from Excel", e->
        {
            final NetPlan np = table.getTableNetworkLayer().getNetPlan();
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);
            final int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);
                if (!file.exists()) throw new Net2PlanException ("File does not exist");
                final Map<String, Object[][]> sheetName2Values = ExcelReader.readFile(file);
                for (Object [][] thisSheetInfo : sheetName2Values.values())
                {
                    if (thisSheetInfo.length <= 1) continue;
                    final int nCols = thisSheetInfo [0].length;
                    final int nRows = thisSheetInfo.length;
                    /* Get columns */
                    final SortedMap<Date , Integer> dates2RowIndex = new TreeMap<> ();
                    for (int contRow = 1; contRow < nRows ; contRow ++)
                    {
                        try
                        {
                            final Date rowDate = dateFormatGmt.parse((String) thisSheetInfo [contRow][0]);
                            dates2RowIndex.put(rowDate, contRow);
                        } catch (Exception ee) {System.out.println(thisSheetInfo [contRow][0]);}
                    }
                    /* Fill info for links  */
                    for (int contCol = 1; contCol < nCols ; contCol ++)
                    {
                        final long neId;
                        NetworkElement ne = null;
                        try
                        {
                            final Object cell = thisSheetInfo [0][contCol];
                            if (cell instanceof Number)
                                neId = ((Number) cell).longValue();
                            else if (cell instanceof String)
                                neId = Long.parseLong((String) cell);
                            else throw new RuntimeException ();
                            if (isLinkTable) ne = np.getLinkFromId (neId);
                            else if (isDemandTable) ne = np.getDemandFromId (neId);
                            else if (isMDemandTable) ne = np.getMulticastDemandFromId (neId);
                            if (ne == null) continue;
                        } catch (Exception ee) { continue; }
                        for (Map.Entry<Date,Integer> dateInfo : dates2RowIndex.entrySet())
                        {
                            try
                            {
                                final Date date = dateInfo.getKey ();
                                final int rowExcel = dateInfo.getValue();
                                final Object cell = thisSheetInfo [rowExcel][contCol];
                                final double val;
                                if (cell instanceof Number) val = ((Number) cell).doubleValue();
                                else if (cell instanceof String) val = Double.parseDouble((String) cell);
                                else throw new RuntimeException ();
                                if (isLinkTable) ((Link) ne).getMonitoredOrForecastedCarriedTraffic().addValue(date, val);
                                else if (isDemandTable) ((Demand) ne).getMonitoredOrForecastedOfferedTraffic().addValue(date, val);
                                else if (isMDemandTable) ((MulticastDemand) ne).getMonitoredOrForecastedOfferedTraffic().addValue(date, val);
                            } catch (Exception ee) {}
                        }
                    }
                }
            }
        } , (a,b)->true, null);

    }



    public static <T extends NetworkElement>  AjtRcMenu getMenuSetMonitoredTraffic (AdvancedJTable_networkElement<T> table)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Set monitored traffic for selected elements", e->
        {
            final Calendar now = Calendar.getInstance();
            DialogBuilder.launch(
                    "Select the traffic value, and the target date of the monitoring",
                    "Please introduce the traffic amount and the date in which the traffic will be stored as monitored traffic.",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())),
                            InputForDialog.inputTfDouble("Traffic (" + table.getTableNetworkLayer().getDemandTrafficUnits() + ")", "Introduce the traffic in the indicated units", 10, 0.0),
                            InputForDialog.inputCheckBox("Use element current traffic?", "If selected, the current element traffic value is used, instead of the traffic amount indicated by the user", true , null)
                    ),
                    (list)->
                    {
                        final Date date;
                        try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                        final double trafficConstantUserDefined = (Double) list.get(1).get();
                        final boolean useCurrentElementTraffic = (Boolean) list.get(2).get();
                        if (isDemandTable)
                            table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().addValue(date, useCurrentElementTraffic? ((Demand)d).getOfferedTraffic(): trafficConstantUserDefined));
                        else if (isMDemandTable)
                            table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().addValue(date, useCurrentElementTraffic? ((MulticastDemand)d).getOfferedTraffic(): trafficConstantUserDefined));
                        else if (isLinkTable)
                            table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().addValue(date, useCurrentElementTraffic? ((Link)d).getCarriedTraffic(): trafficConstantUserDefined));
                    }
            );
        }
                , (a,b)->b>0, null);

    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuSetOfferedTrafficAsForecasted (AdvancedJTable_networkElement<T> table)
    {
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        if (!isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("[Forecast->Current] Assign to the offered traffic the existing value for forecasted traffic, for selected elements", e->
        {
        	final Date date = table.getTableNetworkLayer().getNetPlan().getCurrentDate();
        	if (isDemandTable)
        		table.getSelectedElements().stream().map(d->(Demand)d).
        			filter(d->d.getTrafficPredictor ().isPresent ()).
        			forEach (d->d.setOfferedTraffic(d.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval().apply(date)));
        	else 
        		table.getSelectedElements().stream().map(d->(MulticastDemand)d).
    			filter(d->d.getTrafficPredictor ().isPresent ()).
    			forEach (d->d.setOfferedTraffic(d.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval().apply(date)));

        }
        , (a,b)->b>0, null);

    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuSetTrafficPredictorAsConstantEqualToTrafficInElement (AdvancedJTable_networkElement<T> table)
    {
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("[Current->Forecast]  Set forecasted traffic as constant = current traffic in selected elements", e->
        {
        	final Date date = table.getTableNetworkLayer().getNetPlan().getCurrentDate();
        	if (isDemandTable)
        		table.getSelectedElements().stream().map(d->(Demand)d).
        			forEach (d->
        			{
        				final TrafficPredictor tp = TrafficPredictor_manual_linear.createFromData(date.getTime() , d.getOfferedTraffic() , 0.0).orElse(null);
        				d.setTrafficPredictor(tp);
        			}
        			);
        	else if (isMDemandTable)
        		table.getSelectedElements().stream().map(d->(MulticastDemand)d).
    			forEach (d->
    			{
    				final TrafficPredictor tp = TrafficPredictor_manual_linear.createFromData(date.getTime() , d.getOfferedTraffic() , 0.0).orElse(null);
    				d.setTrafficPredictor(tp);
    			}
    			);
        	else 
        		table.getSelectedElements().stream().map(d->(Link)d).
    			forEach (d->
    			{
    				final TrafficPredictor tp = TrafficPredictor_manual_linear.createFromData(date.getTime() , d.getCarriedTraffic() , 0.0).orElse(null);
    				d.setTrafficPredictor(tp);
    			}
    			);
        }
        , (a,b)->b>0, null);

    }

    
    public static <T extends NetworkElement>  AjtRcMenu getMenuPercentileFilterMonitSamples (AdvancedJTable_networkElement<T> table)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");

        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Percentile-filtering of samples", e->
        {
            DialogBuilder.launch(
                    "Filter-out samples according to the a percentile-indication for this " + elementName ,
                    ". If e.g percentile 95 is chosen, and the time interval is set to \"day\", all the samples in each day, if any, are replaced "
                    + "by a single sample with the value so that a 95% of the samples are below this value",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfCombo("Interval type", "One sample is produced for each interval (if at least one sample already exists)", 20 , "day" , Arrays.asList("hour" , "day", "week", "year") , Arrays.asList("hour" , "day", "week", "year") , null),
                            InputForDialog.inputTfDouble("Percentile (0 , 1)", "Introduce the percentile. 1 means peaking the maximum value in the interval", 10, 0.95)
                    ),
                    (list)->
                    {
                        final String intervalTimeType = (String) list.get(0).get();
                        final double percentile = (Double) list.get(1).get();
                        /* Add values */
                        for (T ee: table.getSelectedElements())
                        {
                            TrafficSeries tm = null;
                            if (isLinkTable) tm = ((Link) ee).getMonitoredOrForecastedCarriedTraffic();
                            else if (isDemandTable) tm = ((Demand) ee).getMonitoredOrForecastedOfferedTraffic();
                            else if (isMDemandTable) tm = ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
                            assert tm != null;
                            tm.applyPercentileFiltering(tm.getFirstDate() , tm.getLastDate() , intervalTimeType, percentile);
                        }
                    }
            );
        } , (a,b)->b>0, null);
    }

    
    public static <TT extends NetworkElement>  AjtRcMenu getMenuCreatePredictorTraffic (AdvancedJTable_networkElement<TT> table)
    {
        return new AjtRcMenu("Create traffic predictor for selected elements traffic", null, (a,b)->b>0, Arrays.asList(
            	new AjtRcMenu("from monitored traffic, exponential fit", e->
                {
                    final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final List<IMonitorizableElement> selElements = table.getSelectedElements().stream().map(ee->(IMonitorizableElement)ee).collect(Collectors.toList());
                    if (selElements.isEmpty()) return;
                    final SortedMap<TT, Double> varianceExplained = new TreeMap<> ();
                    int numberOfIgnoredElements = 0;
                    for (TT ee : table.getSelectedElements())
                    {
                        final TrafficSeries tm = ((IMonitorizableElement) ee).getMonitoredOrForecastedCarriedTraffic();
                        assert tm != null;                                      
                        if (tm.getSize() < 3) { numberOfIgnoredElements ++; continue; }
                        final TrafficPredictor tp = TrafficPredictor.createFromMonitData(TRAFFICPREDICTORTYPE.EXPONENTIALFIT, tm.getValues()).orElse(null);
                        if (tp == null) { numberOfIgnoredElements ++; continue; }
                        varianceExplained.put(ee, tp.getStatistics().getRsquared());
                        ((IMonitorizableElement) ee).setTrafficPredictor(tp);
                    }
                    final String RETURN = String.format("%n");
                    final DecimalFormat df = new DecimalFormat("#.##"); 
                    final double minR2 = varianceExplained.values().stream().mapToDouble(ee->ee).min().orElse(0);
                    final double maxR2 = varianceExplained.values().stream().mapToDouble(ee->ee).max().orElse(0);
                    final double avR2 = varianceExplained.isEmpty()? 0.0 : varianceExplained.values().stream().mapToDouble(ee->ee).sum() / (varianceExplained.size());
                    final String message = "Number of elements: " + table.getSelectedElements().size() + (numberOfIgnoredElements > 0? " [" + numberOfIgnoredElements +  " ignored]" : "") + RETURN + 
                    		"% of variance explained in each demand [MIN / AVG / MAX]: [" + df.format(minR2) + " / " + df.format(avR2) + " / " + df.format(maxR2) + "]"; 
                    JOptionPane.showMessageDialog(null, message , "Output info", JOptionPane.INFORMATION_MESSAGE);
                } , (a,b)->b>0, null),
            	
            	new AjtRcMenu("from monitored traffic, linear fit", e->
                {
                    final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final List<IMonitorizableElement> selElements = table.getSelectedElements().stream().map(ee->(IMonitorizableElement)ee).collect(Collectors.toList());
                    if (selElements.isEmpty()) return;
                    final SortedMap<TT, Double> varianceExplained = new TreeMap<> ();
                    int numberOfIgnoredElements = 0;
                    for (TT ee : table.getSelectedElements())
                    {
                        final TrafficSeries tm = ((IMonitorizableElement) ee).getMonitoredOrForecastedCarriedTraffic();
                        assert tm != null;                                      
                        if (tm.getSize() < 3) { numberOfIgnoredElements ++; continue; }
                        final TrafficPredictor tp = TrafficPredictor.createFromMonitData(TRAFFICPREDICTORTYPE.LINEARFIT, tm.getValues()).orElse(null);
                        if (tp == null) { numberOfIgnoredElements ++; continue; }
                        varianceExplained.put(ee, tp.getStatistics().getRsquared());
                        ((IMonitorizableElement) ee).setTrafficPredictor(tp);
                    }
                    final String RETURN = String.format("%n");
                    final DecimalFormat df = new DecimalFormat("#.##"); 
                    final double minR2 = varianceExplained.values().stream().mapToDouble(ee->ee).min().orElse(0);
                    final double maxR2 = varianceExplained.values().stream().mapToDouble(ee->ee).max().orElse(0);
                    final double avR2 = varianceExplained.isEmpty()? 0.0 : varianceExplained.values().stream().mapToDouble(ee->ee).sum() / (varianceExplained.size());
                    final String message = "Number of elements: " + table.getSelectedElements().size() + (numberOfIgnoredElements > 0? " [" + numberOfIgnoredElements +  " ignored]" : "") + RETURN + 
                    		"% of variance explained in each demand [MIN / AVG / MAX]: [" + df.format(minR2) + " / " + df.format(avR2) + " / " + df.format(maxR2) + "]"; 
                    JOptionPane.showMessageDialog(null, message , "Output info", JOptionPane.INFORMATION_MESSAGE);
                } , (a,b)->b>0, null),

            	new AjtRcMenu("as manual exponential growth", e->
                {
                    final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final List<IMonitorizableElement> selElements = table.getSelectedElements().stream().map(ee->(IMonitorizableElement)ee).collect(Collectors.toList());
                    if (selElements.isEmpty()) return;
                    DialogBuilder.launch(
                            "Select traffic predictor manually", 
                            "Please introduce the initial date, and the compound-annual-growth (CAGR). " +
                            "E.g. a CAGR of 25% means that the traffic grows at the initial date is the current element traffic, and then grows a 25% every year.",
                            "", 
                            table, 
                            Arrays.asList(
                                    InputForDialog.inputTfDouble("CAGR (%)", "Introduce the compound-annual-growth-rate", 10, 0.25)
                                    ),
                            (list)->
                                {
                                    final double cagr = ((Double) list.get(0).get()) / 100.0;
                                    for (TT ee : table.getSelectedElements())
                                    {
                                    	final IMonitorizableElement eee = (IMonitorizableElement) ee;
                                        final Date initialDate = eee.getMonitoredOrForecastedCarriedTraffic().getFirstDate();
                                        if (initialDate == null) continue;
                                    	final TrafficPredictor tp = TrafficPredictor_manual_exponential.createFromData(initialDate.getTime (), eee.getCurrentTrafficToAddMonitSample() , cagr).orElse(null);
                                    	if (tp == null) continue;
                                        eee.setTrafficPredictor(tp);
                                    }
                                }
                            );
                } , (a,b)->b>0, null),
            	
            	new AjtRcMenu("as linear growth", e->
                {
                    final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final List<IMonitorizableElement> selElements = table.getSelectedElements().stream().map(ee->(IMonitorizableElement)ee).collect(Collectors.toList());
                    if (selElements.isEmpty()) return;
                    DialogBuilder.launch(
                            "Select traffic predictor manually", 
                            "Please introduce the initial date, and the lienar annual growth in Gbps. E.g. a linear growth of 10 Gbps, means "
                            + "that the traffic grows linearly 10 Gbps every year. ",
                            "", 
                            table, 
                            Arrays.asList(
                                    InputForDialog.inputTfDouble("Growth per year (Gbps)", "Introduce the traffic growth per year", 10, 10.0)
                                    ),
                            (list)->
                                {
                                    final double annualGrowthGbps = ((Double) list.get(0).get());
                                    for (TT ee : table.getSelectedElements())
                                    {
                                    	final IMonitorizableElement eee = (IMonitorizableElement) ee;
                                        final Date initialDate = eee.getMonitoredOrForecastedCarriedTraffic().getFirstDate();
                                        if (initialDate == null) continue;
                                    	final TrafficPredictor tp = TrafficPredictor_manual_linear.createFromData(initialDate.getTime (), eee.getCurrentTrafficToAddMonitSample() , annualGrowthGbps).orElse(null);
                                    	if (tp == null) continue;
                                        eee.setTrafficPredictor(tp);
                                    }
                                }
                            );
                } , (a,b)->b>0, null)        		
        		));
    }

//    public static <T extends NetworkElement>  AjtRcMenu getMenuPredictTrafficFromSameElementMonitorInfo (AdvancedJTable_networkElement<T> table)
//    {
//        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
//        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
//        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
//        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
//        return new AjtRcMenu("Forecast selected elements traffic from monitor info", e->
//        {
//            final Calendar now = Calendar.getInstance();
//
//            DialogBuilder.launch(
//                    "Select the traffic value, and the target date of the monitoring",
//                    "Please introduce the date to predict, and statistical parameters.",
//                    "",
//                    table,
//                    Arrays.asList(
//                            InputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())),
//                            InputForDialog.inputTfCombo("Fitting type", "The fitting type to use to predict the traffic growth evolution. Exponential means that traffic is fitted to an exponential function of time, linear to a linear function of time", 20 , TrafficSeries.FITTINGTYPE.LINEAR , Arrays.asList(TrafficSeries.FITTINGTYPE.values()) , Arrays.asList(TrafficSeries.FITTINGTYPE.values()).stream().map(ee->ee.getName ()).collect (Collectors.toList()) , null),
//                            InputForDialog.inputTfDouble("Probability of underestimation", "The predicted traffic will be such that the probability of the traffic to be higher that the prediction is the given probabilty. A value of 0.5 provides an unbiased (neither conservative nor optimistic) estimation", 10, 0.05),
//                            InputForDialog.inputTfCombo("Save in...", "Indicates where the prediction will be stored", 20 , "As new monitoring sample" , isLinkTable? Arrays.asList("As new monitoring sample") : Arrays.asList("As new monitoring sample" , "As current demand offered traffic") , isLinkTable? Arrays.asList("As new monitoring sample") : Arrays.asList("As new monitoring sample" , "As current demand offered traffic") , null)
//                    ),
//                    (list)->
//                    {
//                        final SortedMap<T, Double> varianceExplained = new TreeMap<> ();
//                        final Date date;
//                        try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
//                        final TrafficSeries.FITTINGTYPE fittingType = (TrafficSeries.FITTINGTYPE) list.get(1).get();
//                        final double probSubestimation = (Double) list.get(2).get();
//                        final boolean storeAsNewSample = ((String) list.get(3).get()).equals("As new monitoring sample");
//                        if (probSubestimation <= 0 || probSubestimation >= 1) throw new Net2PlanException ("Wrong value of probability");
//                        for (T ee : table.getSelectedElements())
//                        {
//                            TrafficSeries tm = null;
//                            if (isLinkTable) tm = ((Link) ee).getMonitoredOrForecastedCarriedTraffic();
//                            else if (isDemandTable) tm = ((Demand) ee).getMonitoredOrForecastedOfferedTraffic();
//                            else if (isMDemandTable) tm = ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
//                            assert tm != null;
//                            if (tm.getSize() < 3) throw new Net2PlanException ("Not enough data to make the analysis for element of index: " + ee.getIndex());
//                        }
//                        for (T ee : table.getSelectedElements())
//                        {
//                            TrafficSeries tm = null;
//                            if (isLinkTable) tm = ((Link) ee).getMonitoredOrForecastedCarriedTraffic();
//                            else if (isDemandTable) tm = ((Demand) ee).getMonitoredOrForecastedOfferedTraffic();
//                            else if (isMDemandTable) tm = ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
//                            assert tm != null;
//                            
//                            final TrafficPredictor tpLinear = TrafficPredictor.createFromMonitData(TRAFFICPREDICTORTYPE.LINEARFIT, values).orElse(null);
//                            final TrafficPredictor tpExp = TrafficPredictor.createFromMonitData(TRAFFICPREDICTORTYPE.EXPONENTIALFIT, values).orElse(null);
//                            if (tpLinear == null && tpExp == null) return Optional.empty();
//                            if (tpLinear == null || tpExp == null) return Optional.of(tpLinear != null? tpLinear : tpExp);
//                            final double r2Linear = tpLinear.getStatistics().getRsquared();
//                            final double r2Exp = tpExp.getStatistics().getRsquared();
//                            return r2Linear > r2Exp? Optional.of(tpLinear) : Optional.of(tpExp);
//
//                            final TrafficSeries.TrafficPredictor tp;
//                            switch (fittingType)
//                            {
//							case EXPONENTIAL:
//								tp = TrafficPredictor.createFromMonitData(TRAFFICPREDICTORTYPE.LINEARFIT, values).orElse(null);
//								break;
//							case LINEAR:
//								break;
//							default:
//								break;
//                            }
//                            		
//                            		tm.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);
//                            final double val = tp.getPredictorFunction(probSubestimation).apply(date);
//                            varianceExplained.put(ee, tp.getRegResuls().getRSquared());
//                            if (storeAsNewSample) tm.addValue(date, val);
//                            else
//                            if (isDemandTable) ((Demand)ee).setOfferedTraffic(val); else ((MulticastDemand)ee).setOfferedTraffic(val);
//                        }
//                        final String RETURN = String.format("%n");
//                        final DecimalFormat df = new DecimalFormat("#.##");
//                        final double minR2 = varianceExplained.values().stream().mapToDouble(ee->ee).min().orElse(0);
//                        final double maxR2 = varianceExplained.values().stream().mapToDouble(ee->ee).max().orElse(0);
//                        final double avR2 = varianceExplained.isEmpty()? 0.0 : varianceExplained.values().stream().mapToDouble(ee->ee).sum() / ((double) varianceExplained.size());
//                        final String message = "Number of elements: " + table.getSelectedElements().size() + RETURN + "% of variance explained in each demand [MIN / AVG / MAX]" + (fittingType.isExponential()? " (applied to log(traffic))" : "") + ": [" + df.format(minR2) + " / " + df.format(avR2) + " / " + df.format(maxR2);
//                        JOptionPane.showMessageDialog(null, message , "Output info", JOptionPane.INFORMATION_MESSAGE);
//                    }
//            );
//        }
//                , (a,b)->b>0, null);
//
//    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuRemoveMonitorInfoBeforeAfterDate (AdvancedJTable_networkElement<T> table , boolean beforeTrueAfterFalse)
    {
        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new AjtRcMenu(beforeTrueAfterFalse? "before date..." : "after date..." , e->
        {
            final Calendar now = Calendar.getInstance();
            DialogBuilder.launch(
                    "Select the limit date for removing (including that date)",
                    "Please introduce the limit date for removing the monitoring info (info at that specfic date will be also removed).",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ()))
                    ),
                    (list)->
                    {
                        final Date date;
                        try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                        if (isDemandTable)
                        {
                            if (beforeTrueAfterFalse)
                                table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesBeforeOrEqual(date));
                            else
                                table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesAfterOrEqual(date));
                        }
                        else if (isMDemandTable)
                        {
                            if (beforeTrueAfterFalse)
                                table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesBeforeOrEqual(date));
                            else
                                table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesAfterOrEqual(date));
                        }
                        else if (isLinkTable)
                        {
                            if (beforeTrueAfterFalse)
                                table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().removeAllValuesBeforeOrEqual(date));
                            else
                                table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().removeAllValuesAfterOrEqual(date));
                        }
                    }
            );
        }
                , (a,b)->b>0, null);

    }


    public static <T extends NetworkElement>  AjtRcMenu getMenuForecastDemandTrafficUsingGravityModel (AdvancedJTable_networkElement<T> table)
    {

        return new AjtRcMenu("Using gravity model", null , (a,b)->true, Arrays.asList(
                new AjtRcMenu("from monitored traffic", e->
                {
                    final NetworkLayer layer = table.getTableNetworkLayer();
                    final NetPlan np = layer.getNetPlan();
                    final List<Link> links = np.getLinks(layer);
                    final List<Demand> demands = np.getDemands(layer);
                    if (links.isEmpty()) throw new Net2PlanException("No links in this layer");
                    if (np.hasMulticastDemands(layer)) throw new Net2PlanException("The link cannot have multicast demands");
                    if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand origin node has no output links");
                    if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand end node has no input links");
                    final SortedSet<Date> datesWithEnoughInformationFromGM = TrafficMatrixForecastUtils.getDatesWhereGravityModelCanBeApplied (layer);
                    if (datesWithEnoughInformationFromGM.isEmpty()) throw new Net2PlanException ("No dates exist with enough monitoring information to apply the gravity model");
                    DialogBuilder.launch(
                            "Forecast demands traffic using gravity model",
                            "Please indicate the requested parameters. The demands offered traffic will be estimated according to link monitored carried "
                                    + "traffic values of the given date, and stored in the same date. Only dates where enough monitoring information in the links exist are acceptable for this method. "
                                    + "The estimations will be made for all the dates with enough information in the provided range",
                            "",
                            table,
                            Arrays.asList(
                                    InputForDialog.inputTfString("Introduce the initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(datesWithEnoughInformationFromGM.first())),
                                    InputForDialog.inputTfString("Introduce the end GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(datesWithEnoughInformationFromGM.last()))
                            ),
                            (list)->
                            {
                                final Date initialDate, endDate;
                                try { initialDate = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                                try { endDate = dateFormatGmt.parse((String) list.get(1).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                                final SortedSet<Date> datesToApplyEstimation = datesWithEnoughInformationFromGM.stream().filter(d->!d.before(initialDate) && !d.after(endDate)).collect(Collectors.toCollection(TreeSet::new));
                                if (datesToApplyEstimation.isEmpty()) throw new Net2PlanException ("No dates are selected, no gravity model estimations are performed");
                                for (Date date : datesToApplyEstimation)
                                {
                                    final SortedMap<Demand,Double> estimTrafficGm = TrafficMatrixForecastUtils.getGravityModelEstimationFromMonitorTraffic(layer , date);
                                    estimTrafficGm.entrySet().forEach(ee->ee.getKey().getMonitoredOrForecastedOfferedTraffic().addValue (date , ee.getValue()));
                                }
                                JOptionPane.showMessageDialog(null, "Gravity model applied to " + datesToApplyEstimation.size() + " dates", "Output info", JOptionPane.INFORMATION_MESSAGE);
                            }
                    );
                }
                        , (a,b)->true, null) ,

                new AjtRcMenu("from current link carried traffic", e->
                {
                    final NetworkLayer layer = table.getTableNetworkLayer();
                    final NetPlan np = layer.getNetPlan();
                    final List<Link> links = np.getLinks(layer);
                    final List<Demand> demands = np.getDemands(layer);
                    if (links.isEmpty()) throw new Net2PlanException("No links in this layer");
                    if (np.hasMulticastDemands(layer)) throw new Net2PlanException("The link cannot have multicast demands");
                    if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand origin node has no output links");
                    if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand end node has no input links");
                    final SortedMap<Demand,Double> estimTrafficGm = TrafficMatrixForecastUtils.getGravityModelEstimationFromCurrentCarriedTraffic(layer);
                    estimTrafficGm.entrySet().forEach(ee->ee.getKey().setOfferedTraffic(ee.getValue()));
                    JOptionPane.showMessageDialog(null, "Gravity model corretly applied", "Output info", JOptionPane.INFORMATION_MESSAGE);
                }
                , (a,b)->true, null),
                
                new AjtRcMenu("from current date link carried traffic forecast", e->
                {
                    final NetworkLayer layer = table.getTableNetworkLayer();
                    final NetPlan np = layer.getNetPlan();
                    final List<Link> links = np.getLinks(layer);
                    final List<Demand> demands = np.getDemands(layer);
                    if (links.isEmpty()) throw new Net2PlanException("No links in this layer");
                    if (np.hasMulticastDemands(layer)) throw new Net2PlanException("The link cannot have multicast demands");
                    if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand origin node has no output links");
                    if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand end node has no input links");
                    final SortedMap<Demand,Double> estimTrafficGm = TrafficMatrixForecastUtils.getGravityModelEstimationFromCurrentDateLinkCarriedForecast(layer);
                    estimTrafficGm.entrySet().forEach(ee->ee.getKey().setOfferedTraffic(ee.getValue()));
                    JOptionPane.showMessageDialog(null, "Gravity model corretly applied", "Output info", JOptionPane.INFORMATION_MESSAGE);
                }
                , (a,b)->true, null)
        ));
    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuForecastDemandTrafficFromLinkInfo (AdvancedJTable_networkElement<T> table)
    {
        final NetworkLayer layer = table.getTableNetworkLayer();
        final NetPlan np = layer.getNetPlan();

        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        final SortedSet<Date> datesWihtAtLeastOneLinkMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneLinkMonitorInfo(layer);
        final SortedSet<Date> datesWihtAtLeastOneDemandMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneUnicastDemandMonitorInfo(layer);
        final SortedSet<Date> datesWihtAtLeastOneMDemandMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneMulticastDemandMonitorInfo(layer);
        final SortedSet<Date> datesWithDemandMDemandOrLinkInfo = new TreeSet<> ();
        datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneLinkMonitInfo);
        datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneDemandMonitInfo);
        datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneMDemandMonitInfo);
        return new AjtRcMenu("Using regression from link monitor info", e->
        {
            if (datesWithDemandMDemandOrLinkInfo.isEmpty()) throw new Net2PlanException ("No monitoring information available");
            final List<String> optionsInputDemandMonit = Arrays.asList(
                    "No input demand information" ,
                    "Use gravity model estimation from link monit." ,
                    "Same date demand monitoring info" ,
                    "Zero traffic");
            DialogBuilder.launch(
                    "Forecast demands traffic from current link info",
                    "Please indicate the requested parameters. The demands and multicast demands offered traffic will be estimated according to "
                    + "current link monitored carried traffic values, and potentially demand and multicast demand monitored values. "
                            + "Note that this may make the carried traffics to change",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfString("Introduce the initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())),
                            InputForDialog.inputTfString("Introduce the end GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())),
                            InputForDialog.inputTfDouble("Balance between link monitoring vs. demands previous information", "The predicted traffic will be such that the probability of the traffic to be higher that the prediction is the given probabilty. A value of 0.5 provides an unbiased (neither conservative nor optimistic) estimation", 10, 0.5),
                            InputForDialog.inputTfCombo("Input of demand monitoring", "Where the demand monitoring information will come from", 20 , null , optionsInputDemandMonit , null , null),
                            InputForDialog.inputCheckBox("Apply only to dates with full link monitoring info", "If selected, the estimation will be applied only to those dates for which we have monitoring information for ALL the links", true , null)
                    ),
                    (list)->
                    {
                        final Date initialDate , endDate;
                        try { initialDate = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                        try { endDate = dateFormatGmt.parse((String) list.get(1).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                        if (endDate.before(initialDate)) throw new Net2PlanException ("End date cannot be before initial date");
                        final double coeff_preferFitRouting0PreferFitDemand1 = (Double) list.get(2).get();
                        if (coeff_preferFitRouting0PreferFitDemand1 < 0 || coeff_preferFitRouting0PreferFitDemand1 > 1) throw new Net2PlanException ("Wrong value of balance coefficient");
                        final int indexSelectionInputDemandMonit = optionsInputDemandMonit.indexOf((String) list.get(3).get());
                        final boolean applyOnlyForDatesWithFullLinkMonitInfo = (Boolean) list.get(4).get();
                        assert indexSelectionInputDemandMonit != -1;
                        final SortedSet<Date> datesToApplyEstimation = datesWithDemandMDemandOrLinkInfo.stream().
                                filter(d->!d.before(initialDate) && !d.after(endDate)).
                                filter(d->applyOnlyForDatesWithFullLinkMonitInfo? np.getLinks(layer).stream().allMatch(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue(d)) : true).
                                collect(Collectors.toCollection(TreeSet::new));
                        if (indexSelectionInputDemandMonit == 1)
                            datesToApplyEstimation.retainAll(TrafficMatrixForecastUtils.getDatesWhereGravityModelCanBeApplied(layer));
                        if (datesToApplyEstimation.isEmpty()) throw new Net2PlanException ("No dates are eligible for traffic matrix estimations are performed");
                        for (Date date : datesToApplyEstimation)
                        {
                            final SortedMap<Link,Double> inputMonitInfo_someLinks = new TreeMap<> (np.getLinks(layer).stream().filter(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date))));
                            final TrafficMatrixForecastUtils.TmEstimationResults esimRes;
                            if (indexSelectionInputDemandMonit == 0)
                            {
                                /* No demand information is used */
                                esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, null, null, coeff_preferFitRouting0PreferFitDemand1);
                            } else if (indexSelectionInputDemandMonit == 1)
                            {
                                /* Use gravity model */
                                final SortedMap<Demand,Double> gravityModelEstim = TrafficMatrixForecastUtils.getGravityModelEstimationFromMonitorTraffic(layer, date);
                                esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, gravityModelEstim, null, coeff_preferFitRouting0PreferFitDemand1);
                            } else if (indexSelectionInputDemandMonit == 2)
                            {
                                /* Same date demand monitoring info */
                                final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().filter(ee->ee.getMonitoredOrForecastedOfferedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date)));
                                final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().filter(ee->ee.getMonitoredOrForecastedOfferedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date)));
                                esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                            } else if (indexSelectionInputDemandMonit == 3)
                            {
                                /* Demand is zero traffic */
                                final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                                final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                                esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                            } else throw new RuntimeException ();

                            /* Store the information */
                            for (Demand d : np.getDemands(layer))
                                d.getMonitoredOrForecastedOfferedTraffic().addValue(date , esimRes.getEstimationDemand(d));
                            for (MulticastDemand d : np.getMulticastDemands(layer))
                                d.getMonitoredOrForecastedOfferedTraffic().addValue(date , esimRes.getEstimationMDemand(d));
                        }
                        JOptionPane.showMessageDialog(null, "Estimation applied to " + datesToApplyEstimation.size() + " dates", "Output info", JOptionPane.INFORMATION_MESSAGE);
                    }
            );
        }
                , (a,b)->b>0, null);
    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuForecastDemandTrafficFromLinkForecast (AdvancedJTable_networkElement<T> table)
    {
        final NetworkLayer layer = table.getTableNetworkLayer();
        final NetPlan np = layer.getNetPlan();

        final boolean isLinkTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.LINKS;
        final boolean isDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.DEMANDS;
        final boolean isMDemandTable =  table.getAjType() == GUINetworkDesignConstants.AJTableType.MULTICAST_DEMANDS;
        if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Using regression from link forecast info", e->
        {
            final List<String> optionsInputDemandMonit = Arrays.asList(
                    "No input demand information" ,
                    "Use gravity model estimation from link forecast" ,
                    "Current date demand forecast info" ,
                    "Zero traffic");
            DialogBuilder.launch(
                    "Forecast demands traffic from current link info",
                    "Please indicate the requested parameters. The demands and multicast demands offered traffic will be estimated according to "
                    + "current link monitored carried traffic values, and potentially demand and multicast demand monitored values. "
                            + "Note that this may make the carried traffics to change",
                    "",
                    table,
                    Arrays.asList(
                            InputForDialog.inputTfDouble("Balance between link monitoring vs. demands previous information", "The predicted traffic will be such that the probability of the traffic to be higher that the prediction is the given probabilty. A value of 0.5 provides an unbiased (neither conservative nor optimistic) estimation", 10, 0.5),
                            InputForDialog.inputTfCombo("Input of demand monitoring", "Where the demand monitoring information will come from", 20 , null , optionsInputDemandMonit , null , null)
                    ),
                    (list)->
                    {
                        final double coeff_preferFitRouting0PreferFitDemand1 = (Double) list.get(0).get();
                        if (coeff_preferFitRouting0PreferFitDemand1 < 0 || coeff_preferFitRouting0PreferFitDemand1 > 1) throw new Net2PlanException ("Wrong value of balance coefficient");
                        final int indexSelectionInputDemandMonit = optionsInputDemandMonit.indexOf((String) list.get(1).get());
                        assert indexSelectionInputDemandMonit != -1;
                        final SortedMap<Link,Double> inputMonitInfo_someLinks = new TreeMap<> (np.getLinks(layer).stream().filter(ee->ee.getTrafficPredictor().isPresent()).collect(Collectors.toMap(ee->ee, ee->ee.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval().apply (np.getCurrentDate ()))));
                        final TrafficMatrixForecastUtils.TmEstimationResults esimRes;
                        if (indexSelectionInputDemandMonit == 0)
                        {
                            /* No demand information is used */
                            esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, null, null, coeff_preferFitRouting0PreferFitDemand1);
                        } else if (indexSelectionInputDemandMonit == 1)
                        {
                            /* Use gravity model */
                            final SortedMap<Demand,Double> gravityModelEstim = TrafficMatrixForecastUtils.getGravityModelEstimationFromCurrentDateLinkCarriedForecast(layer);
                            esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, gravityModelEstim, null, coeff_preferFitRouting0PreferFitDemand1);
                        } else if (indexSelectionInputDemandMonit == 2)
                        {
                            /* Same date demand monitoring info */
                            final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().filter(ee->ee.getTrafficPredictor().isPresent()).collect(Collectors.toMap(ee->ee, ee->ee.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval().apply (np.getCurrentDate ())));
                            final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().filter(ee->ee.getTrafficPredictor().isPresent()).collect(Collectors.toMap(ee->ee, ee->ee.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval().apply (np.getCurrentDate ())));
                            esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                        } else if (indexSelectionInputDemandMonit == 3)
                        {
                            /* Demand is zero traffic */
                            final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                            final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                            esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                        } else throw new RuntimeException ();

                        /* Store the information */
                        for (Demand d : np.getDemands(layer))
                            d.setOfferedTraffic(esimRes.getEstimationDemand(d));
                        for (MulticastDemand d : np.getMulticastDemands(layer))
                            d.setOfferedTraffic(esimRes.getEstimationMDemand(d));
                        JOptionPane.showMessageDialog(null, "Estimation saved as offered traffic of the demands", "Output info", JOptionPane.INFORMATION_MESSAGE);
                    }
            );
        }
                , (a,b)->b>0, null);

    }

    
	private static LocalDateTime dateToLocalDateTime(Date date) { return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()); 	}

	private static Date localDateTimeToDate(LocalDateTime localDateTime) { return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()); 	}

}

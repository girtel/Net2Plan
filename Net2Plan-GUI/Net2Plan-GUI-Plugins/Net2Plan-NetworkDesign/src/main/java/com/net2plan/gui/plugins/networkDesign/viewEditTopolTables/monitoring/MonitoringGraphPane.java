/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring;


import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.PickManager;
import com.net2plan.gui.utils.NetworkElementOrFr;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.CustomHTMLEditorKit;
import com.net2plan.libraries.TrafficPredictor;
import com.net2plan.utils.Pair;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

public class MonitoringGraphPane extends JPanel
{
	private static final long serialVersionUID = 15555L;
	private final GUINetworkDesign callback;

	public MonitoringGraphPane(GUINetworkDesign callback)
	{
		super();

		this.callback = callback;

		this.setVisible(true);
		this.setLayout(new MigLayout("insets 0, fill, wrap 1"));
		this.setBackground(Color.WHITE);

		StandardChartTheme theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
		theme.setPlotBackgroundPaint(GUINetworkDesignConstants.YELLOW_BRANCH_COLOR);
		theme.setDomainGridlinePaint(Color.LIGHT_GRAY);
		theme.setRangeGridlinePaint(Color.LIGHT_GRAY);
		theme.setExtraLargeFont(this.getFont().deriveFont(Font.BOLD, 20));
		theme.setLargeFont(this.getFont().deriveFont(Font.PLAIN, 18));
		theme.setRegularFont(this.getFont().deriveFont(Font.PLAIN, 12));
		theme.setSmallFont(this.getFont().deriveFont(Font.PLAIN, 10));

		Paint[] paintSequence = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
		Shape[] shapes = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE;
		if(paintSequence.length > 3 && shapes.length > 3)
		{
			paintSequence[0] = GUINetworkDesignConstants.YELLOW_BRANCH_COLOR;
			paintSequence[1] = Color.DARK_GRAY;
			paintSequence[2] = Color.GREEN;
			shapes[0] = shapes[1]; shapes[2] = shapes[0];
		}
		DrawingSupplier supplier = new DefaultDrawingSupplier(paintSequence, paintSequence, DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE, DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, shapes);
		theme.setDrawingSupplier(supplier);

		ChartFactory.setChartTheme(theme);
	}

	public void updateView()
	{
		this.removeAll();
		final NetPlan netPlan = callback.getDesign();
		Optional<PickManager.PickStateInfo> pickInfo = callback.getPickManager().getCurrentPick(netPlan);

		if (!pickInfo.isPresent())
		{
			resetView();
			return;
		}

		PickManager.PickStateInfo pickStateInfo = pickInfo.get();

		Optional<Pair<Constants.NetworkElementType, NetworkLayer>> d = pickStateInfo.getElementTypeOfMainElement();

		if(!d.isPresent())
		{
			resetView();
			return;
		}

		final GUINetworkDesignConstants.AJTableType elementType = GUINetworkDesignConstants.AJTableType.getTypeOfElement(d.get().getFirst());
		final List<NetworkElementOrFr> pickedElement = pickStateInfo.getStateOnlyNeFr();

		assert pickedElement != null && !pickedElement.isEmpty();
		assert elementType != null;

		/* Return empty panel if zero or more than one element is picked */
		/* Check if remove everything */
		if (elementType == null || pickedElement.size() > 1)
		{
			resetView();
			return;
		}

		NetworkElementOrFr nefr = pickedElement.get(0);

		if (nefr.getNe() != null && !(nefr.getNe() instanceof IMonitorizableElement))
		{
			resetView();
			return;
		}

		final IMonitorizableElement monitElement = (IMonitorizableElement) nefr.getNe();
		if (monitElement.getMonitoredOrForecastedCarriedTraffic().getSize() == 0 && !monitElement.getTrafficPredictor().isPresent())
		{
			resetView();
			return;
		}


		this.add(createPanelInfo(monitElement), "al center");

		this.revalidate();
		this.repaint();
	}

	public void resetView()
	{
		this.removeAll();
		this.add(createEmtpyPanel(), "top");
		this.revalidate();
		this.repaint();
	}

	private JPanel createPanelInfo(IMonitorizableElement e)
	{
		TimeSeries ts_monitoringInformation = null;
		if (e.getMonitoredOrForecastedCarriedTraffic().getSize () > 0)
		{
			ts_monitoringInformation = new TimeSeries ("Monitoring samples");
			final SortedMap<Date , Double> values = e.getMonitoredOrForecastedCarriedTraffic().getValues();
			for (Entry<Date,Double> val : values.entrySet())
				ts_monitoringInformation.add (new FixedMillisecond(val.getKey().getTime()) , val.getValue());
		}
		TimeSeries ts_predictorInformation_average = null;
		TimeSeries ts_predictorInformation_95 = null;
		if (e.getTrafficPredictor().isPresent())
		{
			final boolean thereAreMonitSamples = e.getMonitoredOrForecastedCarriedTraffic().getSize() > 0;
			final Date initialDate = thereAreMonitSamples? e.getMonitoredOrForecastedCarriedTraffic().getFirstDate()  : Calendar.getInstance().getTime();
			final Date endDate;
			if (thereAreMonitSamples)
			{
				final Date lastSample = e.getMonitoredOrForecastedCarriedTraffic().getLastDate();
				final Date initialDatePlusSixMonths = shiftDateNumberOfDays(initialDate, 180.0);
				endDate = initialDatePlusSixMonths.after(lastSample)? initialDatePlusSixMonths : shiftDateNumberOfDays(lastSample, 365.0);
			}
			else
				endDate = shiftDateNumberOfDays(initialDate , 365 * 5);
			final int numPredictionSamples = 200;
			final SortedSet<Date> predictionDates = dateRange (initialDate , endDate , numPredictionSamples);
			final TrafficPredictor tp = e.getTrafficPredictor().get();
			ts_predictorInformation_average = new TimeSeries ("Forecast");
			Function<Date,Double> predictorFunction = tp.getPredictorFunctionNoConfidenceInterval();
			for (Date d : predictionDates)
				ts_predictorInformation_average.add(new FixedMillisecond(d) , predictorFunction.apply(d));

			try
			{
				predictorFunction = tp.getPredictorFunction(0.05);
				ts_predictorInformation_95 = new TimeSeries ("Conservative forecast (95%)");
				predictorFunction = tp.getPredictorFunction(0.05);
				for (Date d : predictionDates)
					ts_predictorInformation_95.add(new FixedMillisecond(d) , predictorFunction.apply(d));
			} catch (Exception ee ) {}
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		if (ts_monitoringInformation != null) dataset.addSeries(ts_monitoringInformation);
		if (ts_predictorInformation_average != null) dataset.addSeries(ts_predictorInformation_average);
		if (ts_predictorInformation_95 != null) dataset.addSeries(ts_predictorInformation_95);

		NetworkElement eAsNe = (NetworkElement) e;
		String descriptionString_e = "No element information";
		switch (NetworkElement.getNetworkElementType(eAsNe))
		{
			case MULTICAST_DEMAND: descriptionString_e = "Multicast Demand #" + eAsNe.getIndex(); break;
			case DEMAND: descriptionString_e = "Demand #" + eAsNe.getIndex(); break;
			case LINK: descriptionString_e = "Link #" + eAsNe.getIndex(); break;
			default:
				break;
		}

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"Traffic monitoring & forecasting - " + descriptionString_e,
				"Date", // x-axis label
				"Traffic (Gbps)", // y-axis label
				dataset, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);

		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

		if(e instanceof Link)
		{
			if(ts_monitoringInformation != null || ts_predictorInformation_95 != null || ts_predictorInformation_average != null)
			{
				final Link ee = (Link)e;
				Marker currentEnd = new ValueMarker(ee.getCapacity());
				currentEnd.setPaint(Color.RED);
				currentEnd.setLabel("Nominal capacity");
				currentEnd.setLabelAnchor(RectangleAnchor.TOP_LEFT);
				currentEnd.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
				currentEnd.setLabelFont(this.getFont().deriveFont(Font.ITALIC, 13));
				currentEnd.setLabelBackgroundColor(Color.CYAN);
				currentEnd.setStroke(new BasicStroke(2f));
				plot.addRangeMarker(currentEnd);
			}
		}
		final XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer)
		{
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setDefaultShapesVisible(true);
			renderer.setDefaultShapesFilled(true);
		}

		final DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(500, 350));
		chartPanel.setMouseWheelEnabled(true);

		JPopupMenu rcMenu = chartPanel.getPopupMenu();
		JMenuItem newWindowOption = new JMenuItem("Open in new Window");
		newWindowOption.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				final JFrame frame = new JFrame();
				final ChartPanel chartNewPanel = new ChartPanel(chart);
				chartPanel.setMouseWheelEnabled(true);
				frame.add(chartNewPanel);
				frame.pack();
				frame.setVisible(true);
			}
		});
		rcMenu.add(newWindowOption);

		return chartPanel;
	}

	private JEditorPane createEmtpyPanel()
	{
		final JEditorPane editor = new CustomHTMLEditorKit.CustomJEditorPane();
		editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		editor.setContentType("text/html;charset=UTF-8");
		editor.setEditable(false);

		final CustomHTMLEditorKit htmlEditorKit = new CustomHTMLEditorKit();
		editor.setEditorKit(htmlEditorKit);

		Document doc = htmlEditorKit.createDefaultDocument();
		doc.putProperty("IgnoreCharsetDirective", true);
		editor.setDocument(doc);

		final StringBuilder html = new StringBuilder();

		html.append("<html>");
		html.append("<header>");
		html.append(CustomHTMLEditorKit.style);
		html.append("</header");
		html.append("<body>");
		html.append("<h3>Please, pick an element with monitoring traces or a traffic forecasting to show its monitoring information info.</h3>");
		html.append("</body>");
		html.append("</html>");

		editor.setText(html.toString());
		editor.setCaretPosition(0);
		return editor;
	}

	private static Date shiftDateNumberOfDays (Date original , double moreDays)
	{
		final long newTime = (long) (original.getTime() + 24*3600*1000*moreDays);
		return new Date (newTime < 0? 0 : newTime);
	}
	private SortedSet<Date> dateRange (Date initialDate , Date endDate , int numberOfSamples)
	{
		final SortedSet<Date> res = new TreeSet<> ();
		assert !initialDate.after(endDate);
		final long milisecondsBetweenSamples = (long) ((endDate.getTime() - initialDate.getTime ()) / (Math.max(2.0, -1.0 + numberOfSamples)));
		Date currentDate = initialDate;
		do
		{
			res.add(currentDate);
			currentDate = new Date (currentDate.getTime() + milisecondsBetweenSamples);
		} while (!currentDate.after(endDate));
		return res;
	}
}

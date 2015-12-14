package com.beamatronic.jsonanalysistool;

// Brian Williams
// Created:			June 25, 2015 
// Last Updated:	July 7, 2015
// JSON Analysis Tool
//
// Dependencies
// 
// Developed with Java 1.8 on Mac OS X
//
// https://github.com/jayway/JsonPath
// http://www.swtchart.org/
// Apache HTTPClient
// Jackson

// All jars from Eclipse .classpath:
//
// swt-4.4.2-cocoa-macosx-x86_64/swt.jar
// httpcomponents-client-4.3.6/lib/commons-codec-1.6.jar
// httpcomponents-client-4.3.6/lib/commons-logging-1.1.3.jar
// httpcomponents-client-4.3.6/lib/fluent-hc-4.3.6.jar
// httpcomponents-client-4.3.6/lib/httpclient-4.3.6.jar
// httpcomponents-client-4.3.6/lib/httpclient-cache-4.3.6.jar
// httpcomponents-client-4.3.6/lib/httpcore-4.3.3.jar
// httpcomponents-client-4.3.6/lib/httpmime-4.3.6.jar
// slf4j-api-1.7.12.jar
// json-path-2.0.0.jar
// json-smart-2.1.1.jar
// asm-1.0.2.jar
// plugins/org.swtchart_0.9.0.v20140219.jar
// jackson-annotations-2.4.3.jar
// jackson-core-2.4.3.jar
// jackson-databind-2.4.3.jar

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonExpressionHolder {
	
	// The purpose of this class, JsonExpressionHolder, is to hold the unique list of
	// all Json Path Expressions that have been found
	
	// TODO 2015-07-13
	// You might get expressions that refer to paths that
	// don't work on all peers in a list.  So if we kept a 
	// count of how many times we had seen each expression,
	// we could flag it in the UI.
	
	List<String> allJsonExpressions;
	
	public JsonExpressionHolder() {
		allJsonExpressions = new ArrayList<String>();
	}
	
	public void clear() {
		allJsonExpressions.clear();
	}
	
	public List<String> getExpressions() {
		return allJsonExpressions;
	}
	
	// This supports putting the expressions into the text box
	public String getAllExpressionsNewlineDelimited() {
		String rval = "";
		for (int i = 0; i < allJsonExpressions.size(); i++) {
			rval = rval + allJsonExpressions.get(i) + "\n";
		}
		return rval;
	}
	
	public void add(String s) {
		// Don't add it if it already exists
		
		boolean wasFound = false;
		
		for (int i = 0; i < allJsonExpressions.size(); i++) {
			String eachOne = allJsonExpressions.get(i);
			if (s.equals(eachOne)) {
				wasFound = true;
			}
		}
		
		if (!wasFound) {
			allJsonExpressions.add(s);
		}	
	}
			

	public void print() {
		int i = 0;
		for (String s : allJsonExpressions) {
			System.out.println("Json Expression " + i + ": " + allJsonExpressions.get(i));
			i++;
		}
	}
}


public class JsonAnalysisTool {

	// Convenience method
	private static GridData getNewHorizontalFillGridLayout() {
        GridData newGridDataObject = new GridData();
        newGridDataObject.horizontalAlignment       = GridData.FILL;
        newGridDataObject.grabExcessHorizontalSpace = true;
        return newGridDataObject;
	}

	// Convenience method
	private static GridLayout getNewOneColumnGridLayout() {
		GridLayout newGridLayoutObject = new GridLayout();
		newGridLayoutObject.numColumns = 1;
		return newGridLayoutObject;
	}

	// Convenience method
	private static GridLayout getNewTwoColumnGridLayout() {
		GridLayout newGridLayoutObject = new GridLayout();
		newGridLayoutObject.numColumns = 2;
		return newGridLayoutObject;
	}

	// Recursive method to build the TreeItem tree for the UI and also to
	// get the list of Json Path Expressions as it traverses the
	// JsonNode that was built from the HTTP response.
	private static void addTreeItemFromJsonNode(TreeItem tn, JsonNode jn, String expressionPath, String yourName, JsonExpressionHolder expressionHolder) {
		
		String textValue, valueString, nodeType, eachChildName, myText;
		JsonNode eachChild;
		
		nodeType    = jn.getNodeType().toString();
		valueString = jn.asText();
		textValue   = jn.textValue();
		int mySize  = jn.size();
				
		// Only Objects and Arrays need size, really.
		
		if (yourName == null) {
			myText = "(no name)" + " : (Type " + nodeType + ") : " + valueString ;
		} else {
			myText = yourName + " : (Type " + nodeType + ") : " + valueString;
		}
		
		if (jn.isObject() || jn.isArray()) {
			myText = myText + " (size " + mySize + ")";
		}
		
		String newExpressionPath = "";

		// TIL that if a String is null it's .length() is zero
		if (yourName != null) {
			if (yourName.length() == 0) {
				yourName = "EMPTYSTRING";	// this is just for debugging
			}
		}
		
		if (jn.isArray()) {
			if (expressionPath == null) {
				if (yourName == null) {
					newExpressionPath = "Q";	// This can not happen
				}
				else {
					// Jsonnode is an array
					// Expression path is null
					// yourName is not null
					newExpressionPath = yourName + ".[*]";
				}	
			}
			else {
				if (yourName == null) {
					newExpressionPath = expressionPath + "." + "[*]";
				}
				else {
					newExpressionPath = expressionPath + "." + yourName + "[*]";
				}
			}
		} // json node is an array
		else {
			if (expressionPath == null) {
				if (yourName == null) {
					newExpressionPath = "Z";	// This should not happen
				} else {
					newExpressionPath = yourName;
				}
			}
			else {
				if (yourName == null) {
					newExpressionPath = expressionPath;
				}
				else {
					newExpressionPath = expressionPath + "." + yourName;
				}
			}
		}

		if (mySize == 0) {
			tn.setText(myText);
			expressionHolder.add(newExpressionPath);			
		}
		else {  
			tn.setText(myText);
			
			if (jn.isArray()) {
				for (JsonNode eachArrayChild : jn) {
					TreeItem newChild = new TreeItem(tn ,0);
					newChild.setExpanded(true);
					// We generate an item name, for UI purposes
					// We don't want that item name in the expression path
					addTreeItemFromJsonNode(newChild, eachArrayChild, newExpressionPath, null, expressionHolder);
				}
			}
			else {
				Iterator<Map.Entry<String,JsonNode>> fields = jn.fields();
				// Array does not have fields
				while (fields.hasNext() ) {
					Map.Entry<String,JsonNode> field = fields.next();
					eachChildName = field.getKey();
					eachChild = field.getValue();
					TreeItem newChild = new TreeItem(tn ,0);
					newChild.setExpanded(true);
					addTreeItemFromJsonNode(newChild, eachChild, newExpressionPath, eachChildName, expressionHolder);
				}
			} // jn not an array
		} // my size is not zero
	} // method addTreeItemFromJsonNode
	
	// Try to apply the json path expression to the given JSON document, 
	// which should result in a list of values represented as Strings
	// Fail softly.  Return a List<String> no matter what.
	public static List<String> robustApplyJsonExpression(String bodyOfData, String jsonExpression) {

		List<String> newStringList = new ArrayList<String>();
		
		// If the Json Expression is null, or empty string, then we can just 
		// return an empty list.
		
		if (jsonExpression != null ) {
			if (jsonExpression.length() != 0) {
				String foo = null;
				
				// Perform the operation with the broadest possible type
				// TODO 2015-07-13:  Bizarre.  This might not return a list!?!
				List<Object> objectList1 = JsonPath.read(bodyOfData, jsonExpression);
						
				System.out.println("JsonPath.read returned " + objectList1.size() + " items.");
				
				for (int i = 0; i < objectList1.size(); i++) {
			    	Object obj = objectList1.get(i);
			    	try {
			    		foo = obj.toString();
			    	}
			    	catch (Exception e) {
			    		foo = "CONVERSIONERROR";	// This should not happen
			    	}
			    	newStringList.add(foo);
				}
			} // the length is not zero
		} // the json expression is not null
		
		return newStringList;
	}
	
	private static void expandTreeView(TreeItem item){
			System.out.println("Expanding tree view");
			int numChildItems = item.getItemCount();
			if (numChildItems <= 5) {
				item.setExpanded(true);
			}
	        for(TreeItem child : item.getItems()){
	            expandTreeView(child);
	        }
	}
	
	
	public static void main(String[] args) {

		int    INITIALSHELLWIDTH  = 1202;
		int    INITIALSHELLHEIGHT =  818;

		String SHELLTEXT          = "JSON Analysis Tool";
		String initialURL         = "https://api.github.com/repos/beamatronic/JsonAnalysisTool/stats/participation";
		String INITIALBIGTEXT     = "This is where the JSON data\nfrom the URL will appear\n";
		String DEFAULTCOLUMNLABELEXPRESSION = "$.rows[*].value.time";
		String DEFAULTDATAEXPRESSION        = "$.rows[*].value.memory";
		
        
        System.out.println("Welcome...");

		JsonExpressionHolder expressionHolder = new JsonExpressionHolder();

		Display display = new Display();

		Color colorBlack = new Color(display, 0, 0, 0);
		Color colorRed   = new Color(display, 255, 0, 0);
		Color colorWhite = new Color(display, 255, 255, 255);
		
		Shell shell = new Shell(display);
		
		shell.addListener (SWT.Resize,  new Listener () {
		    public void handleEvent (Event e) {
		      Rectangle rect = shell.getClientArea ();
		      System.out.println(rect);
		    }
		  });		
		
		shell.addListener (SWT.Move,  new Listener () {
		    public void handleEvent (Event e) {
		    	Point p1 = shell.getLocation();
		        System.out.println("Shell moved new location  Point x " + p1.x + " Y " + p1.y);
		    }
		  });		
		
        shell.setText(SHELLTEXT);
        shell.setSize(INITIALSHELLWIDTH, INITIALSHELLHEIGHT);
        
        Rectangle bds = shell.getMonitor().getBounds();
        System.out.println("Shell display bounds:  width  " + bds.width + " height " + bds.height);
        Point p = shell.getSize();
        System.out.println("Shell size:  Point x " + p.x + " Y " + p.y);
        int nLeft = (bds.width - p.x) / 2;
        int nTop = (bds.height - p.y) / 2;
        System.out.println("shell bounds:  left: " + nLeft + " top: " + nTop);
        shell.setBounds(nLeft, nTop, p.x, p.y);

        //GridLayout layoutForShell = new GridLayout();
        //layoutForShell.numColumns = 1;
        shell.setLayout(getNewOneColumnGridLayout());
        
        Group sourceOfDataGroup = new Group(shell, 0);
        sourceOfDataGroup.setText("Source of data");
        sourceOfDataGroup.setLayoutData(getNewHorizontalFillGridLayout());
        sourceOfDataGroup.setLayout(getNewOneColumnGridLayout());

        Composite urlAndButtonComposite = new Composite(sourceOfDataGroup, 0);
        urlAndButtonComposite.setLayout(getNewTwoColumnGridLayout());
        
        Composite basicAuthComposite = new Composite(sourceOfDataGroup, 0);
        RowLayout layoutForBasicAuth = new RowLayout();
        basicAuthComposite.setLayout(layoutForBasicAuth);
        Button useBasicAuthCheckbox = new Button(basicAuthComposite, SWT.CHECK);
        useBasicAuthCheckbox.setText("Use BASIC Auth?");
        Label usernameLabel = new Label(basicAuthComposite, 0);
              usernameLabel.setText("Username:");
        Text usernameText = new Text(basicAuthComposite, SWT.BORDER);        
             usernameText.setText("Administrator");
        Label passwordLabel = new Label(basicAuthComposite, 0);
              passwordLabel.setText("Password:");
        Text passwordText = new Text(basicAuthComposite, SWT.PASSWORD | SWT.BORDER);
             passwordText.setText("password");
        

        useBasicAuthCheckbox.addSelectionListener(new SelectionListener() {
        	   
            public void widgetSelected(SelectionEvent event) {
            	
            	 Button button = (Button) event.widget;
                 if (button.getSelection()) {
                     System.err.println("useBasicAuthCheckbox selected");
            	 }
                 else {
                     System.err.println("useBasicAuthCheckbox deselected");
                 }
            }

            public void widgetDefaultSelected(SelectionEvent event) {
                System.err.println("useBasicAuthCheckbox widgetDefaultSelected");
            }
          });      
        
        Text textForDataURL = new Text(urlAndButtonComposite, 0);
        textForDataURL.setText(initialURL);
        Button getDataButton = new Button(urlAndButtonComposite, 0);
        
        Text bigText = new Text(sourceOfDataGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData gridDataForBigText = new GridData();
        gridDataForBigText.horizontalAlignment = GridData.FILL;
        gridDataForBigText.grabExcessHorizontalSpace = true;
        bigText.setLayoutData(gridDataForBigText);
        
        bigText.setText(INITIALBIGTEXT);
        
        Tree jsonTree = new Tree(sourceOfDataGroup, 0);
        
        GridData gridDataForJSONTree = new GridData();
        gridDataForJSONTree.horizontalAlignment = GridData.FILL;
        gridDataForJSONTree.grabExcessHorizontalSpace = true;
        gridDataForJSONTree.heightHint = 100;
        jsonTree.setLayoutData(gridDataForJSONTree);
            
        TreeItem jsonTreeItem = new TreeItem(jsonTree, 0);
        jsonTreeItem.setExpanded(true);
        
        getDataButton.setText("Get data");
        getDataButton.setSize(new Point(50,50));
        getDataButton.pack();

        bigText.pack();
        // sourceOfDataGroup.pack();

        Group midwayGroup = new Group(shell, 0);
        //midwayGroup.setText("midway");
        midwayGroup.setLayoutData(getNewHorizontalFillGridLayout());
        midwayGroup.setLayout(getNewTwoColumnGridLayout());

        Group allExpressionsGroup = new Group(midwayGroup,0);
        allExpressionsGroup.setText("Suggested JsonPath Expressions");
        allExpressionsGroup.setLayoutData(getNewHorizontalFillGridLayout());
        allExpressionsGroup.setLayout(getNewOneColumnGridLayout());
        
        Text allExpressionsText = new Text(allExpressionsGroup , SWT.V_SCROLL);
        allExpressionsText.setText("This is where suggested JsonPath expressions will appear\n\n\n\n");
        allExpressionsText.setLayoutData(getNewHorizontalFillGridLayout());

        // TODO 2015-07-13
        // Make it so that you can get the JSON data directly from the text box
        // Say if you paste it in, and don't need or want to get it from URL.
        
        getDataButton.addSelectionListener(new SelectionListener() {
        	   
            public void widgetSelected(SelectionEvent event) {
            	
            	String bodyAsString = null;
            	
        		String myURL = textForDataURL.getText();
        		
        		if (myURL.length() == 0 ) {
        			System.out.println("I won't get data from the URL, I will get it from bigText.");
        			bodyAsString = bigText.getText();
        		}
        		else {
	        		System.out.println("Will get data from URL.  My url is " + myURL);
	            	boolean useBasicAuth = useBasicAuthCheckbox.getSelection();
	        		HttpClient httpclient = HttpClients.createDefault();
	        		HttpGet    httpget = new HttpGet(myURL);		
	        		
	            	if (useBasicAuth) {
	            		System.out.println(usernameText.getText());
	            		System.out.println(passwordText.getText());
	            		UsernamePasswordCredentials upc = new UsernamePasswordCredentials(usernameText.getText(), passwordText.getText());
	            		httpget.addHeader(BasicScheme.authenticate(upc, "UTF-8", false));	            		
	            	}
	
	        		try {
	        			HttpResponse response = httpclient.execute(httpget);
	        			bodyAsString = EntityUtils.toString(response.getEntity());
	        			
	        			if (bodyAsString == null) {
	                		bigText.setText("HTTP Response String was null");
	        			}
	        			else {
	        				if (bodyAsString.length() == 0) {
	                    		bigText.setText("HTTP Response String length was zero.  Perhaps you need Basic Auth?");
	        				}
	        				else {				
			        			bigText.setText(bodyAsString);
	        				} // bodyAsString is not zero length
	        			} // bodyAsString is not null
	        		} // try
	        		catch (Exception e) {
	            		bigText.setText(e.getMessage());
	            		e.printStackTrace();	
	         		}

        		} // myURL length is not zero
        		
    			// If bigText already had the data that we wanted 
    			// then we would start by grabbing that text from bigText
    			// and putting it into bodyAsString
    			// and then starting from here
    			    			
    			try {
        			ObjectMapper om = new ObjectMapper();
	    			JsonNode jsonNode = om.readTree(bodyAsString);	
	    			
	    			// UI Housekeeping - Clean Up!
	    			jsonTreeItem.removeAll();
	    			expressionHolder.clear();
	    			jsonTreeItem.setExpanded(true);
	    			
	    			// This is a recursive method that does a number of things at the same time
	    			// It creates a TreeItem structure that is parallel to the JsonNode structure
	    			// Along the way it computes JsonPath expressions dynamically
	    			// and adds them to the global variable
	    			addTreeItemFromJsonNode(jsonTreeItem, jsonNode, null, "$", expressionHolder);        			
	    			allExpressionsText.setText(expressionHolder.getAllExpressionsNewlineDelimited());
	    			
	    			expandTreeView(jsonTreeItem);
    			}
    			catch (Exception e) {
    				e.printStackTrace();
    			}
    			
            
            
            } // widgetSelected()

            public void widgetDefaultSelected(SelectionEvent event) {
              bigText.setText("No worries!");
            }
          });      
        
        // Start Expression Editor stuff
        
        Group expressionGroup = new Group(midwayGroup,0);     
        expressionGroup.setText("JsonPath Expression Editor");
        expressionGroup.setLayoutData(getNewHorizontalFillGridLayout());
        expressionGroup.setLayout(getNewOneColumnGridLayout());
        
        Composite xComposite = new Composite(expressionGroup, 0);
        xComposite.setLayoutData(getNewHorizontalFillGridLayout());
        xComposite.setLayout(getNewTwoColumnGridLayout());
        Label xLabel = new Label(xComposite, 0);
        xLabel.setText("X:");
  
        Text columnLabelsExpression = new Text(xComposite,0);
        columnLabelsExpression.setText(DEFAULTCOLUMNLABELEXPRESSION);        
        columnLabelsExpression.setLayoutData(getNewHorizontalFillGridLayout());

        Composite yComposite = new Composite(expressionGroup, 0);
        yComposite.setLayoutData(getNewHorizontalFillGridLayout());
        yComposite.setLayout(getNewTwoColumnGridLayout());
        Label yLabel = new Label(yComposite, 0);
        yLabel.setText("Y:");

        Text dataExpression         = new Text(yComposite,0);
        dataExpression.setText(DEFAULTDATAEXPRESSION);
        dataExpression.setLayoutData(getNewHorizontalFillGridLayout());
        
        // Start of Results Group
        // Remember
        // setLayoutData is for yourself
        // setLayout is for your children
        
        Group allResultsGroup = new Group(shell, 0);
        allResultsGroup.setText("All Results");        
        allResultsGroup.setLayoutData(getNewHorizontalFillGridLayout());
        
        FillLayout layoutForAllResultsGroup = new FillLayout();
        layoutForAllResultsGroup.type = SWT.HORIZONTAL;
        allResultsGroup.setLayout(layoutForAllResultsGroup);           
        
        Group tableGroup = new Group(allResultsGroup, 0);        
        tableGroup.setText("Table of results");
        Table resultsTable = new Table(tableGroup,SWT.BORDER);
        resultsTable.setSize(new Point(300,200));
        
        TableColumn tc1 = new TableColumn(resultsTable, SWT.CENTER);
        TableColumn tc2 = new TableColumn(resultsTable, SWT.CENTER);
        tc1.setText("X");
        tc2.setText("Y");
        tc1.setWidth(150);
        tc2.setWidth(150);
        resultsTable.setHeaderVisible(true);
                
        Group chartGroup = new Group(allResultsGroup, SWT.NONE);
        chartGroup.setText("Chart of results");
        chartGroup.setSize(new Point(200,200));
        
        Chart chart = new Chart(chartGroup, SWT.NONE);
        
        Button applyButton = new Button(expressionGroup, 0);
        applyButton.setText("Apply expressions");
        applyButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
            	
            	try {
            		// Housekeeping - Clean Up
	                resultsTable.removeAll();

	                String jsonFromBigText = (String) bigText.getText();
	                // System.out.println("Json length: " + jsonFromBigText.length());

	                // Get the JsonPath expressions that the user entered
	                String jsonExpression1 = columnLabelsExpression.getText();
	                String jsonExpression2 = dataExpression.getText();

	                // Assume there is no error in the expressions.  Make them black.
                	columnLabelsExpression.setForeground(colorBlack);
                	dataExpression.setForeground(colorBlack);

	                // Evaluate them both as strings
	                List<String> columnLabelList = null;
	                try {
	                	columnLabelList = robustApplyJsonExpression(jsonFromBigText, jsonExpression1);       
	                }
	                catch (Exception e) {
	                	e.printStackTrace();
	                	columnLabelsExpression.setForeground(colorRed);
	                	columnLabelsExpression.redraw();
	                	columnLabelList = new ArrayList<String>();
	                }
	                	                
	                List<String> dataValueList = null;
	                try {
	                	dataValueList = robustApplyJsonExpression(jsonFromBigText, jsonExpression2);       
	                }
	                catch (Exception e) {
	                	e.printStackTrace();
	                	dataExpression.setForeground(colorRed);
	                	dataExpression.redraw();
	                	dataValueList = new ArrayList<String>();
	                }

	                // Independently evaluate the X and Y expressions
	                // Determine which is larger
	                // If one of them is empty, pad with zeroes to make them the same size

	                int columnLabelListSize = columnLabelList.size();
	                int dataValueListSize   = dataValueList.size();
	                int largerSize = (columnLabelListSize >= dataValueListSize ? columnLabelListSize : dataValueListSize);
	                
	                System.out.println("I have " + columnLabelListSize + " columns and " + dataValueListSize + " data values, so I will make lists of size " + largerSize);
	                
	                if (largerSize == 0) {
	                	System.out.println("Both lists have size zero.  Nothing to graph.");
	                }
	                else {
	                	// TODO 2015-07-13 This takes into account EMPTY lists
	                	// What about when one is simply smaller than the other.
	                	
	                	// How about a simple numberOfPadsToAdd which may be zero, or might not be.
	                	
	                	System.out.println("column label list needs " + (largerSize - columnLabelListSize) + " pads.");
	                	System.out.println("data value list needs   " + (largerSize - dataValueListSize)   + " pads.");
	                	
	                	for (int i = 0; i < (largerSize - columnLabelListSize); i++) {
	                		columnLabelList.add("0");
	                	}
	                		
		               	for (int i = 0; i < (largerSize - dataValueListSize); i++) {
		               		dataValueList.add("0");
		               	}
	                } // largerSize is not zero
	                
	                // TODO
	                // 2017-07-13 Two observations
	                // 1.  Anonymous objects make it hard to write json path expressions that reference them
	                // 2.  Object name could have extra periods in them which makes them look like json path notation
	                
	                // Recheck
	                columnLabelListSize = columnLabelList.size();
	                dataValueListSize   = dataValueList.size();

	                System.out.println("column label list size is " + columnLabelListSize);
	                System.out.println("data value   list size is " + dataValueListSize);
	                
	                // THE CODE BELOW ASSUMES THE LISTS ARE THE SAME SIZE, AND THAT THEY ARE
	                // THE LARGER SIZE
	                
	                // Put values into the results table UI element
	                for (int i = 0; i < largerSize; i++) {
	                	TableItem ti = new TableItem(resultsTable, SWT.NONE);
	                	String[] strings = new String[2];
	                	strings[0] = columnLabelList.get(i);
	                   	strings[1] = dataValueList.get(i);
	                	ti.setText(strings);	             	
	                }

	                // AFTER they are in the resultsTable, now try to put values into the Chart

	                // We have to do some conversion
	         	                
	                String[] xStringsArray = columnLabelList.toArray(new String[columnLabelList.size()]);    
	                
	                // If your expression has something that can't be converted to Double, this will not work
	                // So we are very robust here - we catch exceptions and just use zero if they occur
	                double[] yValuesArray = new double[dataValueList.size()];
	                double eachNewDouble = 0;
	                for (int i = 0; i < dataValueList.size(); i++) {
	                	try {
	                		eachNewDouble = Double.parseDouble(dataValueList.get(i));
	                	}
	                	catch (Exception e) {
	                		eachNewDouble = 0;
	                	}
	                	yValuesArray[i] = eachNewDouble;
	                }
	                
	                // Chart - X
	                chart.getAxisSet().getXAxis(0).setCategorySeries(xStringsArray);
	                
	                // Chart - Y
	                ISeriesSet chartSeriesSet = chart.getSeriesSet();
	                ISeries[]  seriesArray    = chartSeriesSet.getSeries();
	                ISeries    oneSeries      = seriesArray[0];
	                oneSeries.setYSeries(yValuesArray);
	                
	                // Chart - Force Update
	                chart.getAxisSet().adjustRange();
	                chart.redraw();

            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            } // applyButton - widgetSelected

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub				
			}
        });
        
        // Dummy Chart Data
        
        setUpDummyData(chart);
        
        chart.getAxisSet().adjustRange();
        
        chartGroup.pack();
        columnLabelsExpression.pack();
        dataExpression.pack();
        tableGroup.pack();
                
        // Actually cause the window to appear
        shell.open();		

        // Main Event Loop
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
              display.sleep();
            }
          }
       
        System.out.println("The window has been closed...");
       
        System.out.println("Goodbye...");

	}

	private static void setUpDummyData(Chart chart) {
        chart.setSize(new Point(500,300));
        chart.setLayoutData(getNewHorizontalFillGridLayout());
        chart.getTitle().setText("Chart of Extracted data");
        chart.getAxisSet().getXAxis(0).getTitle().setText("X Values");
        chart.getAxisSet().getYAxis(0).getTitle().setText("Y Values");
        IAxis xAxis = chart.getAxisSet().getXAxis(0);       
        xAxis.setCategorySeries(new String[] { "Jan", "Feb", "Mar", "Apr", "May" });
        xAxis.enableCategory(true);
        double[] ySeries = { 0.3, 1.4, 1.3, 1.9, 2.1 };
        ISeriesSet seriesSet = chart.getSeriesSet();        
        ISeries series = seriesSet.createSeries(SeriesType.LINE, "line series");       
        series.setYSeries(ySeries);       
	}
	
} // end of class 

// EOF

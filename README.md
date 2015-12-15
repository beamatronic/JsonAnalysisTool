# JsonAnalysisTool
A Java SWT application that combines a REST client with JSON analysis tools

This tool can obtain JSON via HTTP, and support Basic authentication if you wish.  You can also paste JSON from the clipboard into the tool.

The tool will then format the JSON into a tree format, and will find all possible dot.path expressions.  You can use one of them or enter one of your own expressions.

Finally, you may choose two dot.path expressions to represent a path to some X values, and some Y values ( the idea is that these are parallel arrays).  When you click "Apply expressions" it will update the table and generate a graph

A graphing widget will then graph the XY values.

I'm looking for a good public url that returns some good data but here it is with some github repo stats of a popular repository. 

![A screen shot](/screenshots/JsonAnalysisToolScreenShot.png?raw=true "Screen shot")

Dependencies

- https://github.com/jayway/JsonPath
- http://www.swtchart.org/
- Apache HTTPClient
- Jackson


# JsonAnalysisTool
A Java SWT application that combines a REST client with JSON analysis tools

This tool can obtain JSON via HTTP, and support Basic authentication if you wish.  You can also paste JSON from the clipboard into the tool.

The tool will then format the JSON into a tree format, and will find all possible dot.path expressions.  

Finally, you may choose two dot.path expressions to represent a path to some X values, and some Y values ( the idea is that these are parallel arrays).

A graphing widget will then graph the XY values.

![A screen shot](/screenshots/JsonAnalysisToolScreenShot.png?raw=true "Screen shot")

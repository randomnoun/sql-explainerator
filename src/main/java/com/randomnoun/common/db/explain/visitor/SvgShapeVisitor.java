package com.randomnoun.common.db.explain.visitor;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.db.explain.enums.TooltipTypeEnum;
import com.randomnoun.common.db.explain.graph.Shape;

/** Converts a shape layout into an SVG diagram, or an HTML document containing an SVG diagram.
 * 
 * @author knoxg
 */
public class SvgShapeVisitor extends ShapeVisitor {
	
	Logger logger = Logger.getLogger(SvgShapeVisitor.class);
	
	boolean asHtml = false;
	TooltipTypeEnum tooltipType = TooltipTypeEnum.SVG_TITLE;
	String css;
	String script;
	
	int indent = 0;
	PrintWriter pw;
	
	public SvgShapeVisitor(PrintWriter pw, boolean asHtml, TooltipTypeEnum tooltipType, String css, String script) {
		this.pw = pw;
		this.asHtml = asHtml;
		this.tooltipType = tooltipType;
		this.css = css;
		this.script = script;
	}
	
	@Override
	public void preVisit(Shape b) {
		String s = "";
		if (indent==0) {
			// get range
			RangeShapeVisitor rv = new RangeShapeVisitor();
			b.traverse(rv);
			logger.info("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");

			if (css==null) { css = getResource("/svg.css"); }
			
			// add 1 to max as 1px lines on the border have 0.5px of that line outside the max co-ordinates
			// js tooltips take up entire width/height as the tooltips can extend outside the svg
			String w = tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? "100%" : String.valueOf(rv.getMaxX() + 1);
			String h = tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? "100%" : String.valueOf(rv.getMaxY() + 1);

			// svg arrowhead modified from http://thenewcode.com/1068/Making-Arrows-in-SVG
			// and https://stackoverflow.com/questions/13626748/how-to-prevent-a-svg-marker-arrow-head-to-inherit-paths-stroke-width
			
			s = (asHtml ? "<!DOCTYPE html>\n" +
			  "<html>\n" +
			  "<head>\n" +
			  "<style>\n" + 
			  css + 
			  "</style>\n" +
			  "</head>\n" +
			  "<body>\n" +
			  "<svg width=\"" + w + "\" height=\"" + h + "\" class=\"sql\"" + (tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? " onload=\"initSqlExplain(evt)\"" : "") +">\n" +
			  "  <defs>\n" + 
			  "    <marker id=\"arrowhead\" markerWidth=\"12\" markerHeight=\"7\" refX=\"0\" refY=\"3.5\" orient=\"auto\" markerUnits=\"userSpaceOnUse\">\n" +
			  "      <polygon points=\"0 0, 12 3.5, 0 7\" />\n" +
			  "    </marker>\n" +
			  "  </defs>\n"
			  :
			  
			  "<?xml version=\"1.0\" standalone=\"no\"?>\n" +
			  "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" +
			  "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\"\n" +
			  "  width=\"" + w + "\" height=\"" + h + "\" class=\"sql\"" + (tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? " onload=\"initSqlExplain(evt)\"" : "") + ">\n" +
			  "  <defs>\n" +
			  "    <style type=\"text/css\"><![CDATA[" +
			  css +
			  "    ]]></style>\n" +
			  "    <marker id=\"arrowhead\" markerWidth=\"12\" markerHeight=\"7\" refX=\"0\" refY=\"3.5\" orient=\"auto\" markerUnits=\"userSpaceOnUse\">\n" +
			  "      <polygon points=\"0 0, 12 3.5, 0 7\" />\n" +
			  "    </marker>\n" +
			  "  </defs>\n"
			);
			
			indent += 4;
		}
		for (int i=0; i<indent; i++) { s += " "; }
		
		s += "<g" + ( b.getCssClass() == null ? "" : " class=\"" + b.getCssClass() + "\"") + ">\n"; // SVG group
		pw.print(s);
	}

	private String getResource(String resourceName) {
		InputStream is = SvgShapeVisitor.class.getResourceAsStream(resourceName);
		try {
			return new String(StreamUtil.getByteArray(is));
		} catch (IOException e) {
			throw new IllegalStateException("IOException", e);
		}
	}

	String toHex(Color c) {
		if (c.getAlpha() == 0) { return "transparent"; }
		String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
		if (c.getAlpha() != 255) {
			hex += String.format("#%02x", c.getAlpha());
		}
		return hex;
	}
	
	@Override
	public void visit(Shape b) {
		// TODO Auto-generated method stub
		String is = "";
		for (int i=0; i<indent; i++) { is += " "; }

		int x = b.getAbsoluteX();
		int y = b.getAbsoluteY();
		
		String s = "";
		if (b.getShape().equals("rect")) {
			s += is + "    <rect x=\"" + x + "\"" +
			    " y=\"" + y + "\"" +
			    " width=\"" + b.getWidth() + "\"" +
			    " height=\"" + b.getHeight() + "\"" +
				" style=\"" + 
			      (b.getStroke() == null ? "" : "stroke:" + toHex(b.getStroke()) + "; ") +
				  (b.getFill() == null ? "" : "fill:" + toHex(b.getFill())+ "; ") +
				  (b.getStrokeDashArray() == null ? "" : "stroke-dasharray:" + Text.escapeHtml(Text.join(b.getStrokeDashArray(), " ")) + "; ") +
				"\"";
			
			if (b.getTooltip() == null || tooltipType == TooltipTypeEnum.NONE) {
				s += "/>\n";
			} else {
				if (tooltipType == TooltipTypeEnum.ATTRIBUTE || tooltipType == TooltipTypeEnum.ATTRIBUTE_JS) {
					// replace newlines with &#10; ?
					s += " data-tooltip-html=\"" + Text.escapeHtml(b.getTooltip()) + "\" />\n";
					
				} else if (tooltipType == TooltipTypeEnum.SVG_TITLE) {
					s += ">\n" +
						is + "        <title>" + Text.escapeHtml(b.getTooltip()) + "</title>\n" +
						is + "    </rect>\n";
				}
			}
		} else if (b.getShape().equals("nestedLoop")) {
			// s += is + "<path fill=\"none\" stroke=\"black\" d=\"M 30,0 L 60 30 L 30 60 L 0 30 L 30 0\"></path>\n";
			s += is + "<path fill=\"white\" stroke=\"black\" d=\"M " + (x + 30) + "," + y + " l 30 30 l -30 30 l -30 -30 l 30 -30\">\n";
			s += is + "    <title>" + Text.escapeHtml(b.getTooltip())+ "</title>\n"; 
			s += is + "</path>\n";
			s += is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 25) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">nested</text>\n";
			s += is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 35) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">loop</text>\n";

		}
		if (b.getLabel() != null) {
			int tx = (b.getTextAnchor() == null ? (x + (b.getWidth()/2)) :
				(b.getTextAnchor().equals("start") ? (x) :
				(b.getTextAnchor().equals("end") ? (x + b.getWidth()) : 0)));
					
			s += is + "    <text x=\"" + tx + "\"" +
		      " y=\"" + (y + (b.getHeight()/2)) + "\"" +
			  " style=\"" +
		        (b.getTextColor() == null ? "" : "fill:" + toHex(b.getTextColor())+ ";") + 
		        (b.getTextAnchor() == null ? "" : "text-anchor:" + b.getTextAnchor() + ";") +
		      "\">" + 
			  Text.escapeHtml(b.getLabel()) + "</text>\n";
		}
		if (b.getConnectedTo() != null) {
			Shape ctb = b.getConnectedTo();
			int[] lineTo;
			double strokeWeight = b.getConnectedWeight() == null ? 1 : b.getConnectedWeight(); 
			if (b.getTargetPort().equals("sv")) {
				lineTo = ctb.getAbsolutePortPosition("s");
				lineTo[0] = x + b.getEdgeStartX();
			} else {
				lineTo = ctb.getAbsolutePortPosition(b.getTargetPort());
			}
			
			// still looks a bit weird, maybe something like
			//   https://stackoverflow.com/questions/27254640/svg-marker-scale-only-one-dimension-with-stroke-width
			// to scale the arrow cap
			
			if ((x + b.getEdgeStartX()) == lineTo[0]) {
				// adjust end of line to leave room for arrowhead
				// (arrow is outside the polyline otherwise we don't get a sharp point)
				int a = (y + b.getEdgeStartY() < lineTo[1]) ? -12 : 12; 
				s += is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n";
			} else if ((y + b.getEdgeStartY() == lineTo[1])) {
				int a = (x + b.getEdgeStartX() < lineTo[0]) ? -12 : 12;
				s += is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n";
			} else if (y + b.getEdgeStartY() > lineTo[1]) { // up and horizontal
				int a = (x + b.getEdgeStartX() < lineTo[0]) ? -12 : 12; 
				s += is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (x + b.getEdgeStartX()) + "," + (lineTo[1]) + " " + 
				  (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n";
			} else {  // horizontal and down
				int a = (y + b.getEdgeStartY() < lineTo[1]) ? -12 : 12;
				s += is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0]) + "," + (y + b.getEdgeStartY()) + " " + 
				  (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n";
			}
		}
		
		pw.print(s);
		indent += 4;
	}

	@Override
	public void postVisit(Shape b) {
		String s = "";
		indent -= 4;
		for (int i=0; i<indent; i++) { s += " "; }
		s += "</g>\n"; // end SVG group
		
		if (indent==4) {
			// close off SVG and HTML elements
			
			s += "<g id=\"tooltip\" visibility=\"hidden\" >\n" +
			  "		<foreignObject style=\"overflow: visible;\">\n" +
			  "		    <body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin: 0; padding: 0;\">\n" +
			  "		      <div style=\"padding: 4px; white-space: pre; display: inline-block; background: white; border: 1px solid black; box-shadow: 2px 2px 2px #333;\">Tooltip</div>\n" +
			  "		    </body>      \n" +
			  "		</foreignObject>\n" +
			  "	</g>\n";
			
			if (tooltipType == TooltipTypeEnum.ATTRIBUTE_JS) {
				if (script == null) { script = getResource("/svg.js"); }
			} else {
				script = null;
			}
			if (script != null && !asHtml) {
				s += "    <script type=\"text/ecmascript\"><![CDATA[\n" +
						script + "\n" +
					"    ]]></script>\n";
			}
			s += "</svg>\n";
			if (asHtml) {
				if (script != null) {
					s += "<script type=\"text/ecmascript\">\n" +
							script + "\n" +
						"</script>\n";
				}
				s += "</body>\n" +
					"</html>\n";
			}
			indent -= 4;
		}
		pw.print(s);
	}
	
}
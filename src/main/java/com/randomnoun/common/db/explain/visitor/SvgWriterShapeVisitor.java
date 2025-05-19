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
public class SvgWriterShapeVisitor extends ShapeVisitor {
	
	Logger logger = Logger.getLogger(SvgWriterShapeVisitor.class);
	
	boolean asHtml = false;
	TooltipTypeEnum tooltipType = TooltipTypeEnum.SVG_TITLE;
	String css;
	String script;
	
	int indent = 0;
	PrintWriter pw;
	
	public SvgWriterShapeVisitor(PrintWriter pw, boolean asHtml, TooltipTypeEnum tooltipType, String css, String script) {
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
			logger.debug("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");

			if (css == null) { css = getResource("/svg.css"); }
			
			// add 1 to max as 1px lines on the border have 0.5px of that line outside the max co-ordinates
			// js tooltips take up entire width/height as the tooltips can extend outside the svg
			String w = tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? "100vw" : String.valueOf(rv.getMaxX() + 1);
			String h = tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? "100vh" : String.valueOf(rv.getMaxY() + 1);

			// svg arrowhead modified from http://thenewcode.com/1068/Making-Arrows-in-SVG
			// and https://stackoverflow.com/questions/13626748/how-to-prevent-a-svg-marker-arrow-head-to-inherit-paths-stroke-width
			String onload = (tooltipType == TooltipTypeEnum.ATTRIBUTE_JS ? " onload=\"initSqlExplain(evt)\"" : "");
			
			s = (asHtml ? "<!DOCTYPE html>\n" +
			  "<html>\n" +
			  "<head>\n" +
			  "<style>\n" + 
			  css + 
			  "</style>\n" +
			  "</head>\n" +
			  "<body style=\"padding: 10px;\">\n" +
			  "<svg width=\"" + w + "\" height=\"" + h + "\" class=\"sql\"" + onload + ">\n" +
			  "  <defs>\n" + 
			  "    <marker id=\"arrowhead\" markerWidth=\"12\" markerHeight=\"7\" refX=\"0\" refY=\"3.5\" orient=\"auto\" markerUnits=\"userSpaceOnUse\">\n" +
			  "      <polygon points=\"0 0, 12 3.5, 0 7\" />\n" +
			  "    </marker>\n" +
			  "  </defs>\n"
			  :
			  
			  "<?xml version=\"1.0\" standalone=\"no\"?>\n" +
			  "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" +
			  "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\"\n" +
			  "  width=\"" + w + "\" height=\"" + h + "\" class=\"sql\"" + onload + ">\n" +
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
		s += " ".repeat(indent);
		s += "<g" + ( b.getCssClass() == null ? "" : " class=\"" + b.getCssClass() + "\"") + ">\n"; // SVG group
		pw.print(s);
	}

	private String getResource(String resourceName) {
		InputStream is = SvgWriterShapeVisitor.class.getResourceAsStream(resourceName);
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
		StringBuilder sb = new StringBuilder();
		String is = " ".repeat(indent);

		int x = b.getAbsoluteX();
		int y = b.getAbsoluteY();
		
		if (b.getShape().equals("rect")) {
			sb.append(is + "    <rect x=\"" + x + "\"" +
			    " y=\"" + y + "\"" +
			    " width=\"" + b.getWidth() + "\"" +
			    " height=\"" + b.getHeight() + "\"" +
				" style=\"" + 
			      // (b.getStroke() == null ? "" : "stroke:" + toHex(b.getStroke()) + "; ") +
				  // (b.getFill() == null ? "" : "fill:" + toHex(b.getFill())+ "; ") +
				  // (b.getStrokeDashArray() == null ? "" : "stroke-dasharray:" + Text.escapeHtml(Text.join(b.getStrokeDashArray(), " ")) + "; ") +
				"\"");
			
			sb.append(getTooltipSvg(b, is, "rect"));

		} else if (b.getShape().equals("nestedLoop")) {
			sb.append(is + "<path fill=\"white\" stroke=\"black\" d=\"M " + (x + 30) + "," + y + " l 30 30 l -30 30 l -30 -30 l 30 -30\"");
			sb.append(getTooltipSvg(b, is, "path"));
			sb.append(is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 25) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">nested</text>\n");
			sb.append(is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 35) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">loop</text>\n");

		} else if (b.getShape().equals("hashJoin")) {
			sb.append(is + "<path fill=\"white\" stroke=\"black\" d=\"M " + (x + 30) + "," + y + " l 30 30 l -30 30 l -30 -30 l 30 -30\"");
			sb.append(getTooltipSvg(b, is, "path"));
			sb.append(is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 25) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">hash</text>\n");
			sb.append(is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 35) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">join</text>\n");
		
		}
		
		if (b.getLabel() != null) {
			// default css for 'text' elements in our svg.css is
			//   text-anchor: middle; dominant-baseline: middle; font-size: 14px;

			int tx;
			if (b.getTextAnchor() == null) {
				tx = (b.getWidth() / 2);
			} else if (b.getTextAnchor().equals("start")) {
				tx = 0;
			} else if (b.getTextAnchor().equals("end")) {
				tx = b.getWidth();
			} else {
				tx = 0;
			}
					
			// well this is annoying
			// see https://stackoverflow.com/questions/16701522/how-to-linebreak-an-svg-text-within-javascript
			// except dx and dy co-ords don't work that well with text-anchor:middle; it turns out 
			String[] tspans = b.getLabel().split("\n");
			int ty = (b.getHeight() - (tspans.length * 14)) / 2 + 7;
			for (int i = 0; i < tspans.length; i++) {
				sb.append(is + "    <text x=\"" + (x + tx) + "\"" + // tx
			      " y=\"" + (y + ty + i * 14) + "\"" + // (y + (b.getHeight()/2))
				  " style=\"" +
			        // (b.getTextColor() == null ? "" : "fill:" + toHex(b.getTextColor())+ ";") + 
			        (b.getTextAnchor() == null ? "" : "text-anchor:" + b.getTextAnchor() + ";") +
			      "\">\n");
				sb.append(Text.escapeHtml(tspans[i]));
				sb.append(is + "    </text>\n");
			}
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
				sb.append(is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n");
			} else if ((y + b.getEdgeStartY() == lineTo[1])) {
				int a = (x + b.getEdgeStartX() < lineTo[0]) ? -12 : 12;
				sb.append(is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n");
			} else if (y + b.getEdgeStartY() > lineTo[1]) { // up and horizontal
				int a = (x + b.getEdgeStartX() < lineTo[0]) ? -12 : 12; 
				sb.append(is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (x + b.getEdgeStartX()) + "," + (lineTo[1]) + " " + 
				  (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n");
			} else {  // horizontal and down
				int a = (y + b.getEdgeStartY() < lineTo[1]) ? -12 : 12;
				sb.append(is + "    <polyline points=\"" + (x + b.getEdgeStartX()) + "," + (y + b.getEdgeStartY()) + " " +
			      (lineTo[0]) + "," + (y + b.getEdgeStartY()) + " " + 
				  (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
				  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n");
			}
		}
		
		pw.print(sb.toString());
		indent += 4;
	}

	private String getTooltipSvg(Shape b, String is, String tagName) {
		String s = "";
		if (b.getTooltip() == null || tooltipType == TooltipTypeEnum.NONE) {
			s += "/>\n";
		} else {
			if (tooltipType == TooltipTypeEnum.ATTRIBUTE || tooltipType == TooltipTypeEnum.ATTRIBUTE_JS) {
				// replace newlines with &#10; ?
				s += " data-tooltip-html=\"" + Text.escapeHtml(b.getTooltip()) + "\" />\n";
				
			} else if (tooltipType == TooltipTypeEnum.SVG_TITLE) {
				String unstyledTooltip = b.getTooltip().replaceAll("<[^>]+>", "");
				s += ">\n" +
					is + "        <title>" + unstyledTooltip + "</title>\n" +
					is + "    </" + tagName + ">\n";
			}
		}
		return s;
	}

	@Override
	public void postVisit(Shape b) {
		String s = "";
		indent -= 4;
		s += " ".repeat(indent);
		s += "</g>\n"; // end SVG group
		
		if (indent==4) {
			// close off SVG and HTML elements
			
			if (tooltipType == TooltipTypeEnum.ATTRIBUTE_JS) {
				// maybe the javascript should create this element ( the body tag can cause issues with containing doc )
				s += "<g class=\"tooltip\" visibility=\"hidden\" >\n" +
				  "    <foreignObject style=\"overflow: visible;\">\n" +
				  "        <body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin: 0; padding: 0;\">\n" +
				  "            <div class=\"tooltip\">Tooltip</div>\n" +
				  "        </body>\n" +
			      "    </foreignObject>\n" +
				  "</g>\n";
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
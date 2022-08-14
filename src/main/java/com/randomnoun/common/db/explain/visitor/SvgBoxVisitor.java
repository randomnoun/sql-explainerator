package com.randomnoun.common.db.explain.visitor;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.db.explain.graph.Box;

/** Converts a box layout into an SVG diagram.
 * 
 * (Well, a HTML diagram containing a SVG diagram, including any styles required)
 * 
 * @author knoxg
 */
public class SvgBoxVisitor extends BoxVisitor {
	
	Logger logger = Logger.getLogger(SvgBoxVisitor.class);
	
	int indent = 0;
	PrintWriter pw;
	
	public SvgBoxVisitor(PrintWriter pw) {
		this.pw = pw;
	}
	
	@Override
	public void preVisit(Box b) {
		String s = "";
		if (indent==0) {
			// get range
			RangeBoxVisitor rv = new RangeBoxVisitor();
			b.traverse(rv);
			logger.info("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");

			InputStream is = SvgBoxVisitor.class.getResourceAsStream("/svg.css");
			String css;
			try {
				css = new String(StreamUtil.getByteArray(is));
			} catch (IOException e) {
				throw new IllegalStateException("IOException", e);
			}
			
			// add 1 to max as 1px lines on the border have 0.5px of that line outside the max co-ordinates
			s = "<!DOCTYPE html>\n" +
			  "<html>\n" +
			  "<head>\n" +
			  "<style>\n" + 
			  css + 
			  "</style>\n" +
			  "</head>\n" +
			  "<body>\n" +
			  "<svg width=\"" + (rv.getMaxX()+1) + "\" height=\"" + (rv.getMaxY()+1) + "\">\n" +
			  // svg arrowhead modified from http://thenewcode.com/1068/Making-Arrows-in-SVG
			  // and https://stackoverflow.com/questions/13626748/how-to-prevent-a-svg-marker-arrow-head-to-inherit-paths-stroke-width
			  "<defs>\n" +
			  "    <marker id=\"arrowhead\" markerWidth=\"12\" markerHeight=\"7\" refX=\"0\" refY=\"3.5\" orient=\"auto\" markerUnits=\"userSpaceOnUse\">\n" +
			  "      <polygon points=\"0 0, 12 3.5, 0 7\" />\n" +
			  "    </marker>\n" +
			  "  </defs>";
			indent += 4;
		}
		for (int i=0; i<indent; i++) { s += " "; }
		
		s += "<g" + ( b.getCssClass() == null ? "" : " class=\"" + b.getCssClass() + "\"") + ">\n"; // SVG group
		pw.print(s);
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
	public void visit(Box b) {
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
			s += (b.getTooltip() == null ? "/>\n" : ">\n" +
				is + "        <title>" + Text.escapeHtml(b.getTooltip()) + "</title>\n" +
				is + "    </rect>\n");
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
			Box ctb = b.getConnectedTo();
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
	public void postVisit(Box b) {
		String s = "";
		indent -= 4;
		for (int i=0; i<indent; i++) { s += " "; }
		s += "</g>\n"; // end SVG group
		if (indent==4) {
			indent -= 4;
			s += "</svg>\n" +
			"</body>\n" +
			"</html>\n";
		}
		pw.print(s);
	}
	
}
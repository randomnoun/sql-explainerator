package com.randomnoun.common.db.explain;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.db.explain.enums.TooltipTypeEnum;
import com.randomnoun.common.db.explain.graph.Box;
import com.randomnoun.common.db.explain.json.QueryBlockNode;
import com.randomnoun.common.db.explain.layout.Layout;
import com.randomnoun.common.db.explain.parser.PlanParser;
import com.randomnoun.common.db.explain.visitor.RangeBoxVisitor;
import com.randomnoun.common.db.explain.visitor.ReweightBoxVisitor;
import com.randomnoun.common.db.explain.visitor.SvgBoxVisitor;

/** Class used to convert sql execution plans ( from 'EXPLAIN' statements ) into diagrams.
 * 
 * <p>The diagrams are based on the output of MySQL Workbench, but the algorithm has been clean-roomed 
 * so that I don't have to GPL this code.
 *
 * <p>Unit tests are based on the sakila database, which is BSD licensed.
 */
public class SqlExplainToImage {

	static Logger logger = Logger.getLogger(SqlExplainToImage.class);
	
	// The box class is how we're describing the resulting diagram. It contains boxes, which represent shapes or other containers
	Box b;
	TooltipTypeEnum tooltipType = TooltipTypeEnum.SVG_TITLE;
	String css;
	String script;
	
	public SqlExplainToImage() {
	}
	
	// @TODO set options ( alternate css resources )

	public void parseJson(String json, String server_version) throws ParseException {
		// parse the json and create the diagram
		PlanParser pp = new PlanParser();
		pp.parse(json, server_version);
		QueryBlockNode qbn = pp.getTopNode();
		createDiagram(qbn);
	}

	public void parseJson(Reader r, String server_version) throws ParseException, IOException {
		// parse the json and create the diagram
		PlanParser pp = new PlanParser();
		pp.parse(r, server_version);
		QueryBlockNode qbn = pp.getTopNode();
		createDiagram(qbn);
	}

	private void createDiagram(QueryBlockNode qbn) {
		// create the layout
		Layout layout = new Layout(qbn);
		b = layout.getLayoutBox();
		
		// translate diagram so that top-left is 0, 0
		RangeBoxVisitor rv = new RangeBoxVisitor();
		b.traverse(rv);
		b.setPosX(b.getPosX() - rv.getMinX());
		b.setPosY(b.getPosY() - rv.getMinY());
		
		ReweightBoxVisitor rwv = new ReweightBoxVisitor(rv.getMinWeight(), rv.getMaxWeight());
		b.traverse(rwv);
	}

	/** Return the diagram as SVG */
	public String getSvg() {
		if (b==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		StringWriter sw = new StringWriter();
		writeSvg(sw);
		return sw.toString();
	}
	
	public void writeSvg(Writer w) {
		if (b==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		PrintWriter pw = new PrintWriter(w);
	    SvgBoxVisitor sbv = new SvgBoxVisitor(pw, false, tooltipType, css, script);
		b.traverse(sbv);
		pw.flush();
	}
	
	/** Return the diagram as HTML */
	public String getHtml() {
		if (b==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		StringWriter sw = new StringWriter();
		writeSvg(sw);
		return sw.toString();
	}
	
	public void writeHtml(Writer w) {
		if (b==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
	    PrintWriter pw = new PrintWriter(w);
	    SvgBoxVisitor sbv = new SvgBoxVisitor(pw, true, tooltipType, css, script);
		b.traverse(sbv);
		pw.flush();
	}

	public TooltipTypeEnum getTooltipType() {
		return tooltipType;
	}

	public void setTooltipType(TooltipTypeEnum tooltipType) {
		this.tooltipType = tooltipType;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}



}

package com.randomnoun.common.db.explain;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.db.explain.enums.TooltipTypeEnum;
import com.randomnoun.common.db.explain.graph.Shape;
import com.randomnoun.common.db.explain.json.QueryBlockNode;
import com.randomnoun.common.db.explain.layout.Layout;
import com.randomnoun.common.db.explain.layout.CompatibleLayout;
import com.randomnoun.common.db.explain.parser.PlanParser;
import com.randomnoun.common.db.explain.visitor.RangeShapeVisitor;
import com.randomnoun.common.db.explain.visitor.ReweightShapeVisitor;
import com.randomnoun.common.db.explain.visitor.SvgWriterShapeVisitor;

/** Class used to convert sql execution plans ( from 'EXPLAIN' statements ) into diagrams.
 * 
 * <p>The diagrams are based on the output of MySQL Workbench, but the algorithm has been clean-roomed 
 * so that I don't have to GPL this code.
 *
 * <p>Unit tests are based on the sakila database, which is BSD licensed.
 */
public class SqlExplainerator {

	static Logger logger = Logger.getLogger(SqlExplainerator.class);
	
	// The shape class is how we're describing the resulting diagram. It contains shapes, which represent rectangles or other containers
	Shape s;
	TooltipTypeEnum tooltipType = TooltipTypeEnum.SVG_TITLE;
	String css;
	String script;
	String serverVersion;
	PlanParser planParser;
	Layout layout;
	
	public SqlExplainerator() {
		// defaults
	}
	
	// @TODO set options ( alternate css resources )

	public void parseJson(String json) throws ParseException {
		// parse the json and create the diagram
		if (planParser == null) {
			planParser = new PlanParser(serverVersion, true);
		}
		planParser.parse(json);
		QueryBlockNode qbn = planParser.getTopNode();
		createDiagram(qbn);
	}

	public void parseJson(Reader r) throws ParseException, IOException {
		// parse the json and create the diagram
		if (planParser == null) {
			planParser = new PlanParser(serverVersion, true);
		}
		planParser.parse(r);
		QueryBlockNode qbn = planParser.getTopNode();
		createDiagram(qbn);
	}

	private void createDiagram(QueryBlockNode qbn) {
		// create the layout
		if (layout == null) {
			 layout = new CompatibleLayout();
		}
		layout.setQueryBlockNode(qbn);
		s = layout.getLayoutShape();
		
		// translate diagram so that top-left is 0, 0
		RangeShapeVisitor rv = new RangeShapeVisitor();
		s.traverse(rv);
		s.setPosX(s.getPosX() - rv.getMinX());
		s.setPosY(s.getPosY() - rv.getMinY());
		
		ReweightShapeVisitor rwv = new ReweightShapeVisitor(rv.getMinWeight(), rv.getMaxWeight());
		s.traverse(rwv);
	}

	/** Return the diagram as SVG */
	public String getSvg() {
		if (s==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		StringWriter sw = new StringWriter();
		writeSvg(sw);
		return sw.toString();
	}
	
	public void writeSvg(Writer w) {
		if (s==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		PrintWriter pw = new PrintWriter(w);
	    SvgWriterShapeVisitor sbv = new SvgWriterShapeVisitor(pw, false, tooltipType, css, script);
		s.traverse(sbv);
		pw.flush();
	}
	
	/** Return the diagram as HTML */
	public String getHtml() {
		if (s==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
		StringWriter sw = new StringWriter();
		writeSvg(sw);
		return sw.toString();
	}
	
	public void writeHtml(Writer w) {
		if (s==null) { throw new IllegalStateException("call parseJson() before generating diagram"); }
	    PrintWriter pw = new PrintWriter(w);
	    SvgWriterShapeVisitor sbv = new SvgWriterShapeVisitor(pw, true, tooltipType, css, script);
		s.traverse(sbv);
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

	public Layout getLayout() {
		return layout;
	}

	public void setLayout(Layout layout) {
		this.layout = layout;
	}

	public PlanParser getPlanParser() {
		return planParser;
	}

	public void setPlanParser(PlanParser planParser) {
		this.planParser = planParser;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}



}

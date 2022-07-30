package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** The JSON created by this class is similar to the GSON 'pretty' JSON output, but simple elements 
 * within an array ( numbers, Strings ) are shown on the same line
 *  
 * @author knoxg
 */
public class CompactJsonWriter { 

	// modified from the JSONObject.toJSONString() and JSONArray.toJSONString()
	// methods in org.json.simple 
	
	int indent = 0;
	boolean isLastValueNative = true;
	
    public String getFormattedJson(String json) throws ParseException {
    	try {
	    	JSONParser jp = new JSONParser();
	    	Object jsObj = jp.parse(json);
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	PrintWriter pw = new PrintWriter(baos);
	    	writeJSONValue(jsObj, pw);
	    	pw.flush();
	    	return baos.toString();
    	} catch (IOException ioe) {
    		// should never happen
    		throw new IllegalStateException("IOException processing json", ioe);
    	} 
    }
    
    private void writeIndent(PrintWriter out) {
    	out.println();
    	for (int i=0; i<indent; i++) { out.print(' '); }
    }
    
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void writeJSONMap(Map map, PrintWriter out) throws IOException {
		if(map == null){
			out.write("null");
			return;
		}
		
		boolean first = true;
		List keyList = new ArrayList(map.keySet());
		Collections.sort(keyList);
		Iterator iter=keyList.iterator();
		
        out.write("{"); 
        indent += 2;
        writeIndent(out);
		while(iter.hasNext()){
            if(first) {
                first = false;
            } else { 
                out.print(',');
                writeIndent(out);
            }
			Object key = iter.next();
            out.write('\"');
            out.write(JSONValue.escape(String.valueOf(key)));
            out.write('\"');
            out.write(" : ");
            writeJSONValue(map.get(key), out);
		}
		indent -= 2;
		if (!first) { writeIndent(out); }
		out.write("} ");
		
	}

	@SuppressWarnings({ "rawtypes" })
	public void writeJSONValue(Object value, PrintWriter out) throws IOException {
		isLastValueNative = true;
		if(value == null){
			out.write("null");
			return;
		}
		
		if(value instanceof String){		
            out.write('\"');
			out.write(JSONValue.escape((String)value));
            out.write('\"');
			return;
		}
		
		if(value instanceof Double){
			if(((Double)value).isInfinite() || ((Double)value).isNaN())
				out.write("null");
			else
				out.write(value.toString());
			return;
		}
		
		if(value instanceof Float){
			if(((Float)value).isInfinite() || ((Float)value).isNaN())
				out.write("null");
			else
				out.write(value.toString());
			return;
		}		
		
		if(value instanceof Number){
			out.write(value.toString());
			return;
		}
		
		if(value instanceof Boolean){
			out.write(value.toString());
			return;
		}
		
		if(value instanceof Map){
			writeJSONMap((Map)value, out);
			isLastValueNative = false;
			return;
		}
		
		if(value instanceof List){
			writeJSONList((List)value, out);
			isLastValueNative = false;
            return;
		}
		
		// throw exception ?
		out.write(value.toString());
	}
	
	// lists on simple native objects are on one line
	// lists of more complex objects are on separate lines 
	@SuppressWarnings({ "rawtypes" })
	private void writeJSONList(List list, PrintWriter out) throws IOException{
		if(list == null){
			out.write("null");
			return;
		}
		
		boolean first = true;
		Iterator iter=list.iterator();
		
        out.write("[ ");
		indent+=2;
        while(iter.hasNext()){
            if(first) {
                first = false;
            } else {
                out.write(", ");
                if (!isLastValueNative) { writeIndent(out); }
            }
            
			Object value=iter.next();
			if(value == null){
				out.write("null");
				continue;
			}
			
			writeJSONValue(value, out);
		}
		out.write(" ]");
		indent-=2;
	}

}
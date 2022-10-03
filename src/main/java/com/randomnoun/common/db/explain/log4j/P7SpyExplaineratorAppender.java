package com.randomnoun.common.db.explain.log4j;

/* (c) 2022 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Log4j appender to convert p7spy-generated log events into SVG diagrams 
 *
 * <p>The code in this class is based on the WriterAppender class
 * in the log4j source code.
 * 
 * <p>Configuration properties: 
 * 
 * @TODO check what the dailyrollingfileappender calls these things
 * 
 * <p><b>something - the folder to write the diagrams to
 * <p><b>namingConvention - the naming convention
 * <p><b>ignoreDuplicates - will ignore duplicates, at the expense of memory usage
 * <p><b>minDurationMillis - if present, will only create diagrams for queries that take over minDurationMillis to execute
 *
 * <p>What you'll want to do is to create one of these appenders, and then configure
 * log4j to use this appender for any p7spy-generated logs, which will all originate
 * from Loggers in the form 'com.randomnoun.p7spy.jdbc_4_3.P7PreparedStatement' or something like that.
 * 
 * <p>Example log4j configuration:
 * <pre class="code">
 * log4j.rootLogger=DEBUG, EXPLAINERATOR
 * 
 * log4j.appender.EXPLAINERATOR=com.randomnoun.common.db.explain.log4j.P7SpyExplaineratorAppender
 * log4j.appender.EXPLAINERATOR.something=the-directory
 * 
 * 
 * log4j.logger.com.randomnoun.p7spy.jdbc_4_3.P7PreparedStatement.something=EXPLAINERATOR
 * </pre>
 * 
 * <p>And then go run some SQL and see diagrams appear in that folder, which you can then
 * convert to PNGs using inkscape, probably. Or maybe puppeteer.
 *
 * @blog http://www.randomnoun.com/wp/yet-to-be-written
 * 
 * @author knoxg
 */
public class P7SpyExplaineratorAppender
    extends AppenderSkeleton
{
    
    
    
    public final static long DEFAULT_LOG_SIZE = 1000;
    private long maximumLogSize = DEFAULT_LOG_SIZE;
    private LinkedList<LoggingEvent> loggingEvents;

    /** Create a new MemoryAppender object */
    public P7SpyExplaineratorAppender()
    {
        // this should be a LinkList since we use this data structure as a queue
        loggingEvents = new LinkedList<LoggingEvent>();
    }

    /** Create a new MemoryAppender with the specified log size
     * 
     * @param logSize The maximum number of logging events to store in this class 
     */ 
    public P7SpyExplaineratorAppender(int logSize)
    {
        this.maximumLogSize = logSize;
        // this should be a LinkList since we use this data structure as a queue
        loggingEvents = new LinkedList<LoggingEvent>();
    }

    /** Immediate flush is always set to true, regardless of how
     *  this logger is configured. 
     *
     * @param value ignored
     */
    public void setImmediateFlush(boolean value)
    {
        // this method does nothing
    }

    /** Returns value of the <b>ImmediateFlush</b> option. */
    public boolean getImmediateFlush()
    {
        return true;
    }

    /** Set the maximum log size */
    public void setMaximumLogSize(long logSize)
    {
        this.maximumLogSize = logSize;
    }

    /** Return the maximum log size */
    public long getMaximumLogSize()
    {
        return maximumLogSize;
    }

    /** This method does nothing. 
     */
    public void activateOptions()
    {
    }

    /**
       This method is called by the {@link AppenderSkeleton#doAppend}
       method.
       <p>If the output stream exists and is writable then write a log
       statement to the output stream. Otherwise, write a single warning
       message to <code>System.err</code>.
       <p>The format of the output will depend on this appender's
       layout.
     */
    public void append(LoggingEvent event)
    {
        // Reminder: the nesting of calls is:
        //
        //    doAppend()
        //      - check threshold
        //      - filter
        //      - append();
        //        - checkEntryConditions();
        //        - subAppend();
        if (!checkEntryConditions())
        {
            return;
        }

        subAppend(event);
    }

    /**
       This method determines if there is a sense in attempting to append.
       <p>It checks whether there is a set output target and also if
       there is a set layout. If these checks fail, then the boolean
       value <code>false</code> is returned. 
       
       <p>This method always returns true; which I guess means we can't have
       thresholds set on our MemoryAppender.
     */
    protected boolean checkEntryConditions()
    {
        /*
           if (this.layout == null)
           {
               errorHandler.error("No layout set for the appender named [" + name + "].");
               return false;
           } */
        return true;
    }

    /** Close this appender instance. The event log list is cleared by this method. 
     * This method is identical to calling clear()
     */
    public synchronized void close()
    {
        loggingEvents.clear();
    }
    
    /** Clear all events from this appender. */
    public synchronized void clear() 
    {
		synchronized(loggingEvents) {
			loggingEvents.clear();
		}
    }

    /** Actual appending occurs here. */
    protected void subAppend(LoggingEvent event)
    {
    	Object id = event.getMDC("p7Id");
    	Object duration = event.getMDC("p7Duration");
    	Object message = event.getMessage();
    	
    	// @TODO do things with that
    	// in order to call an 'explain' on this I'll need the db connection, args and argTypes probably.
    	// yeesh.
    	
    	
        // this.qw.write(this.layout.format(event));
        synchronized(loggingEvents) {
	        if (loggingEvents.size() >= maximumLogSize)
	        {
	            loggingEvents.removeLast();
	        }
	
	        loggingEvents.addFirst(event);
        }
    }

    /**
       The MemoryAppender does not require a layout. Hence, this method returns
       <code>false</code>.
     */
    public boolean requiresLayout()
    {
        return false;
    }

    /** Returns a list of logging events captured by this appender. (This list 
     *  is cloned in order to prevent ConcurrentModificationExceptions occuring
     *  whilst iterating through it) */
    public List<LoggingEvent> getLoggingEvents()
    {
        return new ArrayList<LoggingEvent>(loggingEvents);
    }
}

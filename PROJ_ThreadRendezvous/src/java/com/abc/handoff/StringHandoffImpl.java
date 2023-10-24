package com.abc.handoff;

import com.abc.pp.stringhandoff.*;
import com.programix.thread.*;

public class StringHandoffImpl implements StringHandoff {

    private String message;
    private boolean hasMessage;
    private boolean isPasserInStringHandoff;
    private boolean isReceiverInStringHandoff;

    public StringHandoffImpl() {
        this.message = null;
        this.hasMessage = false;
        this.isPasserInStringHandoff = false;
        this.isReceiverInStringHandoff = false;
    }

    @Override
    public synchronized void pass(String msg, long msTimeout)
            throws InterruptedException, TimedOutException, ShutdownException, IllegalStateException {
    	
    	try {
    		// if there's already a passer in the string hand off, throw IllegalStateException
        	if(isPasserInStringHandoff) {throw new IllegalStateException();}
        	
        	// drop off message
        	isPasserInStringHandoff = true;
        	message = msg;
    		hasMessage = true;
    		notifyAll();
        	
        	// if there's no timeout passer waits for message to be picked up, else there are timeouts
        	if(msTimeout == 0L) {
        		while(hasMessage) {
        			wait();
        		}
        	} else {
        		// variables to calculate timeouts
            	long msEndTime = System.currentTimeMillis() + msTimeout;
            	long msRemaining = msTimeout;
            	
            	// while message is waiting to be picked up and timeout is greater than 0, wait timeout
            	while(hasMessage && msRemaining > 0L) {
            		wait(msRemaining);
            		msRemaining = msEndTime - System.currentTimeMillis();
            	}
            	
            	// if message still hasn't been picked up throw TimedOutException
            	if(hasMessage) {
            		isPasserInStringHandoff = false;
            		throw new TimedOutException();
            	}
        	}
        	
        	isPasserInStringHandoff = false;
        	
    	} catch(InterruptedException e) {	// if any interruption occurs, catch it and throw InterruptedException
    		isPasserInStringHandoff = false;
    		
    		throw new InterruptedException();
    	}
    }

    @Override
    public synchronized void pass(String msg) throws InterruptedException, ShutdownException, IllegalStateException {
        pass(msg, 0L);
    }

    @Override
    public synchronized String receive(long msTimeout)
            throws InterruptedException, TimedOutException, ShutdownException, IllegalStateException {
    	
    	String receivedMessage;
    	
    	try {
    		// if there's already a receiver in the string hand off, throw IllegalStateException
    		if(isReceiverInStringHandoff) {throw new IllegalStateException();}
        	
    		// let other receivers know that a receiver is already in here
        	isReceiverInStringHandoff = true;
        	
        	// if a message is already dropped off, pick it up
        	if(hasMessage == true) {
        		receivedMessage = message;
        		hasMessage = false;
        		notifyAll();
        		isReceiverInStringHandoff = false;
        		
        		return receivedMessage;
        	}
        	
        	// if there's no timeout receiver waits for message to be dropped off, else there are timeouts
        	if(msTimeout == 0L) {
        		while(!hasMessage) {
        			wait();
        		}
        		
        		// message was dropped off, pick up message and return
        		receivedMessage = message;
            	hasMessage = false;
            	notifyAll();
            	isReceiverInStringHandoff = false;
            	
            	return receivedMessage;
        	} else {
        		// variables to calculate timeouts
            	long msEndTime = System.currentTimeMillis() + msTimeout;
            	long msRemaining = msTimeout;
            	
            	// while message hasn't been dropped off and timeout is greater than 0, wait timeout
            	while(!hasMessage && msRemaining > 0L) {
            		wait(msRemaining);
            		msRemaining = msEndTime - System.currentTimeMillis();
            	}
            	
            	isReceiverInStringHandoff = false;
            	
            	// if the message still hasn't been dropped off throw TimedOutException
            	if(!hasMessage) {
            		throw new TimedOutException();
            	} else {							// else message got dropped off, pick it up and return
            		receivedMessage = message;
                	hasMessage = false;
                	notifyAll();
                	
                	return receivedMessage;
            	}
        	}
    	} catch(InterruptedException e) {		// if any interruption occurs, catch it and throw InterruptedException
    		isReceiverInStringHandoff = false;
    		
    		throw new InterruptedException();
    	}
    }

    @Override
    public synchronized String receive() throws InterruptedException, ShutdownException, IllegalStateException {
        return receive(0L);
    }

    @Override
    public synchronized void shutdown() {
        notifyAll(); // Unblock all waiting threads (passers and receivers)
    }

    @Override
    public Object getLockObject() {
        return this;
    }
}
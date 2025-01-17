package com.shimizukenta.secs.hsms.impl;

import com.shimizukenta.secs.hsms.HsmsMessage;
import com.shimizukenta.secs.hsms.HsmsMessagePassThroughListener;
import com.shimizukenta.secs.hsms.HsmsSessionMessagePassThroughBiListener;
import com.shimizukenta.secs.impl.AbstractQueueBiObserver;

public class HsmsSessionMessagePassThroughQueueBiObserver extends AbstractQueueBiObserver<AbstractHsmsSession, HsmsMessagePassThroughListener, HsmsSessionMessagePassThroughBiListener, HsmsMessage> {

	public HsmsSessionMessagePassThroughQueueBiObserver(AbstractHsmsSession comm) {
		super(comm);
	}
	
	@Override
	protected void notifyValueToListener(HsmsMessagePassThroughListener listener, HsmsMessage value) {
		listener.passThrough(value);
	}
	
	@Override
	protected void notifyValueToBiListener(
			HsmsSessionMessagePassThroughBiListener biListener,
			HsmsMessage value,
			AbstractHsmsSession communicator) {
		
		biListener.passThrough(value, communicator);
	}

}

package com.shimizukenta.secs.hsmsgs.impl;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import com.shimizukenta.secs.hsms.HsmsException;
import com.shimizukenta.secs.hsms.HsmsMessage;
import com.shimizukenta.secs.hsms.HsmsSendMessageException;
import com.shimizukenta.secs.hsms.HsmsWaitReplyMessageException;
import com.shimizukenta.secs.hsms.impl.AbstractHsmsAsyncSocketChannel;
import com.shimizukenta.secs.hsms.impl.AbstractHsmsLinktest;
import com.shimizukenta.secs.hsms.impl.AbstractHsmsMessage;
import com.shimizukenta.secs.hsms.impl.HsmsMessageBuilder;
import com.shimizukenta.secs.hsms.impl.HsmsTransactionManager;
import com.shimizukenta.secs.impl.AbstractSecsLog;
import com.shimizukenta.secs.local.property.TimeoutProperty;
import com.shimizukenta.secs.secs2.Secs2BytesParseException;

public abstract class AbstractHsmsGsAsyncSocketChannel extends AbstractHsmsAsyncSocketChannel {

	private final AbstractHsmsGsCommunicator communicator;
	
	private final AbstractHsmsLinktest linktest;
	
	public AbstractHsmsGsAsyncSocketChannel(
			AsynchronousSocketChannel channel,
			AbstractHsmsGsCommunicator communicator) {
		
		super(channel);
		
		this.communicator = communicator;
		this.linktest = new AbstractHsmsGsLinktest(this, this.communicator) {};
	}
	
	@Override
	public void linktesting()
			throws HsmsSendMessageException,
			HsmsWaitReplyMessageException,
			HsmsException,
			InterruptedException {
		
		this.linktest.testing();
	}
	
	@Override
	protected HsmsMessageBuilder messageBuilder() {
		return this.communicator.msgBuilder();
	}
	
	@Override
	protected AbstractHsmsMessage buildMessageFromBytes(byte[] header, List<byte[]> bodies) throws Secs2BytesParseException {
		return HsmsMessageBuilder.fromBytes(header, bodies);
	}
	
	@Override
	protected AbstractHsmsMessage buildMessageFromMessage(HsmsMessage message) {
		if ( message instanceof AbstractHsmsMessage ) {
			return (AbstractHsmsMessage)message;
		} else {
			return HsmsMessageBuilder.build(message.header10Bytes(), message.secs2());
		}
	}
	
	private final HsmsTransactionManager<AbstractHsmsMessage> transMgr = new HsmsTransactionManager<>();
	
	@Override
	protected HsmsTransactionManager<AbstractHsmsMessage> transactionManager() {
		return this.transMgr;
	}
	
	@Override
	protected void notifyLog(AbstractSecsLog log) throws InterruptedException {
		this.communicator.notifyLog(log);
	}
	
	@Override
	protected void notifyTrySendHsmsMsgPassThrough(HsmsMessage msg) throws InterruptedException {
		this.communicator.notifyTrySendHsmsMessagePassThrough(msg);
	}
	
	@Override
	protected void notifySendedHsmsMsgPassThrough(HsmsMessage msg) throws InterruptedException {
		this.communicator.notifySendedHsmsMessagePassThrough(msg);
	}
	
	@Override
	protected void notifyReceiveHsmsMessagePassThrough(HsmsMessage msg) throws InterruptedException {
		this.communicator.notifyReceiveHsmsMessagePassThrough(msg);
	}
	
	@Override
	protected TimeoutProperty timeoutT3() {
		return this.communicator.config().timeout().t3();
	}
	
	@Override
	protected TimeoutProperty timeoutT6() {
		return this.communicator.config().timeout().t6();
	}
	
	@Override
	protected TimeoutProperty timeoutT8() {
		return this.communicator.config().timeout().t8();
	}
	
	@Override
	protected void resetLinktestTimer() {
		this.linktest.resetTimer();
	}
	
}

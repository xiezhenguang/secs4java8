package com.shimizukenta.secs.hsmsgs.impl;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.shimizukenta.secs.SecsCommunicatableStateChangeBiListener;
import com.shimizukenta.secs.SecsException;
import com.shimizukenta.secs.SecsLogListener;
import com.shimizukenta.secs.SecsMessage;
import com.shimizukenta.secs.SecsMessageReceiveBiListener;
import com.shimizukenta.secs.SecsMessageReceiveListener;
import com.shimizukenta.secs.SecsSendMessageException;
import com.shimizukenta.secs.SecsWaitReplyMessageException;
import com.shimizukenta.secs.hsms.HsmsException;
import com.shimizukenta.secs.hsms.HsmsMessage;
import com.shimizukenta.secs.hsms.HsmsMessageDeselectStatus;
import com.shimizukenta.secs.hsms.HsmsMessagePassThroughListener;
import com.shimizukenta.secs.hsms.HsmsMessageReceiveListener;
import com.shimizukenta.secs.hsms.HsmsMessageRejectReason;
import com.shimizukenta.secs.hsms.HsmsMessageSelectStatus;
import com.shimizukenta.secs.hsms.HsmsMessageType;
import com.shimizukenta.secs.hsms.HsmsSendMessageException;
import com.shimizukenta.secs.hsms.HsmsSession;
import com.shimizukenta.secs.hsms.HsmsSessionCommunicateStateChangeBiListener;
import com.shimizukenta.secs.hsms.HsmsSessionMessagePassThroughBiListener;
import com.shimizukenta.secs.hsms.HsmsSessionMessageReceiveBiListener;
import com.shimizukenta.secs.hsms.HsmsWaitReplyMessageException;
import com.shimizukenta.secs.hsms.impl.AbstractHsmsAsyncSocketChannel;
import com.shimizukenta.secs.hsms.impl.AbstractHsmsSession;
import com.shimizukenta.secs.hsms.impl.HsmsMessageBuilder;
import com.shimizukenta.secs.hsms.impl.HsmsMessagePassThroughQueueObserver;
import com.shimizukenta.secs.hsmsgs.HsmsGsCommunicator;
import com.shimizukenta.secs.hsmsgs.HsmsGsCommunicatorConfig;
import com.shimizukenta.secs.hsmsgs.HsmsGsUnknownSessionIdException;
import com.shimizukenta.secs.impl.AbstractBaseCommunicator;
import com.shimizukenta.secs.impl.AbstractSecsLog;
import com.shimizukenta.secs.impl.AbstractSecsMessage;
import com.shimizukenta.secs.impl.AbstractSecsThrowableLog;
import com.shimizukenta.secs.impl.SecsLogQueueObserver;
import com.shimizukenta.secs.secs2.Secs2;
import com.shimizukenta.secs.sml.SmlMessage;

public abstract class AbstractHsmsGsCommunicator extends AbstractBaseCommunicator implements HsmsGsCommunicator {
	
	private final HsmsGsCommunicatorConfig config;
	private final Set<AbstractHsmsSession> sessions;
	
	private final HsmsGsMessageReceiveBiObserver hsmsMsgRecvBiObserver;
	
	private final HsmsGsSecsCommunicatableBiObserver secsCommStatableObserver;
	private final HsmsGsHsmsCommunicateStateBiObserver hsmsCommStateObserver;
	
	private final SecsLogQueueObserver logQueueObserver;
	
	private final HsmsMessagePassThroughQueueObserver trySendHsmsMsgPassThroughQueueObserver;
	private final HsmsMessagePassThroughQueueObserver sendedHsmsMsgPassThroughQueueObserver;
	private final HsmsMessagePassThroughQueueObserver recvHsmsMsgPassThroughQueueObserver;
	
	private final HsmsGsMessagePassThroughBiObserver trySendHsmsMsgPassThroughBiObserver;
	private final HsmsGsMessagePassThroughBiObserver sendedHsmsMsgPassThroughBiObserver;
	private final HsmsGsMessagePassThroughBiObserver recvHsmsMsgPassThroughBiObserver;
	
	public AbstractHsmsGsCommunicator(HsmsGsCommunicatorConfig config) {
		this.config = config;
		this.sessions = config.sessionIds().stream()
				.map(i -> new AbstractHsmsGsSession(config, i.intValue()) {})
				.collect(Collectors.toSet());
		
		this.hsmsMsgRecvBiObserver = new HsmsGsMessageReceiveBiObserver();
		
		this.secsCommStatableObserver = new HsmsGsSecsCommunicatableBiObserver();
		this.hsmsCommStateObserver = new HsmsGsHsmsCommunicateStateBiObserver();
		
		this.trySendHsmsMsgPassThroughQueueObserver = new HsmsMessagePassThroughQueueObserver(this);
		this.sendedHsmsMsgPassThroughQueueObserver = new HsmsMessagePassThroughQueueObserver(this);
		this.recvHsmsMsgPassThroughQueueObserver = new HsmsMessagePassThroughQueueObserver(this);
		
		this.trySendHsmsMsgPassThroughBiObserver = new HsmsGsMessagePassThroughBiObserver();
		this.sendedHsmsMsgPassThroughBiObserver = new HsmsGsMessagePassThroughBiObserver();
		this.recvHsmsMsgPassThroughBiObserver = new HsmsGsMessagePassThroughBiObserver();
		
		this.logQueueObserver = new SecsLogQueueObserver(this);
		
		for (HsmsSession s : this.sessions) {
			
			s.addHsmsMessageReceiveBiListener(this.hsmsMsgRecvBiObserver);
			
			s.addSecsCommunicatableStateChangeBiListener(this.secsCommStatableObserver);
			s.addHsmsCommunicateStateChangeBiListener(this.hsmsCommStateObserver);
			
			s.addTrySendHsmsMessagePassThroughBiListener(this.trySendHsmsMsgPassThroughBiObserver);
			s.addSendedHsmsMessagePassThroughBiListener(this.sendedHsmsMsgPassThroughBiObserver);
			s.addReceiveHsmsMessagePassThroughBiListener(this.recvHsmsMsgPassThroughBiObserver);
		}
	}
	
	public HsmsGsCommunicatorConfig config() {
		return this.config;
	}
	
	private final HsmsMessageBuilder msgBuilder = new AbstractHsmsGsMessageBuilder() {};
	
	public HsmsMessageBuilder msgBuilder() {
		return this.msgBuilder;
	}
	
	@Override
	public void open() throws IOException {
		super.open();
		
		for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
			s.open();
		}
	}
	
	private final Object syncClosed = new Object();
	
	@Override
	public void close() throws IOException {
		
		synchronized ( this.syncClosed ) {
			
			if ( this.isClosed() ) {
				return;
			}
			
			try {
				
				for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
					if ( ! s.deselect() ) {
						s.separate();
					}
				}
			}
			catch ( InterruptedException ignore ) {
			}
			
			IOException ioExcept = null;
			
			try {
				super.close();
			}
			catch ( IOException e ) {
				ioExcept = e;
			}
			
			for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
				try {
					s.close();
				}
				catch ( IOException e ) {
					ioExcept = e;
				}
			}
			
			if ( ioExcept != null ) {
				throw ioExcept;
			}
		}
	}
	
	public Set<AbstractHsmsSession> getAbstractHsmsSessions() {
		return this.sessions;
	}
	
	@Override
	public Set<HsmsSession> getSessions() {
		return Collections.unmodifiableSet(this.getAbstractHsmsSessions());
	}
	
	@Override
	public HsmsSession getSession(int sessionId) throws HsmsGsUnknownSessionIdException {
		for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
			if ( s.sessionId() == sessionId ) {
				return s;
			}
		}
		throw new HsmsGsUnknownSessionIdException(sessionId);
	}
	
	@Override
	public boolean existSession(int sessionId) {
		for ( AbstractHsmsSession s : getAbstractHsmsSessions() ) {
			if ( s.sessionId() == sessionId ) {
				return true;
			}
		}
		return false;
	}
	
	protected void completionAction(AsynchronousSocketChannel channel)
			throws InterruptedException {
		
		final AbstractHsmsAsyncSocketChannel asyncChannel = buildAsyncSocketChannel(channel);
		final BlockingQueue<HsmsMessage> queue = new LinkedBlockingQueue<>();
		
		asyncChannel.addHsmsMessageReceiveListener(msg -> {
			try {
				queue.put(msg);
			}
			catch ( InterruptedException ignore ) {
			}
		});
		
		final Collection<Callable<Void>> tasks = new ArrayList<>();
		
		tasks.add(() -> {
			try {
				asyncChannel.reading();
			}
			catch ( HsmsException e ) {
				this.notifyLog(e);
			}
			catch ( InterruptedException ignore ) {
			}
			return null;
		});
		
		tasks.add(() -> {
			try {
				asyncChannel.linktesting();
			}
			catch ( HsmsSendMessageException | HsmsWaitReplyMessageException | HsmsException e ) {
				this.notifyLog(e);
			}
			catch ( InterruptedException ignore ) {
			}
			return null;
		});
		
		tasks.add(() -> {
			try {
				this.recvMsgTask(asyncChannel, queue);
			}
			catch ( HsmsSendMessageException | HsmsWaitReplyMessageException | HsmsException e ) {
				this.notifyLog(e);
			}
			catch ( InterruptedException ignore ) {
			}
			return null;
		});
		
		this.getAbstractHsmsSessions().forEach(session -> {
			tasks.add(() -> {
				try {
					this.trySelectRequesting(asyncChannel, session);
				}
				catch ( InterruptedException ignore ) {
				}
				return null;
			});
		});
		
		try {
			this.executeInvokeAny(tasks);
		}
		catch ( ExecutionException e ) {
			
			Throwable t = e.getCause();
			
			if ( t instanceof RuntimeException ) {
				throw (RuntimeException)t;
			}
			
			this.notifyLog(t);
		}
	}
	
	private void trySelectRequesting(
			AbstractHsmsAsyncSocketChannel asyncChannel,
			AbstractHsmsSession session)
					throws InterruptedException {
		
		for ( ;; ) {
			config.isTrySelectRequest().waitUntilTrue();
			session.waitUntilUnsetAsyncSocketChannel();
			
			try {
				HsmsMessageSelectStatus status = asyncChannel.sendSelectRequest(session)
						.map(HsmsMessageSelectStatus::get)
						.orElse(HsmsMessageSelectStatus.UNKNOWN);
				
				switch ( status ) {
				case SUCCESS:
				case ENTITY_ACTIVED: {
					session.setAsyncSocketChannel(asyncChannel);
					break;
				}
				default: {
					/* Nothing */
				}
				}
			}
			catch ( HsmsSendMessageException | HsmsWaitReplyMessageException | HsmsException e ) {
				this.notifyLog(e);
			}
			
			config.retrySelectRequestTimeout().sleep();
		}
	}
	
	private void recvMsgTask(
			AbstractHsmsAsyncSocketChannel asyncChannel,
			BlockingQueue<HsmsMessage> queue)
					throws HsmsSendMessageException,
					HsmsWaitReplyMessageException,
					HsmsException,
					InterruptedException {
		
		for ( ;; ) {
			
			final HsmsMessage msg = queue.take();
			
			switch ( msg.messageType() ) {
			case DATA: {
				
				final int id = msg.sessionId();
				
				boolean recved = false;
				
				for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
					
					if ( s.sessionId() == id ) {
						if ( s.putReceiveHsmsMessage(msg) ) {
							recved = true;
						}
						break;
					}
				}
				
				if ( ! recved ) {
					asyncChannel.sendRejectRequest(msg, HsmsMessageRejectReason.NOT_SELECTED);
				}
				break;
			}
			case SELECT_REQ: {
				
				final int id = msg.sessionId();
				
				boolean responsed = false;
				
				for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
					
					if ( s.sessionId() == id ) {
						
						if ( s.setAsyncSocketChannel(asyncChannel) ) {
							
							asyncChannel.sendSelectResponse(msg, HsmsMessageSelectStatus.SUCCESS);
							
						} else {
							
							if ( s.equalsAsyncSocketChannel(asyncChannel) ) {
								asyncChannel.sendSelectResponse(msg, HsmsMessageSelectStatus.ENTITY_ACTIVED);
							} else {
								asyncChannel.sendSelectResponse(msg, HsmsMessageSelectStatus.ENTITY_ALREADY_USED);
							}
						}
						
						responsed = true;
						break;
					}
				}
				
				if ( ! responsed ) {
					asyncChannel.sendSelectResponse(msg, HsmsMessageSelectStatus.ENTITY_UNKNOWN);
				}
				break;
			}
			case DESELECT_REQ: {
				
				final int id = msg.sessionId();
				
				boolean responsed = false;
				
				for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
					
					if ( s.sessionId() == id ) {
						boolean f = s.unsetAsyncSocketChannel();
						HsmsMessageDeselectStatus status = f ? HsmsMessageDeselectStatus.SUCCESS : HsmsMessageDeselectStatus.NO_SELECTED;
						asyncChannel.sendDeselectResponse(msg, status);
						responsed = true;
						break;
					}
				}
				
				if ( ! responsed ) {
					asyncChannel.sendDeselectResponse(msg, HsmsMessageDeselectStatus.FAILED);
				}
				break;
			}
			case LINKTEST_REQ: {
				
				asyncChannel.sendLinktestResponse(msg);
				break;
			}
			case SEPARATE_REQ: {
				
				final int id = msg.sessionId();
				
				for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
					if ( s.sessionId() == id ) {
						s.unsetAsyncSocketChannel();
						break;
					}
				}
				break;
			}
			case SELECT_RSP:
			case DESELECT_RSP:
			case LINKTEST_RSP:
			case REJECT_REQ: {
				
				asyncChannel.sendRejectRequest(msg, HsmsMessageRejectReason.TRANSACTION_NOT_OPEN);
				break;
			}
			default: {
				if ( HsmsMessageType.supportSType(msg) ) {
					
					if ( ! HsmsMessageType.supportPType(msg) ) {
						asyncChannel.sendRejectRequest(msg, HsmsMessageRejectReason.NOT_SUPPORT_TYPE_P);
					}
					
				} else {
					
					asyncChannel.sendRejectRequest(msg, HsmsMessageRejectReason.NOT_SUPPORT_TYPE_S);
				}
			}
			}
		}
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, int strm, int func, boolean wbit)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(strm, func, wbit);
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, int strm, int func, boolean wbit, Secs2 secs2)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(strm, func, wbit, secs2);
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, SecsMessage primaryMsg, int strm, int func, boolean wbit)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(primaryMsg, strm, func, wbit);
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, SecsMessage primaryMsg, int strm, int func, boolean wbit, Secs2 secs2)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(primaryMsg, strm, func, wbit, secs2);
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, SmlMessage sml)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(sml);
	}
	
	@Override
	public Optional<SecsMessage> send(int sessionId, SecsMessage primaryMsg, SmlMessage sml)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException, InterruptedException {
		
		return getSession(sessionId).send(primaryMsg, sml);
	}
	
	@Override
	public boolean addSecsMessageReceiveListener(SecsMessageReceiveListener listener) {
		return this.hsmsMsgRecvBiObserver.addListener(listener);
	}
	
	@Override
	public boolean removeSecsMessageReceiveListener(SecsMessageReceiveListener listener) {
		return this.hsmsMsgRecvBiObserver.removeListener(listener);
	}
	
	@Override
	public boolean addSecsMessageReceiveBiListener(SecsMessageReceiveBiListener biListener) {
		return this.hsmsMsgRecvBiObserver.addBiListener(biListener);
	}
	
	@Override
	public boolean removeSecsMessageReceiveBiListener(SecsMessageReceiveBiListener biListener) {
		return this.hsmsMsgRecvBiObserver.removeBiListener(biListener);
	}
	
	@Override
	public boolean addHsmsMessageReceiveListener(HsmsMessageReceiveListener listener) {
		return this.hsmsMsgRecvBiObserver.addListener(listener);
	}
	
	@Override
	public boolean removeHsmsMessageReceiveListener(HsmsMessageReceiveListener listener) {
		return this.hsmsMsgRecvBiObserver.removeListener(listener);
	}
	
	@Override
	public boolean addHsmsMessageReceiveBiListener(HsmsSessionMessageReceiveBiListener biListener) {
		return this.hsmsMsgRecvBiObserver.addBiListener(biListener);
	}
	
	@Override
	public boolean removeHsmsMessageReceiveBiListener(HsmsSessionMessageReceiveBiListener biListener) {
		return this.hsmsMsgRecvBiObserver.removeBiListener(biListener);
	}
	
	
	@Override
	public boolean addSecsCommunicatableStateChangeBiListener(SecsCommunicatableStateChangeBiListener biListener) {
		return this.secsCommStatableObserver.addListener(biListener);
	}
	
	@Override
	public boolean removeSecsCommunicatableStateChangeBiListener(SecsCommunicatableStateChangeBiListener biListener) {
		return this.secsCommStatableObserver.removeListener(biListener);
	}
	
	@Override
	public boolean addHsmsCommunicateStateChangeBiListener(HsmsSessionCommunicateStateChangeBiListener biListener) {
		return this.hsmsCommStateObserver.addListener(biListener);
	}
	
	@Override
	public boolean removeHsmsCommunicateStateChangeBiListener(HsmsSessionCommunicateStateChangeBiListener biListener) {
		return this.hsmsCommStateObserver.removeListener(biListener);
	}
	
	protected AbstractHsmsAsyncSocketChannel buildAsyncSocketChannel(AsynchronousSocketChannel channel) {
		return new AbstractHsmsGsAsyncSocketChannel(channel, this) {};
	}
	
	
	@Override
	public boolean addSecsLogListener(SecsLogListener lstnr) {
		return this.logQueueObserver.addListener(lstnr);
	}
	
	@Override
	public boolean removeSecsLogListener(SecsLogListener lstnr) {
		return this.logQueueObserver.removeListener(lstnr);
	}
	
	public void notifyLog(AbstractSecsLog log) throws InterruptedException {
		
		log.subjectHeader(this.config.logSubjectHeader().toString());
		
		this.logQueueObserver.put(log);
		
		final AbstractSecsMessage msg = log.optionalAbstractSecsMessage().orElse(null);
		if ( msg != null ) {
			for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
				if ( s.sessionId() == msg.sessionId() ) {
					s.notifyLog(log);
				}
			}
		}
	}
	
	public void notifyLog(Throwable t) throws InterruptedException {
		
		this.notifyLog(new AbstractSecsThrowableLog(t) {

			private static final long serialVersionUID = -6535535244608063298L;
		});
	}
	
	
	/* Try-Send HSMS Message Pass Through */
	
	@Override
	public boolean addTrySendHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.trySendHsmsMsgPassThroughQueueObserver.addListener(listener);
	}
	
	@Override
	public boolean removeTrySendHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.trySendHsmsMsgPassThroughQueueObserver.removeListener(listener);
	}
	
	@Override
	public boolean addTrySendHsmsMessagePassThroughBiListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.trySendHsmsMsgPassThroughBiObserver.addBiListener(biListener);
	}
	
	@Override
	public boolean removeTrySendHsmsMessagePassThroughBiListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.trySendHsmsMsgPassThroughBiObserver.removeBiListener(biListener);
	}
	
	public void notifyTrySendHsmsMessagePassThrough(HsmsMessage msg) throws InterruptedException {
		this.trySendHsmsMsgPassThroughQueueObserver.put(msg);
		for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
			if ( msg.sessionId() == s.sessionId() ) {
				s.notifyTrySendHsmsMessagePassThrough(msg);
			}
		}
	}
	
	
	/* Sended HSMS Message Pass Through */
	
	@Override
	public boolean addSendedHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.sendedHsmsMsgPassThroughQueueObserver.addListener(listener);
	}
	
	@Override
	public boolean removeSendedHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.sendedHsmsMsgPassThroughQueueObserver.removeListener(listener);
	}
	
	@Override
	public boolean addSendedHsmsMessagePassThroughBiListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.sendedHsmsMsgPassThroughBiObserver.addBiListener(biListener);
	}
	
	@Override
	public boolean removeSendedHsmsMessagePassThroughBiListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.sendedHsmsMsgPassThroughBiObserver.removeBiListener(biListener);
	}
	
	public void notifySendedHsmsMessagePassThrough(HsmsMessage msg) throws InterruptedException {
		this.sendedHsmsMsgPassThroughQueueObserver.put(msg);
		for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
			if ( msg.sessionId() == s.sessionId() ) {
				s.notifySendedHsmsMessagePassThrough(msg);
			}
		}
	}
	
	
	/* Receive HSMS Message Pass Through */
	
	@Override
	public boolean addReceiveHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.recvHsmsMsgPassThroughQueueObserver.addListener(listener);
	}
	
	@Override
	public boolean removeReceiveHsmsMessagePassThroughListener(HsmsMessagePassThroughListener listener) {
		return this.recvHsmsMsgPassThroughQueueObserver.removeListener(listener);
	}
	
	@Override
	public boolean addReceiveHsmsMessagePassThroughBiListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.recvHsmsMsgPassThroughBiObserver.addBiListener(biListener);
	}
	
	@Override
	public boolean removeReceiveHsmsMessagePassThroughListener(HsmsSessionMessagePassThroughBiListener biListener) {
		return this.recvHsmsMsgPassThroughBiObserver.removeBiListener(biListener);
	}
	
	public void notifyReceiveHsmsMessagePassThrough(HsmsMessage msg) throws InterruptedException {
		this.recvHsmsMsgPassThroughQueueObserver.put(msg);
		for ( AbstractHsmsSession s : this.getAbstractHsmsSessions() ) {
			if ( msg.sessionId() == s.sessionId() ) {
				s.notifyReceiveHsmsMessagePassThrough(msg);
			}
		}
	}
	
}

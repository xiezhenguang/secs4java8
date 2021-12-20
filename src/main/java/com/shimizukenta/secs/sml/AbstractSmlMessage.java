package com.shimizukenta.secs.sml;

import java.io.Serializable;

import com.shimizukenta.secs.secs2.Secs2;

abstract public class AbstractSmlMessage implements SmlMessage, Serializable {
	
	private static final long serialVersionUID = 6167166233801516804L;
	
	private final int strm;
	private final int func;
	private final boolean wbit;
	private final Secs2 secs2;
	
	protected AbstractSmlMessage(int strm, int func, boolean wbit, Secs2 secs2) {
		this.strm = strm;
		this.func = func;
		this.wbit = wbit;
		this.secs2 = secs2;
	}
	
	@Override
	public int getStream() {
		return strm;
	}
	
	@Override
	public int getFunction() {
		return func;
	}
	
	@Override
	public boolean wbit() {
		return wbit;
	}
	
	@Override
	public Secs2 secs2() {
		return secs2;
	}
	
	protected static final String BR = System.lineSeparator();
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder("S")
				.append(strm)
				.append("F")
				.append(func);
		
		if (wbit) {
			sb.append(" W");
		}
		
		String body = secs2.toString();
		
		if ( ! body.isEmpty() ) {
			sb.append(BR).append(body);
		}
		
		return sb.append(".").toString();
	}
}
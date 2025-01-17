package com.shimizukenta.secs.secs2.impl;

import java.math.BigInteger;
import java.util.List;

import com.shimizukenta.secs.secs2.Secs2;
import com.shimizukenta.secs.secs2.Secs2Builder;

public class AbstractSecs2Builder implements Secs2Builder {
	
	public AbstractSecs2Builder() {
		/* Nothing */
	}
	
	private static Secs2RawBytes rawEmpty = new Secs2RawBytes();
	private static Secs2List listEmpty = new Secs2List();
	private static Secs2Int4 i4empty = new Secs2Int4();
	private static Secs2Int8 i8empty = new Secs2Int8();
	private static Secs2Uint4 u4empty = new Secs2Uint4();
	private static Secs2Uint8 u8empty = new Secs2Uint8();
	
	@Override
	public Secs2 empty() {
		return rawEmpty;
	}
	
	@Override
	public Secs2 raw(byte[] bs) {
		return new Secs2RawBytes(bs);
	}
	
	@Override
	public Secs2 list() {
		return listEmpty;
	}
	
	@Override
	public Secs2 list(Secs2... values) {
		return new Secs2List(values);
	}
	
	@Override
	public Secs2 list(List<? extends Secs2> values) {
		return new Secs2List(values);
	}
	
	@Override
	public Secs2 ascii(CharSequence cs) {
		return new Secs2Ascii(cs);
	}
	
	@Override
	public Secs2 binary() {
		return new Secs2Binary();
	}
	
	@Override
	public Secs2 binary(byte... bs) {
		return new Secs2Binary(bs);
	}
	
	@Override
	public Secs2 binary(List<Byte> bs) {
		return new Secs2Binary(bs);
	}
	
	@Override
	public Secs2 bool() {
		return new Secs2Boolean();
	}
	
	@Override
	public Secs2 bool(boolean... bools) {
		return new Secs2Boolean(bools);
	}
	
	@Override
	public Secs2 bool(List<Boolean> bools) {
		return new Secs2Boolean(bools);
	}
	
	@Override
	public Secs2 int1() {
		return new Secs2Int1();
	}
	
	@Override
	public Secs2 int1(int... values) {
		return new Secs2Int1(values);
	}
	
	@Override
	public Secs2 int1(long... values) {
		return new Secs2Int1(values);
	}
	
	@Override
	public Secs2 int1(BigInteger... values) {
		return new Secs2Int1(values);
	}

	@Override
	public Secs2 int1(List<? extends Number> values) {
		return new Secs2Int1(values);
	}
	
	@Override
	public Secs2 int2() {
		return new Secs2Int2();
	}
	
	@Override
	public Secs2 int2(int... values) {
		return new Secs2Int2(values);
	}
	
	@Override
	public Secs2 int2(long... values) {
		return new Secs2Int2(values);
	}
	
	@Override
	public Secs2 int2(BigInteger... values) {
		return new Secs2Int2(values);
	}

	@Override
	public Secs2 int2(List<? extends Number> values) {
		return new Secs2Int2(values);
	}
	
	@Override
	public Secs2 int4() {
		return i4empty;
	}
	
	@Override
	public Secs2 int4(int... values) {
		return new Secs2Int4(values);
	}
	
	@Override
	public Secs2 int4(long... values) {
		return new Secs2Int4(values);
	}
	
	@Override
	public Secs2 int4(BigInteger... values) {
		return new Secs2Int4(values);
	}

	@Override
	public Secs2 int4(List<? extends Number> values) {
		return new Secs2Int4(values);
	}
	
	@Override
	public Secs2 int8() {
		return i8empty;
	}
	
	@Override
	public Secs2 int8(int... values) {
		return new Secs2Int8(values);
	}
	
	@Override
	public Secs2 int8(long... values) {
		return new Secs2Int8(values);
	}
	
	@Override
	public Secs2 int8(BigInteger... values) {
		return new Secs2Int8(values);
	}

	@Override
	public Secs2 int8(List<? extends Number> values) {
		return new Secs2Int8(values);
	}
	
	@Override
	public Secs2 uint1() {
		return new Secs2Uint1();
	}
	
	@Override
	public Secs2 uint1(int... values) {
		return new Secs2Uint1(values);
	}
	
	@Override
	public Secs2 uint1(long... values) {
		return new Secs2Uint1(values);
	}
	
	@Override
	public Secs2 uint1(BigInteger... values) {
		return new Secs2Uint1(values);
	}

	@Override
	public Secs2 uint1(List<? extends Number> values) {
		return new Secs2Uint1(values);
	}

	@Override
	public Secs2 uint2() {
		return new Secs2Uint2();
	}
	
	@Override
	public Secs2 uint2(int... values) {
		return new Secs2Uint2(values);
	}
	
	@Override
	public Secs2 uint2(long... values) {
		return new Secs2Uint2(values);
	}
	
	@Override
	public Secs2 uint2(BigInteger... values) {
		return new Secs2Uint2(values);
	}

	@Override
	public Secs2 uint2(List<? extends Number> values) {
		return new Secs2Uint2(values);
	}

	@Override
	public Secs2 uint4() {
		return u4empty;
	}
	
	@Override
	public Secs2 uint4(int... values) {
		return new Secs2Uint4(values);
	}
	
	@Override
	public Secs2 uint4(long... values) {
		return new Secs2Uint4(values);
	}
	
	@Override
	public Secs2 uint4(BigInteger... values) {
		return new Secs2Uint4(values);
	}

	@Override
	public Secs2 uint4(List<? extends Number> values) {
		return new Secs2Uint4(values);
	}

	@Override
	public Secs2 uint8() {
		return u8empty;
	}
	
	@Override
	public Secs2 uint8(int... values) {
		return new Secs2Uint8(values);
	}
	
	@Override
	public Secs2 uint8(long... values) {
		return new Secs2Uint8(values);
	}
	
	@Override
	public Secs2 uint8(BigInteger... values) {
		return new Secs2Uint8(values);
	}

	@Override
	public Secs2 uint8(List<? extends Number> values) {
		return new Secs2Uint8(values);
	}
	
	@Override
	public Secs2 float4() {
		return new Secs2Float4();
	}
	
	@Override
	public Secs2 float4(float... values) {
		return new Secs2Float4(values);
	}
	
	@Override
	public Secs2 float4(List<? extends Number> values) {
		return new Secs2Float4(values);
	}
	
	@Override
	public Secs2 float8() {
		return new Secs2Float8();
	}
	
	@Override
	public Secs2 float8(double... values) {
		return new Secs2Float8(values);
	}
	
	@Override
	public Secs2 float8(List<? extends Number> values) {
		return new Secs2Float8(values);
	}
	
}

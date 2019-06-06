/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io;

import java.nio.ByteBuffer;

/**
 * The packet abstraction used for the primary I/O functions of {@link TunDevice}.
 *
 * <p>The main attribute for manipulation is the {@link #packet} {@link ByteBuffer}. Additionally, the class contains a
 * small set of utility functions. The uint* functions take the {@link ByteBuffer#order()} into account, which defaults
 * to the network byte order (big endian) when obtained from one of the {@link TunDevice#read()} methods. The utility
 * methods do not change (or save and restore) the {@link ByteBuffer#position()} of {@link #packet}.
 * </p>
 *
 * <p>There are no limitations on manipulating a Packet obtained from one of the {@link TunDevice#read()}
 * functions.</p>
 */
public final class Packet {
	/**
	 * The packet and the main attribute for manipulation before sending. When obtained by reading from {@link
	 * TunDevice}, network byte order (big endian) is active.
	 */
	public final ByteBuffer packet;

	public Packet(int capacity) {
		this.packet = ByteBuffer.allocate(capacity);
	}

	public Packet(ByteBuffer packet) {
		this.packet = packet;
	}

	public int uint8(int index) {
		return Byte.toUnsignedInt(packet.get(index));
	}

	public int uint16(int index) {
		return Short.toUnsignedInt(packet.getShort(index));
	}

	public long uint32(int index) {
		return Integer.toUnsignedLong(packet.getInt(index));
	}

	public Packet uint8(int index, int value) {
		packet.put(index, (byte) value);
		return this;
	}

	public Packet uint16(int index, int value) {
		packet.putShort(index, (short) value);
		return this;
	}

	public Packet uint32(int index, long value) {
		packet.putInt(index, (int) value);
		return this;
	}

	public byte[] bytes(int position, int length) {
		byte[] r = new byte[length];
		int orgPos = packet.position();
		packet.position(position);
		packet.get(r);
		packet.position(orgPos);
		return r;
	}

	public byte[] bytes() {
		return bytes(0, packet.limit());
	}

	public Packet bytes(int position, byte[] bytes, int offset, int length) {
		int orgPos = packet.position();
		packet.position(position);
		packet.put(bytes, offset, length);
		packet.position(orgPos);
		return this;
	}

	public Packet bytes(int position, byte[] bytes) {
		return bytes(position, bytes, 0, bytes.length);
	}

	public int size() {
		return packet.limit();
	}

	public int ipVersion() {
		return uint8(0) >> 4;
	}

	public boolean isIpv4() {
		return ipVersion() == 4;
	}

	public boolean isIpv6() {
		return ipVersion() == 6;
	}
}

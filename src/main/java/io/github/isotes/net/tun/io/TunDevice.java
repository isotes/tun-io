/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io;

import com.sun.jna.LastErrorException;
import com.sun.jna.NativeLong;
import io.github.isotes.net.tun.io.jna.Darwin;
import io.github.isotes.net.tun.io.jna.FdAndName;
import io.github.isotes.net.tun.io.jna.LibC;
import io.github.isotes.net.tun.io.jna.Linux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Open/create a TUN device on macOS and Linux.
 *
 * <p>While the class supports both Linux and macOS, there are limitations on the latter:</p>
 * <ul>
 *     <li>It is only possible to create new devices (in contrast to opening existing ones).</li>
 *     <li>Names must follow the pattern 'utun[NUMBER]' with 0 being already in use on macOS Sierra and newer.</li>
 * </ul>
 *
 * <p>The basic approach is opening/creating a TUN device with one of the open() methods of this class. After
 * configuring the interface, e.g., with ip/ifconfig, which is not covered by this library, it is possible to read/write
 * IPv4 and IPv6 packets using the appropriate methods. I/O is always performed on packet granularity.</p>
 *
 * <p>The {@link Packet} class is used for reading and can be used for writing (in addition to byte arrays). Apart from
 * the {@link ByteBuffer} representing the packet, it contains a number of utility methods for easier manipulation.</p>
 *
 * <p>On Linux, we recommend creating and configuring the TUN device with the <code>ip</code> command which allows setting
 * permissions so that the application using this library does not need to run with elevated privileges.</p>
 *
 * <p>See <a href="https://github.com/isotes/tun-io-example" target="_top">tun-io-example</a> for a full-fledged
 * example.</p>
 */
public class TunDevice implements AutoCloseable {
	private static final int DEFAULT_MTU = 2048;
	private static final byte[] IPV4_HEADER_DARWIN = new byte[]{0, 0, 0, 2};  // AF_INET in socket.h
	private static final byte[] IPV6_HEADER_DARWIN = new byte[]{0, 0, 0, 30};  // AF_INET6 in socket.h
	/* package */ final int fd;
	private final String name;
	/* package */ NativeLong readMtu = new NativeLong(DEFAULT_MTU);

	/* package */ TunDevice(String name, int fd) {
		this.name = name;
		this.fd = fd;
	}

	/**
	 * Create a new TUN device with the name automatically chosen by the OS. See {@link #open(String)} for more
	 * information.
	 *
	 * @return the new tun device
	 * @throws IOException if one of the system call fails
	 */
	public static TunDevice open() throws IOException {
		return open(null);
	}

	/**
	 * Create/open a TUN device name 'utun[NUMBER]'. For compatibility with macOS Sierra and newer, the number should be
	 * greater than 0. See {@link #open(String) } for more information.
	 *
	 * @param number the number
	 * @return the open tun device
	 * @throws IOException if one of the system call fails
	 */
	public static TunDevice open(int number) throws IOException {
		return open("utun" + number);
	}

	/**
	 * Create/open a TUN device with the specified name.
	 *
	 * <p>On Linux, name can be a simple file name (ASCII) of up to 16 characters. </p>
	 *
	 * <p>On macOS, it is only possible to create new devices and the name must be of the form 'utun[N]', with N
	 * being a number starting at 0 (e.g., utun12). However, starting from macOS Sierra, utun0 is always created by the
	 * system and may not be used by other programs. Thus, using a number of 1 or higher is strongly recommended. </p>
	 *
	 * @param name the name of the device or null if the name should be automatically chosen by the OS
	 * @return the open tun device
	 * @throws IOException if one of the system call fails
	 */
	public static TunDevice open(String name) throws IOException {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("mac")) {
				FdAndName fdAndName = Darwin.open(name);
				return new TunDeviceWithHeader(fdAndName.name, fdAndName.fd, IPV4_HEADER_DARWIN, IPV6_HEADER_DARWIN);
			} else {
				FdAndName fdAndName = Linux.open(name);
				return new TunDevice(fdAndName.name, fdAndName.fd);
			}
		} catch (LastErrorException ex) {
			throw new IOException("Error opening TUN device: " + ex.getMessage(), ex);
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public void close() throws IOException {
		try {
			LibC.close(fd);
		} catch (LastErrorException ex) {
			throw new IOException("Error closing TUN device: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Set the MTU used for the buffers in the read() methods
	 *
	 * @param readMtu the new buffer size when calling the read() methods
	 * @return the TunDevice
	 */
	public TunDevice setReadMtu(int readMtu) {
		this.readMtu = new NativeLong(readMtu);
		return this;
	}

	protected Packet read(int limitIpVersion) throws IOException {
		try {
			while (true) {
				ByteBuffer inbuf = ByteBuffer.allocate(readMtu.intValue());
				int n = LibC.read(fd, inbuf, readMtu);
				int version = Byte.toUnsignedInt(inbuf.get(0)) >> 4;
				if (version != limitIpVersion && limitIpVersion != 0) {
					continue;
				}
				inbuf.order(ByteOrder.BIG_ENDIAN);
				inbuf.limit(n);
				return new Packet(inbuf);
			}
		} catch (LastErrorException ex) {
			throw new IOException("Reading from TUN device " + getName() + " failed: " + ex.getMessage(), ex);
		}
	}

	public Packet read() throws IOException {
		return read(0);
	}

	public Packet readIPv4Packet() throws IOException {
		return read(4);
	}

	public Packet readIPv6Packet() throws IOException {
		return read(6);
	}

	public TunDevice write(Packet packet) throws IOException {
		return write(packet.packet);
	}

	public TunDevice write(ByteBuffer packet) throws IOException {
		try {
			LibC.write(fd, packet, new NativeLong(packet.remaining()));
		} catch (LastErrorException ex) {
			throw new IOException("Writing to TUN device " + getName() + " failed: " + ex.getMessage(), ex);
		}
		return this;
	}

	public TunDevice write(byte[] packet) throws IOException {
		try {
			LibC.write(fd, packet, new NativeLong(packet.length));
		} catch (LastErrorException ex) {
			throw new IOException("Writing to TUN device " + getName() + " failed: " + ex.getMessage(), ex);
		}
		return this;
	}

	public Packet newPacket(int capacity) {
		return new Packet(ByteBuffer.allocate(capacity));
	}

	private static class TunDeviceWithHeader extends TunDevice {
		private final byte[] ipv4Header;
		private final byte[] ipv6Header;
		private final int headerSize;


		/* package */ TunDeviceWithHeader(String name, int fd, byte[] ipv4Header, byte[] ipv6Header) {
			super(name, fd);
			this.ipv4Header = ipv4Header;
			this.ipv6Header = ipv6Header;
			this.headerSize = ipv4Header.length;
		}

		@Override
		protected Packet read(int limitIpVersion) throws IOException {
			try {
				while (true) {
					ByteBuffer inbuf = ByteBuffer.allocate(headerSize + readMtu.intValue());
					int n = LibC.read(fd, inbuf, readMtu);
					int version = Byte.toUnsignedInt(inbuf.get(headerSize)) >> 4;
					if (version != limitIpVersion && limitIpVersion != 0) {
						continue;
					}
					// slice without the header but with the full capacity allowing the later use of the complete buffer
					inbuf.position(headerSize);
					ByteBuffer packetBuf = inbuf.slice();
					packetBuf.order(ByteOrder.BIG_ENDIAN);  // default to network byte order
					packetBuf.limit(n - headerSize);
					return new Packet(packetBuf);
				}
			} catch (LastErrorException ex) {
				throw new IOException("Reading from TUN device " + getName() + " failed: " + ex.getMessage(), ex);
			}
		}

		@Override
		public TunDevice write(ByteBuffer packet) throws IOException {
			byte[] bytes = new byte[headerSize + packet.remaining()];
			byte[] header = Byte.toUnsignedInt(packet.get(0)) >> 4 == 6 ? ipv6Header : ipv4Header;
			System.arraycopy(header, 0, bytes, 0, header.length);
			packet.slice().get(bytes, headerSize, packet.remaining());
			return super.write(bytes);
		}

		@Override
		public TunDevice write(byte[] packet) throws IOException {
			byte[] bytes = new byte[headerSize + packet.length];
			byte[] header = Byte.toUnsignedInt(packet[0]) >> 4 == 6 ? ipv6Header : ipv4Header;
			System.arraycopy(header, 0, bytes, 0, header.length);
			System.arraycopy(packet, 0, bytes, headerSize, packet.length);
			return super.write(bytes);
		}

	}
}

/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io.jna;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.nio.charset.StandardCharsets;

/**
 * Linux implementation for opening/creating a TUN device based using the
 * <a href="https://www.kernel.org/doc/Documentation/networking/tuntap.txt">TUN/TAP</a> interface provided by the
 * kernel.
 */
public class Linux {
	private static final NativeLong TUNSETIFF = new NativeLong(0x400454caL);  // from if_tun.h
	private static final int O_RDWR = 2;  // from fcntl-linux.h

	public static FdAndName open(String name) {
		if (name != null && name.isEmpty()) {
			name = null;
		}
		if (name != null) {
			if (!StandardCharsets.US_ASCII.newEncoder().canEncode(name)) {
				throw new IllegalArgumentException("'Name' must be an ASCII string (or null)");
			}
			if (name.length() >= 16) {
				throw new IllegalArgumentException("'Name' must be shorter than 16 characters");
			}
		}

		int fd = LibC.open("/dev/net/tun", O_RDWR);
		IfReq ifReq = new IfReq(name, (short) (IfReq.FLAGS_IFF_TUN | IfReq.FLAGS_IFF_NO_PI));
		LibC.ioctl(fd, TUNSETIFF, ifReq);
		return new FdAndName(fd, Native.toString(ifReq.name, StandardCharsets.US_ASCII));
	}

	/**
	 * Models struct ifreq in if.h
	 */
	@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
	@Structure.FieldOrder({"name", "flags", "padding"})
	public static class IfReq extends Structure {
		public static final int LENGTH = 0x28;
		public static final short FLAGS_IFF_TUN = 0x0001;  // from if_tun.h
		public static final short FLAGS_IFF_NO_PI = 0x1000;  // from if_tun.h

		public byte[] name = new byte[0x10];
		public short flags;
		@SuppressWarnings("unused")
		public byte[] padding = new byte[LENGTH - 0x10 - 2];

		IfReq(String name, short flags) {
			if (name != null) {
				byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
				System.arraycopy(bytes, 0, this.name, 0, bytes.length);
			}
			this.flags = flags;
		}
	}
}

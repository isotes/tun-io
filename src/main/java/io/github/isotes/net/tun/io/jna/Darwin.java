/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.nio.charset.StandardCharsets;

/**
 * macOS implementation for creating a TUN device using the
 * <a href="https://github.com/apple/darwin-xnu/blob/master/bsd/net/if_utun.h">utun kernel control</a>) interface
 */
public class Darwin {
	private static final int SYSPROTO_CONTROL = 2;
	private static final int AF_SYSTEM = 32;
	private static final int SOCK_DGRAM = 2;
	private static final String UTUN_CONTROL_NAME = "com.apple.net.utun_control";  // from if_utun.h
	private static final int UTUN_OPT_IFNAME = 2;  // from if_utun.h
	private static final NativeLong CTLIOCGINFO = new NativeLong(0xC0644E03L);  // from kern_control.h

	public static FdAndName open(String name) {
		int number = 0;  // default: name automatically chosen by OS
		if (name != null && name.isEmpty()) {
			name = null;
		}
		if (name != null) {
			if (!name.startsWith("utun")) {
				throw new IllegalArgumentException("Parameter 'name' must start with 'utun' on macOS (or be null)");
			}
			try {
				number = Integer.valueOf(name.substring(4)) + 1;  // 1 -> utun0, ...
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Parameter 'name' must be 'utun<Number>' on macOS");
			}
		}

		int fd = LibC.socket(AF_SYSTEM, SOCK_DGRAM, SYSPROTO_CONTROL);
		CtlInfo ctlInfo = new CtlInfo(UTUN_CONTROL_NAME);
		LibC.ioctl(fd, CTLIOCGINFO, ctlInfo);
		SockAddress sockAddress = new SockAddress(AF_SYSTEM, (short) SYSPROTO_CONTROL, ctlInfo.id, number);
		LibC.connect(fd, sockAddress, sockAddress.len);
		SockName sockName = new SockName();
		IntByReference sockNameLen = new IntByReference(SockName.LENGTH);
		LibC.getsockopt(fd, SYSPROTO_CONTROL, UTUN_OPT_IFNAME, sockName, sockNameLen);
		return new FdAndName(fd, new String(sockName.name, 0, sockNameLen.getValue() - 1, StandardCharsets.US_ASCII));
	}

	@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
	@Structure.FieldOrder({"name"})
	public static class SockName extends Structure {
		public static final int LENGTH = 16;
		public byte[] name = new byte[16];
	}

	@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
	@Structure.FieldOrder({"len", "family", "sysaddr", "reserved"})
	public static class SockAddress extends Structure {
		public byte len = 1 + 1 + 2 + (1 + 1 + 5) * 4;
		public byte family;
		public short sysaddr;
		public int[] reserved = new int[7];

		public SockAddress(int addressFamily, short sysaddr, int... reserved) {
			this.family = (byte) addressFamily;
			this.sysaddr = sysaddr;
			System.arraycopy(reserved, 0, this.reserved, 0, reserved.length);
		}
	}

	@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
	@Structure.FieldOrder({"id", "name"})
	public static class CtlInfo extends Structure {
		public int id;
		public byte[] name = new byte[96];

		public CtlInfo(String name) {
			byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(bytes, 0, this.name, 0, bytes.length);
		}
	}
}

/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io.jna;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

import java.nio.ByteBuffer;

public class LibC {

	static {
		Native.register(Platform.C_LIBRARY_NAME);
	}

	public static native int open(String pathname, int flags) throws LastErrorException;

	public static native int close(int fd) throws LastErrorException;

	public static native int read(int fd, byte[] data, NativeLong len) throws LastErrorException;

	public static native int read(int fd, ByteBuffer data, NativeLong len) throws LastErrorException;

	public static native int write(int fd, byte[] data, NativeLong len) throws LastErrorException;

	public static native int write(int fd, ByteBuffer data, NativeLong len) throws LastErrorException;

	public static native int socket(int domain, int type, int protocol) throws LastErrorException;

	public static native int connect(int sockfd, Structure address, int addrlen) throws LastErrorException;

	public static native int getsockopt(int sockfd, int level, int optname, Structure opt, IntByReference optlen) throws LastErrorException;

	public static native int ioctl(int fd, NativeLong cmd, Structure p) throws LastErrorException;
}

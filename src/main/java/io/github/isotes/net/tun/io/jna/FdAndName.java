/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.isotes.net.tun.io.jna;

public class FdAndName {
	public final int fd;
	public final String name;

	public FdAndName(int fd, String name) {
		this.fd = fd;
		this.name = name;
	}
}

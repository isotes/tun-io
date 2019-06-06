/*
 * Copyright (c) 2019 Robert Sauter
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * JNA based access to TUN devices on Linux and macOS.
 *
 * <p>On Linux, the library uses the
 * <a href="https://www.kernel.org/doc/Documentation/networking/tuntap.txt" target="_top">TUN/TAP</a>
 * interface provided by the kernel. On macOS, the library uses the
 * <a href="https://github.com/apple/darwin-xnu/blob/master/bsd/net/if_utun.h" target="_top">utun kernel control</a>
 * interface.</p>
 *
 * <p>Apart from JNA, the library has no dependencies and has been successfully tested on macOS, Linux on a PC (Ubuntu
 * 18.04 64-bit), and Linux on a Raspberry PI 3 with Raspbian 9 (based on Debian Stretch).</p>
 *
 * <p>See {@link io.github.isotes.net.tun.io.TunDevice} for more information.</p>
 */
package io.github.isotes.net.tun.io;

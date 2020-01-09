# tun-io
JNA based access to TUN devices on Linux and macOS.

## Use
See [Maven Central](https://search.maven.org/search?q=g:io.github.isotes%20a:tun-io) for the current Maven coordinates. Additionally, you have to declare a dependency on [JNA](https://search.maven.org/search?q=g:net.java.dev.jna%20a:jna). The library requires Java 8 but should also work with newer versions.

## API
The main entry point is the [TunDevice](src/main/java/io/github/isotes/net/tun/io/TunDevice.java) class which contains several `open()` methods to open or create a TUN device. Afterwards, it is possible to read/write IPv4 and IPv6 packets. The following shows a minimal code snippet while a full-fledged example can be found in the [tun-io-example project](https://github.com/isotes/tun-io-example) and more information is in the [API documentation](https://isotes.github.io/javadoc/tun-io-1.0.0/).


```Java
public static void main(String[] args) {
    try (TunDevice tun = TunDevice.open()) {
        System.out.println("Created tun device " + tun.getName());
        // device configuration (address, ...) required
        while (true) {
            Packet packet = tun.read();
            System.out.printf("IPv%d packet with %d bytes\n", packet.ipVersion(), packet.size());
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```


## TUN Device Configuration
The configuration of the TUN device is not covered by this library. The [tun-io-example project](https://github.com/isotes/tun-io-example) makes use of the `ip`/`ifconfig` command line utilities provided by Linux and macOS respectively and can be used as inspiration.


## Portability
The library supports only TUN (vs. TAP) interfaces to be compatible with macOS. Nevertheless, there are a couple of further limitations on macOS:
 - It is only possible to create new devices (in contrast to opening existing ones).
 - Names must follow the pattern 'utun[NUMBER]' with 0 being already in use on macOS Sierra and newer.

On Linux, the library uses the [TUN/TAP](https://www.kernel.org/doc/Documentation/networking/tuntap.txt) interface provided by the kernel. On macOS, the library uses the [utun kernel control](https://github.com/apple/darwin-xnu/blob/master/bsd/net/if_utun.h) interface.

The only dependency is [JNA](https://github.com/java-native-access/jna) and the library has been successfully tested on macOS (10.14), Linux on a PC (Ubuntu 18.04 64-bit), and Linux on a Raspberry PI 3 with Raspbian 9 (based on Debian Stretch).


## Development
The library is a Gradle project. The development is usually performed in combination with the tun-io-example project and using the [Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html) feature (in conjunction with IntelliJ) has worked flawlessly. Just edit `settings.gradle` of the example project and add the line `includeBuild '../tun-io'` (adjust the path if necessary). Then open that project in the IDE to work on this library and the example project simultaneously.


## License
[Apache 2.0](LICENSE)

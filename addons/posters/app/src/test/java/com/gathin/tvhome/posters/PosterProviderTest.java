package com.gathin.tvhome.posters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.Test;

public class PosterProviderTest {
    private static boolean isPublic(String ip) throws Exception {
        return PosterProvider.isPublic(InetAddress.getByName(ip));
    }

    @Test
    public void publicAddressesAreFetched() throws Exception {
        assertTrue(isPublic("151.101.1.1"));
        assertTrue(isPublic("2606:4700::1111"));
    }

    @Test
    public void homeNetworkAndLoopbackAreRefused() throws Exception {
        assertFalse(isPublic("10.0.4.97"));
        assertFalse(isPublic("192.168.1.1"));
        assertFalse(isPublic("172.16.0.1"));
        assertFalse(isPublic("127.0.0.1"));
        assertFalse(isPublic("169.254.1.1"));
        assertFalse(isPublic("0.0.0.0"));
        assertFalse(isPublic("::1"));
        assertFalse(isPublic("fe80::1"));
        assertFalse(isPublic("fd12:3456::1"));
    }

    @Test
    public void carrierNatRangeIsRefused() throws Exception {
        assertFalse(isPublic("100.64.0.1"));
        assertFalse(isPublic("100.127.255.254"));
        assertTrue(isPublic("100.128.0.1"));
    }
}

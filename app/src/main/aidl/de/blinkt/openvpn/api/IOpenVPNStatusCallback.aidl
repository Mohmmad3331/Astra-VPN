// From the OpenVPN for Android project (schwabe/ics-openvpn), External OpenVPN API.
// This AIDL file is exempted from the GPLv2 for exactly this use (third-party clients).
package de.blinkt.openvpn.api;

interface IOpenVPNStatusCallback {
    oneway void newStatus(in String uuid, in String state, in String message, in String level);
}

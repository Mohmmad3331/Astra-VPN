// From the OpenVPN for Android project (schwabe/ics-openvpn), External OpenVPN API.
// This AIDL file is exempted from the GPLv2 for exactly this use (third-party clients).
package de.blinkt.openvpn.api;

import de.blinkt.openvpn.api.APIVpnProfile;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

import android.content.Intent;
import android.os.ParcelFileDescriptor;

interface IOpenVPNAPIService {
    List<APIVpnProfile> getProfiles();

    void startProfile(String profileUUID);

    boolean addVPNProfile(String name, String config);

    void startVPN(in String inlineconfig);

    Intent prepare(in String packagename);

    Intent prepareVPNService();

    void disconnect();

    void pause();

    void resume();

    void registerStatusCallback(in IOpenVPNStatusCallback cb);

    void unregisterStatusCallback(in IOpenVPNStatusCallback cb);

    void removeProfile(in String profileUUID);

    boolean protectSocket(in ParcelFileDescriptor fd);

    APIVpnProfile addNewVPNProfile(String name, boolean userEditable, String config);

    void startVPNwithExtras(in String inlineconfig, in Bundle extras);

    APIVpnProfile addNewVPNProfileWithExtras(String name, boolean userEditable, String config, in Bundle extras);

    @nullable APIVpnProfile getDefaultProfile();

    void setDefaultProfile(String profileUUID);
}

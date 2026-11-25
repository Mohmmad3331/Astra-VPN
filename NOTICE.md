# Third-party notices

## OpenVPN for Android (ics-openvpn) — External API

The following files are copied from the [ics-openvpn](https://github.com/schwabe/ics-openvpn)
project by Arne Schwabe, specifically its "External OpenVPN API", which the
project publishes for other apps to copy and use:

- `app/src/main/aidl/de/blinkt/openvpn/api/IOpenVPNAPIService.aidl`
- `app/src/main/aidl/de/blinkt/openvpn/api/IOpenVPNStatusCallback.aidl`
- `app/src/main/aidl/de/blinkt/openvpn/api/APIVpnProfile.aidl`
- `app/src/main/java/de/blinkt/openvpn/api/APIVpnProfile.java`

Per the project's own source comment on `APIVpnProfile.java`:

> This file is used for implementing the external API and this file like
> the AIDL and is exempted from the GPLv2.

The rest of ics-openvpn (the actual OpenVPN engine) is NOT embedded in this
app. Astra VPN talks to a separately installed copy of the "OpenVPN for
Android" app over this API — see `README.md`.

Reference: https://github.com/schwabe/ics-openvpn/blob/master/doc/API.md

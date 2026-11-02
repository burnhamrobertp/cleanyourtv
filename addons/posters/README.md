# TV Posters

Real artwork on the launcher's Continue watching row (`continue_watching`). Netflix, Peacock and Prime Video give
`https://` posters, and the launcher has no internet access, so without this each card shows the app's banner.

The launcher asks this app for each poster on the row (ten at most); it downloads each one once and keeps the
last 40 on the TV. It has no screen and nothing to set up. The contract is in
[`docs/modules.md`](../../docs/modules.md#posters-for-the-continue-watching-row-comgathintvhomeposters).

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Downloads the posters the row asks for, over `https://`, from the streaming apps' image servers. Nothing else goes online. |

It answers only apps signed with its own key, which means the launcher when both are built the same way (both
in Docker, or both natively). It refuses `http://`, addresses on the home network, anything that is not an
image, and anything over 4 MB. No background service, nothing at boot, no telemetry.

## Build and install

Build it the same way as the launcher, so both carry the same key, then install it like any add-on
([`addons/README.md`](../README.md)). Press Home and the row's posters load. Uninstall it and the row goes
back to banners.

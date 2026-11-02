# Security policy

Thank you for helping keep NaviCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/NaviCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of NaviCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

- Your password, sign-in tokens or other data leaking to anyone other than your own Navidrome server, for example in addresses shared with other apps, the media notification, error messages or redirects
- Ways around the app lock, or protected screens showing up in screenshots when they shouldn't
- Another app on the phone being able to make NaviCtrl do something you didn't ask for, such as controlling playback or browsing your library through the music service
- Anything that lets someone run code or read files they shouldn't

Problems in Navidrome itself should go to that project.

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
20:78:6F:26:52:F1:D3:27:F8:FC:C1:8B:3D:D5:43:06:6F:AD:33:34:90:E2:3B:FE:00:34:93:09:A1:10:FE:FC
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.

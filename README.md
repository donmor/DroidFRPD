# DroidFRPD
This is an Android FRP daemon manager based on [fatedier/frp](https://github.com/fatedier/frp) project.

## Configurations
#### Manage configuration files
Aside of internal configuration editor, the configuration files can be managed via Files app or SAF-compliant apps.
#### Start daemon on device boot
You may have to grant start on boot permission manually.
#### Ignore battery optimization
There's an option in Settings that jumps to system battery optimization setting page.
#### Remove activity from recents
If enabled, DroidFRPD will be removed from Recent Activities, preventing it from being killed unexpectedly.

## Build cores from source
It's necessary to build core executables from source if:
- Using domain name as `serverAddr`
- Using on Android 16+ amd64 devices (which requires 16KiB memory page size)

Perform the following steps to build core executables from source:
- A Linux / macOS machine is recommended
- Clone this repository with its submodule (using `--recursive`)
- Install Android NDK and Go
- Setup `ccArm64` and `ccAmd64` in `local.properties` (see `local.example.properties`)
- Set `frp.buildExecutables = true` in `local.properties`
- Now any Android build operations will trigger `go build` as well


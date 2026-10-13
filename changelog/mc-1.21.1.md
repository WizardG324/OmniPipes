## v1.1
- Added support for Powah to allow Energizing Rods to be placed on pipes
- Added fluid_upgrade_compat and energy_upgrade_compat item tags, so packs can make their own items work as type upgrades
- Pipe networks are now cached, and only rebuild when something in them changes
- Sides that keep failing now sleep longer each time, and wake early when something next to them changes
- Faster transfers, especially with large or full inventories
- Added the /omnipipes command (stats, networks, network <pos>) for admins and moderators to diagnose pipe performance
- Config: retriesBeforePausing and extractRecheckDelay were replaced by maxSleepTicks

## v1.0
- 1.21.1 port
- EMI standalone dragging support
- Dragging from REI works as intended

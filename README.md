# Turbo Server List

A simple Fabric mod that significantly decreases the server list first-load time by skipping the blocked servers list fetching

## How It Works

In vanilla Minecraft, when you launch the game and open the multiplayer menu for the first time, the client cannot immediately start pinging or connecting to servers.
Instead, it first fetches the blocked servers list from `https://sessionserver.mojang.com/blockedservers` to validate server addresses.
Only after this request completes does Minecraft begin establishing connections.

Although the list is fetched only once per game launch, players who have poor connectivity to Mojang's servers may experience delays 
of seconds or even minutes while waiting for the blocked list to load

This mod's implementation is straightforward: it simply eliminates the blocked servers list fetch,
allowing the multiplayer menu to open instantly with smooth server list refreshing and lightning-fast pings/joins

## Requirements

It's a client-side-only mod that only requires fabric loader >= 0.10.4. No other dependencies are needed

It should work on all Minecraft versions supported by [Fabric](https://fabricmc.net/)

## Recommended Usage

For the best possible experience (instant server list population with immediate pings),
use this mod together with [Fast IP Ping](https://github.com/Fallen-Breath/fast-ip-ping)

The combination eliminates both the blocked-servers fetch delay and the slow DNS resolution/ping phase,
resulting in the server list refreshing and showing ping results almost instantly when you open the multiplayer menu

## ⚠️ Disclaimer

This mod is not designed for accessing servers in the blocked list. 
If you connect to a Mojang-blocked server via this mod, you assume full responsibility for any possible EULA/Usage Guidelines violations

Always review: 

- [Minecraft EULA](https://www.minecraft.net/en-us/eula)
- [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines)

Provided "as-is" without warranties

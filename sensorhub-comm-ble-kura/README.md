#### Bluetooth LE Communication Support
Bluetooth network provider based on BlueZ hcitool and gatttool commands.

On Ubuntu, you need to run the following command to get permission to send commands with hcitool as a normal user:

```
sudo setcap 'cap_net_raw,cap_net_admin+eip' `which hcitool`
```

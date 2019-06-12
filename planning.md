# Planning

## Octoprint API

* REST endpoint for octoprint version
* REST endpoint for printer data
* Websocket publishes info on current job

## Process

1. Start websocket server
2. Get printer configuration as specified in json file
3. At specified interval:
	a. For each printer, request octoprint version (`/api/version`). If printer is offline or if octoprint version is out of supported range, mark it as such and skip printer. If it changed, publish.
	b. Request printer and printing state (`/api/printer`). If it changed, publish.
	b. If currently printing, subscribe to push updates. Filter incoming messages and re-publish on server together with previous rest api data.

## Architecture

API responses are stored in an embedded cache.

## Config.json

```
{
	server_address, (default "localhost:8080")
	polling_interval, (in seconds, default 5)
	printers: [
		{
			name, (optional, defaults to server_address)
			server_address (required)
		}
	]
}
```

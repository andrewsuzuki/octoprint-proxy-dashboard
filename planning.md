# Planning

## Octoprint API

* REST endpoint for octoprint version
* REST endpoint for printer data
* Websocket publishes info on current job

## Process

1. Start websocket server
2. Get printer configuration as specified in yaml file
3. At specified interval:
	a. For each printer, request octoprint version (`/api/version`). If printer is offline or if octoprint version is out of supported range, mark it as such and skip printer
	b. Request printer and printing state (`/api/printer`)
	b. If currently printing, subscribe to push updates. Filter incoming messages and re-publish on server together with previous rest api data.

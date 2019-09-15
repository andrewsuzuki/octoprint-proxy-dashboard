# Possible Octoprint State Strings

Extracted from Octoprint source (version 1.3.11, September 2019), [here](https://github.com/foosel/OctoPrint/blob/e2519abbbe34f4b6c07ad1ee97436715323ba154/src/octoprint/util/comm.py#L670).

Organized into three groups: initializing, ok, and errored.

## States

Opening serial port
Detecting serial port
Detecting baudrate
Connecting

Operational
Starting print from SD
Starting to send file to SD
Starting
Printing from SD
Sending file to SD
Transferring file to SD
Printing
Cancelling
Pausing
Pause
Resuming
Finishing

Error: {}
Offline
Offline (Error: {})
Unknown State ({})

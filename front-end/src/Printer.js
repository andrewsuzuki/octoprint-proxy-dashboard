import React from "react";
import classNames from "classnames";
import get from "lodash.get";
import "./Printer.css";

const flagConfig = {
  cancelling: { activeBg: "#ffc5c5", inactiveHide: true },
  closedOrError: { activeBg: "#ffc5c5", inactiveHide: false },
  error: { activeBg: "#ff0000", inactiveHide: false },
  finishing: { inactiveHide: true },
  operational: { activeBg: "#d1ffc5", inactiveHide: false },
  paused: { activeBg: "#fffec5", inactiveHide: false },
  pausing: { activeBg: "#fffec5", inactiveHide: true },
  printing: { activeBg: "#d1ffc5", inactiveHide: false },
  ready: { activeBg: "#d1ffc5", inactiveHide: false },
  resuming: { activeBg: "#fffec5", inactiveHide: true },
  sdReady: { activeBg: "#d1ffc5", inactiveHide: false }
};

function Flags({ flags }) {
  if (!flags) {
    return null;
  }

  const flagNames = Object.keys(flags);

  return (
    <p>
      {flagNames.map(name => {
        const value = flags[name];
        const config = flagConfig[name] || {};

        if (!value && config.inactiveHide) {
          return null;
        }

        return (
          <span
            key={name}
            className={classNames("Printer-flag", {
              "Printer-flag-inactive": !value
            })}
            style={
              value && config.activeBg ? { background: config.activeBg } : {}
            }
          >
            {name}
          </span>
        );
      })}
    </p>
  );
}

function Temperatures({ temps }) {
  if (!temps) {
    return null;
  }

  const tempNames = Object.keys(temps);

  return (
    <div>
      {tempNames.map(name => {
        const { actual, offset, target } = temps[name];
        return (
          <p key={name}>
            <span role="img" aria-labelledby="thermometer">
              🌡️
            </span>{" "}
            <strong>{name}</strong> actual {actual}ºc &middot; offset {offset}ºc
            &middot; target {target}ºc
          </p>
        );
      })}
    </div>
  );
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + " Bytes";
  else if (bytes < 1048576) return (bytes / 1024).toFixed(2) + " KB";
  else if (bytes < 1073741824) return (bytes / 1048576).toFixed(2) + " MB";
  else return (bytes / 1073741824).toFixed(2) + " GB";
}

function File({ file }) {
  if (!file || !file.display) {
    return null;
  }

  const { display, size } = file;

  return (
    <p>
      <span role="img" aria-labelledby="document">
        📄
      </span>{" "}
      <strong>{display}</strong> ({formatBytes(size)})
    </p>
  );
}

function formatSeconds(seconds) {
  const numyears = Math.floor(seconds / 31536000);
  const numdays = Math.floor((seconds % 31536000) / 86400);
  const numhours = Math.floor(((seconds % 31536000) % 86400) / 3600);
  const numminutes = Math.floor((((seconds % 31536000) % 86400) % 3600) / 60);
  const numseconds = Math.round((((seconds % 31536000) % 86400) % 3600) % 60);
  const result =
    (numyears ? numyears + "y " : "") +
    (numdays ? numdays + "d " : "") +
    (numhours ? numhours + "h " : "") +
    (numminutes ? numminutes + "m " : "") +
    (numseconds ? numseconds + "s" : "");
  return result || "0";
}

function TimeEstimates({ job }) {
  if (!job || !job.estimatedPrintTime) {
    return null;
  }

  const { averagePrintTime, estimatedPrintTime, lastPrintTime } = job;

  return (
    <p>
      <span role="img" aria-labelledby="hourglass">
        ⌛
      </span>{" "}
      {averagePrintTime && (
        <span>[{formatSeconds(averagePrintTime)} average] </span>
      )}
      {lastPrintTime && <span>[{formatSeconds(lastPrintTime)} last] </span>}
      {estimatedPrintTime && (
        <span>[{formatSeconds(estimatedPrintTime)} estimated] </span>
      )}
    </p>
  );
}

function Progress({ progress }) {
  if (!progress || progress.completion === null) {
    return null;
  }

  const { completion, printTime, printTimeLeft } = progress;

  return (
    <p>
      {completion === 100 ? (
        <span role="img" aria-labelledby="success checkmark">
          ✅️
        </span>
      ) : (
        <span role="img" aria-labelledby="printer">
          🖨️
        </span>
      )}{" "}
      {completion.toFixed(0)}% printed (time printing:{" "}
      {formatSeconds(printTime)}, time left: {formatSeconds(printTimeLeft)})
    </p>
  );
}

function Printer({ printer }) {
  const {
    name,
    errored,
    connected,
    last_retrieved,
    printer_info,
    current_job
  } = printer;

  return (
    <div className="Printer">
      <h2
        className={classNames("Printer-name", {
          "Printer-name-errored": errored || !connected
        })}
      >
        {name}
      </h2>
      <p>Last polled {new Date(last_retrieved).toLocaleTimeString()}</p>
      {errored ? <p>Could not contact server</p> : null}
      {!connected ? (
        <p>Not connected to printer. Please connect manually.</p>
      ) : (
        <React.Fragment>
          <Flags flags={get(printer_info, "state.flags")} />
          <Temperatures temps={get(printer_info, "temperature")} />
          <File file={get(current_job, "job.file")} />
          <TimeEstimates job={get(current_job, "job")} />
          <Progress progress={get(current_job, "progress")} />
        </React.Fragment>
      )}
    </div>
  );
}

export default Printer;

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

function niceTimeString(timestamp) {
  return new Date(timestamp).toLocaleTimeString();
}

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

function tempString(temp) {
  const ks = ["actual", "target", "offset"];
  return ks
    .map(k => ({ k, t: temp[k] }))
    .filter(({ t }) => typeof t === "number")
    .map(({ k, t }) => `${k} ${t}ºc`)
    .join(" · ");
}

function Temperatures({ temps }) {
  if (!temps) {
    return null;
  }

  return (
    <div>
      {temps.map(temp => {
        const { name } = temp;
        return (
          <p key={name}>
            <span role="img" aria-labelledby="thermometer">
              🌡️
            </span>{" "}
            <strong>{name}</strong> {tempString(temp)}
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

function Cam({ cam }) {
  const { data, timestamp } = cam;
  const niceTime = niceTimeString(timestamp);
  return (
    <div className="Printer-cam">
      <img alt={`Webcam snapshot from ${niceTime}`} src={data} />
      <span>{niceTime}</span>
    </div>
  );
}

function Printer({ printer, cam }) {
  const {
    "display-name": name,
    timestamp,
    status, // either connected, disconnected, unreachable, or incompatible
    connection, // either null or {version: string}
    general, // either null or object
    slicer // either null or object
  } = printer;

  const errorString = (() => {
    if (status === "disconnected") {
      return "Disconnected from Octoprint server, is the printer on?";
    } else if (status === "unreachable") {
      return "Could not reach Octoprint server.";
    } else if (status === "incompatible") {
      return "The Octoprint server is not compatible with this proxy.";
    } else if (false) {
      // TODO: also consider general->state.text
    }
    // no error
    return null;
  })();

  return (
    <div className="Printer">
      <h2
        className={classNames("Printer-name", {
          "Printer-name-errored": errorString
        })}
      >
        {name}
      </h2>
      <p>Last polled {niceTimeString(timestamp)}</p>
      {errorString ? (
        <p>
          <i>{errorString}</i>
        </p>
      ) : (
        <React.Fragment>
          <Flags flags={get(general, "state.flags")} />
          <Temperatures temps={get(general, "temps")} />
          {/*
          <File file={get(current_job, "job.file")} />
          <TimeEstimates job={get(current_job, "job")} />
          <Progress progress={get(current_job, "progress")} /> */}
        </React.Fragment>
      )}
      {cam && <Cam cam={cam} />}
    </div>
  );
}

export default Printer;

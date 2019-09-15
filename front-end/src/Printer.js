import React from "react";
import classNames from "classnames";
import get from "lodash.get";
import TimeAgo from "timeago-react";
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

function File({ file }) {
  if (!file || !file.name) {
    return null;
  }

  const { name } = file;

  return (
    <p>
      <span role="img" aria-labelledby="document">
        📄
      </span>{" "}
      <strong>{name}</strong>
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

function TimeEstimates({ times }) {
  if (!times || (!times.estimated && !times.last)) {
    return null;
  }

  const { estimated, last } = times;

  return (
    <p>
      <span role="img" aria-labelledby="hourglass">
        ⌛
      </span>{" "}
      {estimated && <span>{formatSeconds(estimated)} estimated</span>}
      {estimated && last && ", "}
      {last && <span>{formatSeconds(last)} last print</span>}
    </p>
  );
}

function Filament({ filament }) {
  if (!filament || (!filament.length && !filament.volume)) {
    return null;
  }

  const { length, volume } = filament;

  return (
    <p>
      <span role="img" aria-labelledby="thread">
        🧵
      </span>{" "}
      {length && (
        <span>
          {length}mm{" "}
          {volume && (
            <span>
              {" "}
              ({volume}cm<sup>3</sup>)
            </span>
          )}{" "}
          of filament
        </span>
      )}
    </p>
  );
}

function Progress({ progress }) {
  if (!progress || progress.percent === null) {
    return null;
  }

  const {
    percent,
    "seconds-spent": secondsSpent,
    "seconds-left": secondsLeft
  } = progress;

  return (
    <p>
      {percent === 100 ? (
        <span role="img" aria-labelledby="success checkmark">
          ✅️
        </span>
      ) : (
        <span role="img" aria-labelledby="printer">
          🖨️
        </span>
      )}{" "}
      {percent.toFixed(0)}% printed (time printing:{" "}
      {formatSeconds(secondsSpent)}, time left: {formatSeconds(secondsLeft)})
    </p>
  );
}

function Slicer({ slicer }) {
  if (!slicer || !slicer.progress) {
    return null;
  }

  const { "source-path": sourcePath, progress } = slicer;

  return (
    <p>
      <span role="img" aria-labelledby="pizza slice">
        🍕
      </span>{" "}
      <strong>Slicing</strong> {sourcePath && `${sourcePath} `}
      {progress && `(${progress.toFixed(0)}%)`}
    </p>
  );
}

function Cam({ cam }) {
  if (!cam || !cam.data) {
    return null;
  }

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
    general, // either null or object
    slicer // either null or object
  } = printer;

  const t = get(general, "state.text");

  const errorString = (() => {
    if (status === "disconnected") {
      return "Disconnected from the Octoprint server.";
    } else if (status === "unreachable") {
      return "Couldn't connect to Octoprint server. Is it powered on?";
    } else if (status === "incompatible") {
      return "The Octoprint server is not compatible with this proxy.";
    }

    // Take hints from Octoprint state text
    // See document at front-end/possible-state-strings.md
    if (t) {
      const tl = t.toLowerCase();

      const offlineBaseString =
        "Octoprint isn't connected to its printer. Visit its web interface to connect manually.";

      if (tl === "offline") {
        return offlineBaseString;
      } else if (tl.startsWith("offline") && tl.includes("error")) {
        // handle states of format "Offline: (Error: {})"
        // by include it as-is after the baseline offline error string
        return offlineBaseString + " It reports: " + t;
      } else if (tl.includes("error") || tl.includes("unknown state")) {
        return "Octoprint encountered an error: " + t;
      }
    }

    // no error
    return null;
  })();

  return (
    <div className="Printer">
      <h2
        className={classNames("Printer-name", {
          "Printer-name-errored": !!errorString
        })}
      >
        {name}
      </h2>
      <p>
        {!errorString && t && `${t} · `}
        Last update: <TimeAgo datetime={timestamp} />
      </p>
      {errorString ? (
        <p>
          <i>{errorString}</i>
        </p>
      ) : (
        <React.Fragment>
          <Flags flags={get(general, "state.flags")} />
          <Temperatures temps={get(general, "temps")} />
          <File file={get(general, "job.file")} />
          <TimeEstimates times={get(general, "job.times")} />
          <Filament filament={get(general, "job.filament")} />
          <Progress progress={get(general, "progress")} />
        </React.Fragment>
      )}
      {/* Include slicer and cam if available, even if there's an error */}
      <Slicer slicer={slicer} />
      <Cam cam={cam} />
    </div>
  );
}

export default Printer;

// Copyright 2019 Andrew Suzuki. All rights reserved.
package core

import (
    "log"
    "fmt"
    "time"
    "net/http"
    "encoding/json"
    "strings"
)

type PrinterConfig struct {
    Name string `mapstructure:"name"`
    ServerAddress string `mapstructure:"server_address"`
}

type Config struct {
    ServerAddress string `mapstructure:"server_address"`
    PollingInterval int `mapstructure:"polling_interval"`
    PrinterConfigs []PrinterConfig `mapstructure:"printers"`
}

type Printer struct {
    Name string `json:"name"`
    ServerAddress string `json:"server_address"`

    LastRetrieved time.Time `json:"last_retrieved"`
    Errored bool `json:"errored"`

    ApiVersion string `json:"api_version"`
    PrinterInfo map[string]interface{} `json:"printer_info"`
    CurrentJob map[string]interface{} `json:"current_job"`
}

var printers []Printer

// Run is the main function called from cmd/root.go
func Run(c Config) {
    // begin polling printers in goroutine
    go poll(c)

    // initial poll
    updatePrinters(c.PrinterConfigs)

    // set up endpoint and serve
    http.HandleFunc("/", rootEndpoint)
    log.Fatal(http.ListenAndServe(c.ServerAddress, nil))
}

// rootEndpoint is the root http handling function,
// returns a json representation of the current printers state
func rootEndpoint(w http.ResponseWriter, r *http.Request) {
    js, err := json.Marshal(printers)
    if err != nil {
        http.Error(w, "Could not generate response", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.Write(js)
}

// poll runs updatePrinters at the configured polling interval
func poll(c Config) {
    piSeconds := c.PollingInterval

    if piSeconds < 2 {
        log.Fatalln("Polling interval not set or too low")
    }

    piDuration := time.Duration(piSeconds)*time.Second

    for range time.Tick(piDuration) {
        updatePrinters(c.PrinterConfigs)
    }
}

// updatePrinters queries each configured printer
// at a few endpoints and saves them as new Printers in printers
func updatePrinters(printerConfigs []PrinterConfig) {
    log.Println("Updating printers")

    newPrinters := make([]Printer, len(printerConfigs))

    for k, printerConfig := range printerConfigs {
        p := &newPrinters[k]
        p.Name = printerConfig.Name
        p.ServerAddress = printerConfig.ServerAddress
        p.LastRetrieved = time.Now()
        p.Errored = false

        apiVersion, err := getApiVersion(p.ServerAddress)
        if err != nil {
            p.Errored = true
            continue
        }
        p.ApiVersion = apiVersion

        printerInfo, err := getPrinterInfo(p.ServerAddress)
        if err != nil {
            p.Errored = true
            continue
        }
        p.PrinterInfo = printerInfo

        currentJob, err := getCurrentJob(p.ServerAddress)
        if err != nil {
            p.Errored = true
            continue
        }
        p.CurrentJob = currentJob
        if strings.Contains(fmt.Sprintf("%v", currentJob["state"]), "Error") {
            p.Errored = true
            continue
        }

        log.Println(newPrinters[k])
    }

    printers = newPrinters
}

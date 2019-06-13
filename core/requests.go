package core

import (
    "net/http"
    "encoding/json"
)

// Endpoints
const endpointVersion = "/api/version"
const endpointPrinter = "/api/printer?history=false&exclude=sd"
const endpointCurrentJob = "/api/job"

// getApiVersion requests the Octoprint api version
// at the given server
func getApiVersion(serverAddress string) (string, error) {
    res, err := http.Get(serverAddress + endpointVersion)

    if err != nil {
        return "", err
    }

    var result map[string]string

	json.NewDecoder(res.Body).Decode(&result)
    return result["api"], nil
}

// getPrinterInfo requests state and temperature data
// for the given printer
func getPrinterInfo(serverAddress string) (map[string]interface{}, error) {
    res, err := http.Get(serverAddress + endpointPrinter)

    if err != nil {
        return nil, err
    }

    var result map[string]interface{}

	json.NewDecoder(res.Body).Decode(&result)

    return result, nil
}

// getCurrentJob requests information on the
// current job for the given printer
func getCurrentJob(serverAddress string) (map[string]interface{}, error) {
    res, err := http.Get(serverAddress + endpointCurrentJob)

    if err != nil {
        return nil, err
    }

    var result map[string]interface{}

	json.NewDecoder(res.Body).Decode(&result)

    return result, nil
}


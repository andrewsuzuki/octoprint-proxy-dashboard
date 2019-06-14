package core

import (
	"encoding/json"
	"log"
	"net/http"
)

// Endpoints
const endpointVersion = "/api/version"
const endpointPrinter = "/api/printer?history=false&exclude=sd"
const endpointCurrentJob = "/api/job"

func requestWithPrinterConfig(method string, endpoint string, p PrinterConfig) (*http.Response, error) {
	req, err := http.NewRequest(method, p.ServerAddress+endpoint, nil)
	if err != nil {
		log.Fatal(err)
	}
	if len(p.ApiKey) > 0 {
		req.Header.Set("X-Api-Key", p.ApiKey)
	}
	resp, err := http.DefaultClient.Do(req)
	if resp != nil {
		defer resp.Body.Close()
	}
	return resp, err
}

// getApiVersion requests the Octoprint api version
// at the given server
func getApiVersion(p PrinterConfig) (string, error) {
	res, err := requestWithPrinterConfig("GET", endpointVersion, p)

	if err != nil {
		return "", err
	}

	var result map[string]string

	json.NewDecoder(res.Body).Decode(&result)
	return result["api"], nil
}

// getPrinterInfo requests state and temperature data
// for the given printer
func getPrinterInfo(p PrinterConfig) (map[string]interface{}, error) {
	res, err := requestWithPrinterConfig("GET", endpointPrinter, p)

	if err != nil {
		return nil, err
	}

	var result map[string]interface{}

	json.NewDecoder(res.Body).Decode(&result)

	return result, nil
}

// getCurrentJob requests information on the
// current job for the given printer
func getCurrentJob(p PrinterConfig) (map[string]interface{}, error) {
	res, err := requestWithPrinterConfig("GET", endpointCurrentJob, p)

	if err != nil {
		return nil, err
	}

	var result map[string]interface{}

	json.NewDecoder(res.Body).Decode(&result)

	return result, nil
}

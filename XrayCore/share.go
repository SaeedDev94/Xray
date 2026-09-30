package XrayCore

import (
	"encoding/base64"
	"encoding/json"
)

type ShareResponse struct {
	Success bool            `json:"success"`
	Data    json.RawMessage `json:"data,omitempty"`
	Err     string          `json:"error,omitempty"`
}

func (response ShareResponse) EncodeToBase64() string {
	response.Success = response.Err == ""
	jsonData, err := json.Marshal(&response)
	if err != nil {
		return ""
	}
	return base64.StdEncoding.EncodeToString(jsonData)
}

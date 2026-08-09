package XrayCore

import (
	"encoding/base64"
	"encoding/json"

	"github.com/xtls/xray-core/infra/conf"
)

type ShareResponse struct {
	Success bool         `json:"success"`
	Data    *conf.Config `json:"data,omitempty"`
	Err     string       `json:"error,omitempty"`
}

func (response ShareResponse) EncodeToBase64() string {
	response.Success = response.Err == ""
	jsonData, err := json.Marshal(&response)
	if err != nil {
		return ""
	}
	return base64.StdEncoding.EncodeToString(jsonData)
}

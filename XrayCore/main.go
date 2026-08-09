package XrayCore

import (
	"XrayCore/lib"

	"github.com/xtls/libxray/share"
)

func Test(dir string, config string) string {
	err := lib.Test(dir, config)
	return lib.WrapError(err)
}

func Start(dir string, config string) string {
	err := lib.Start(dir, config)
	return lib.WrapError(err)
}

func Stop() string {
	err := lib.Stop()
	return lib.WrapError(err)
}

func Version() string {
	return lib.Version()
}

func Json(link string) string {
	response := ShareResponse{}
	xrayJson, err := share.ConvertShareLinksToXrayJson(link)
	if err == nil {
		response.Data = xrayJson
	} else {
		response.Err = err.Error()
	}
	return response.EncodeToBase64()
}

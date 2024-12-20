package XrayCore

import "github.com/xtls/xray-core/infra/conf"
import "github.com/xtls/libxray/nodep"
import "github.com/xtls/libxray/share"
import "XrayCore/lib"

func Test(dir string, config string) string {
  err := lib.Test(dir, config)
  return lib.WrapError(err)
}

func Start(dir string, config string, memory int64) string {
  err := lib.Start(dir, config, memory)
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
  var response nodep.CallResponse[*conf.Config]
  xrayJson, err := share.ConvertShareLinksToXrayJson(link)
  return response.EncodeToBase64(xrayJson, err)
}

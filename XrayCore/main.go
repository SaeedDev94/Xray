package XrayCore

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

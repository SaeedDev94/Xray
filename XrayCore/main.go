package XrayCore

import (
  "os"
  "runtime/debug"
  "time"
  "github.com/xtls/xray-core/common/cmdarg"
  "github.com/xtls/xray-core/core"
  _ "github.com/xtls/xray-core/main/distro/all"
)

var coreServer *core.Instance

func Test(dir string, config string) string {
  err := test(dir, config)
  return wrapError(err)
}

func Start(dir string, config string, memory int64) string {
  err := start(dir, config, memory)
  return wrapError(err)
}

func Stop() string {
  err := stop()
  return wrapError(err)
}

func Version() string {
  return core.Version()
}

func setEnv(dir string) {
  os.Setenv("xray.location.asset", dir)
}

func makeServer(config string) (*core.Instance, error) {
  file := cmdarg.Arg{config}
  json, err := core.LoadConfig("json", file)
  if err != nil {
    return nil, err
  }
  server, err := core.New(json)
  if err != nil {
    return nil, err
  }
  return server, nil
}

func test(dir string, config string) error {
  setEnv(dir)
  _, err := makeServer(config)
  if err != nil {
    return err
  }
  return nil
}

func start(dir string, config string, memory int64) (err error) {
  setEnv(dir)
  maxMemory(memory)
  coreServer, err = makeServer(config)
  if err != nil {
    return
  }
  if err = coreServer.Start(); err != nil {
    return
  }
  debug.FreeOSMemory()
  return nil
}

func stop() error {
  if coreServer != nil {
    err := coreServer.Close()
    coreServer = nil
    if err != nil {
      return err
    }
  }
  return nil
}

func freeMemory(interval time.Duration) {
  go func() {
    for {
      time.Sleep(interval)
      debug.FreeOSMemory()
    }
  }()
}

func maxMemory(value int64) {
  debug.SetGCPercent(10)
  debug.SetMemoryLimit(value)
  duration := time.Duration(1) * time.Second
  freeMemory(duration)
}

func wrapError(err error) string {
  if err != nil {
    return err.Error()
  }
  return ""
}

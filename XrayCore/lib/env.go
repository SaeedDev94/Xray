package lib

import "os"

func SetEnv(dir string) {
  os.Setenv("xray.location.asset", dir)
}

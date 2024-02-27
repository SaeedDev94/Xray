package lib

import (
  "runtime/debug"
  "time"
)

func FreeMemory(interval time.Duration) {
  go func() {
    for {
      time.Sleep(interval)
      debug.FreeOSMemory()
    }
  }()
}

func MaxMemory(value int64) {
  debug.SetGCPercent(10)
  debug.SetMemoryLimit(value)
  duration := time.Duration(1) * time.Second
  FreeMemory(duration)
}

func FreeOSMemory() {
  debug.FreeOSMemory()
}

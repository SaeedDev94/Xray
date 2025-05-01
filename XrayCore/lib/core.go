package lib

import (
	"github.com/xtls/xray-core/common/cmdarg"
	"github.com/xtls/xray-core/core"
	_ "github.com/xtls/xray-core/main/distro/all"
)

var coreServer *core.Instance

func Server(config string) (*core.Instance, error) {
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

func Start(dir string, config string) (err error) {
	SetEnv(dir)
	coreServer, err = Server(config)
	if err != nil {
		return
	}
	if err = coreServer.Start(); err != nil {
		return
	}
	return nil
}

func Stop() error {
	if coreServer != nil {
		err := coreServer.Close()
		coreServer = nil
		if err != nil {
			return err
		}
	}
	return nil
}

func Version() string {
	return core.Version()
}

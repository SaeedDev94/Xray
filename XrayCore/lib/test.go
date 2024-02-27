package lib

func Test(dir string, config string) error {
  SetEnv(dir)
  _, err := Server(config)
  if err != nil {
    return err
  }
  return nil
}

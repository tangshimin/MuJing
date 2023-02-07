Logo 格式转换命令
```shell
magick logo.png logo.svg
magick logo.svg logo.icns
magick -density 384 -define icon:auto-resize=256,128,96,64,48,32,16 -background none logo.svg logo.ico
```
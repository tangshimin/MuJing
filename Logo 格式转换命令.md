Logo 格式转换命令
```shell
# png 转 svg
magick logo.png logo.svg
# svg 转 ico
magick -density 384 -define icon:auto-resize=256,128,96,64,48,32,16 -background none logo.svg logo.ico
```
png 转 icns 使用这个网站转换：https://www.aconvert.com/
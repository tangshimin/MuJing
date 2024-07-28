#### Shortcut
- Advertised shortcuts（广告快捷方式） 是Windows Installer的一个特性，它们与传统的快捷方式有所不同。 Advertised shortcuts并不直接指向应用程序的可执行文件，而是指向Windows Installer组件。 当用户通过advertised shortcut启动应用程序时，Windows Installer会首先检查应用程序的安装状态，如果需要，它会自动修复或完成安装。

  卸载软件的时候，advertised shortcut会被自动删除。
  
- 非 Advertised shortcuts （普通快捷方式），可以使用右键菜单的“打开文件位置”打开目标文件夹。
  卸载软件的时候，普通快捷方式不会被自动删除。
```xml
            <File Id="MuJing.exe" KeyPath="yes" Source="$(var.SourceDir)\MuJing.exe">
              <Shortcut Advertise="yes" Directory="DesktopFolder" Icon="icon.ico" IconIndex="0" Id="DesktopShortcut" Name="幕境" WorkingDirectory="INSTALLDIR"/>
              <Shortcut Advertise="yes" Directory="ProgramMenuDir" Icon="icon.ico" IconIndex="0" Id="startMenuShortcut" Name="幕境" WorkingDirectory="INSTALLDIR"/>
            </File>
```
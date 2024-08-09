#### Wix 教程
- 官方的 [WiX Toolset Tutorial](https://www.firegiant.com/wix/tutorial/)
- Github 上的 [WiXInstallerExamples](https://github.com/kurtanr/WiXInstallerExamples)
- 博客园的中文教程[Wix 安装部署](https://www.cnblogs.com/stoneniqiu/category/522235.html)

#### Shortcut
Wix 官方文档：[Shortcut Element](https://wixtoolset.org/documentation/manual/v3/xsd/wix/shortcut.html)
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
  删除桌面快捷方式的方法：
```xml
  <Directory Id="DesktopFolder" Name="Desktop">
    <Component Id="DeleteDesktopShortcut" Guid="{CAC2B592-1B6D-4439-AA42-D6DBFAB4D302}" Win64="yes">
      <Shortcut Advertise="no" Directory="DesktopFolder" Target="[INSTALLDIR]MuJing.exe" Icon="icon.ico" IconIndex="0"
                Id="DesktopShortcut" Name="幕境" WorkingDirectory="INSTALLDIR"/>
      <RemoveFile Id="DesktopShortcut" On="uninstall" Name="幕境.lnk" Directory="DesktopFolder"/>
      <RegistryValue Id="DesktopShortcutReg" Key="Software\MuJing" KeyPath="yes" Name="ProductCode" Root="HKCU"
                     Type="string" Value="[ProductCode]"/>
    </Component>
  </Directory>
```
  删除开始菜单快捷方式的方法：
```xml
<Directory Id="ProgramMenuDir" Name="幕境">
    <Component Guid="{A37549DB-C288-3FE3-B8E5-8530D25A07B5}" Id="ProgramMenuDirComponent" Win64="yes">
        <Shortcut Advertise="no" Directory="ProgramMenuDir" Target = "[INSTALLDIR]MuJing.exe" Icon="icon.ico" IconIndex="0" Id="startMenuShortcut" Name="幕境" WorkingDirectory="INSTALLDIR"/>
        <RemoveFolder Directory="ProgramMenuDir" Id="CleanUpShortCutDir" On="uninstall"/>
        <RegistryValue Id="ProgramMenuShortcutReg" Key="Software\MuJing" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
    </Component>
    <Component Guid="{A37549DB-C288-3FE3-B8E5-8530D25A08B5}" Id="RemoveShortcutComponent" Win64="yes">
        <RemoveFile Id="RemoveMenuShortcut" On="uninstall" Name="幕境.lnk" Directory="ProgramMenuDir"/>
        <RegistryValue Id="RemoveMenuShortcutReg" Key="Software\MuJing" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
    </Component>
</Directory>
```

#### KeyPath
官方文档里的介绍：
The KeyPath attribute is set to yes to tell the Windows Installer that this particular file should be used to determine whether the component is installed. If you do not set the KeyPath attribute, WiX will look at the child elements under the component in sequential order and try to automatically select one of them as a key path. Allowing WiX to automatically select a key path can be dangerous because adding or removing child elements under the component can inadvertantly cause the key path to change, which can lead to installation problems. In general, you should always set the KeyPath attribute to yes to ensure that the key path will not inadvertantly change if you update your setup authoring in the future.

KeyPath 是 Windows Installer 中用于标识组件的关键路径。它用于确保组件的唯一性和完整性。每个组件必须有一个唯一的 KeyPath，通常是一个文件或注册表项。对于安装到用户配置文件的组件，KeyPath 必须是一个注册表项，而不是文件

#### RegistryValue
在 Windows Installer 中，如果多个 RegistryValue 元素具有相同的 Name 和 Value 属性，但它们属于不同的组件或路径，则不会有问题。然而，如果它们在同一个组件中，可能会导致冲突或覆盖
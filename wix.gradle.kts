import java.io.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.OutputKeys
import java.nio.charset.StandardCharsets


val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/")
val iconPath = project.file("src/main/resources/logo/logo.ico").absolutePath
val removeIconPath = project.file("RemoveConfig/src/main/resources/remove.ico").absolutePath
val licensePath = project.file("license.rtf").absolutePath
val manufacturer = "深圳市龙华区幕境网络工作室"
val shortcutName = "幕境"
val appName = "幕境"

project.tasks.register("renameApp") {
    group = "compose wix"
    description = "rename the app-image"
    val createDistributable = tasks.named("createDistributable")
    dependsOn(createDistributable)
    doLast {
        val imageDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/幕境")
        val appDirFile = imageDir.getAsFile()
        val appDirParent = appDirFile.parentFile
        val appName = appDirFile.name
        val newAppName = "MuJing"
        val newAppDir = File(appDirParent, newAppName)
        // 幕境 -> MuJing
        appDirFile.renameTo(newAppDir)

        // 把 newAppDir 目录下的 幕境.exe 重命名为 MuJing.exe
        val appExe = File(newAppDir, "$appName.exe")
        val newAppExe = File(newAppDir, "$newAppName.exe")
        appExe.renameTo(newAppExe)

        // 把 newAppDir 目录下的 app 目录下的 幕境.cfg 重命名为 MuJing.cfg
        val appCfg = File(newAppDir, "app/$appName.cfg")
        val newAppCfg = File(newAppDir, "app/$newAppName.cfg")
        appCfg.renameTo(newAppCfg)
    }

}

tasks.register<Exec>("createRemoveConfigExe") {
    group = "compose wix"
    description = "Create RemoveConfig exe"
    workingDir(project.layout.projectDirectory.dir("RemoveConfig"))
    commandLine("gradlew.bat", "createDistributable")
    val renameApp = tasks.named("renameApp")
    dependsOn(renameApp)
    doLast{
        val removeConfigApp =project.layout.projectDirectory.dir("RemoveConfig/build/compose/binaries/main/app/RemoveConfig/app/").asFile
        val mujingApp = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing/app/").asFile
        val mujing = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing/").asFile

        val removeJar = removeConfigApp.listFiles { file -> file.name.startsWith("RemoveConfig") && file.extension == "jar" }?.first()
        val removecfg = project.layout.projectDirectory.dir("RemoveConfig/build/compose/binaries/main/app/RemoveConfig/app/RemoveConfig.cfg").asFile
        val removeExe = project.layout.projectDirectory.dir("RemoveConfig/build/compose/binaries/main/app/RemoveConfig/RemoveConfig.exe").asFile

        // 需要把 RemoveConfig.cfg 和 RemoveConfig.jar 复制到 mujingApp 里面, 把 RemoveConfig.exe 复制到 mujing 里面
        removeJar?.copyTo(File(mujingApp, removeJar?.name))
        removecfg.copyTo(File(mujingApp, removecfg.name))
        removeExe.copyTo(File(mujing, removeExe.name))
    }

}

project.tasks.register<Exec>("harvest") {
    group = "compose wix"
    description = "Generates Wxs authoring from application image"
    val removeConfig = tasks.named("createRemoveConfigExe")
    dependsOn(removeConfig)
    workingDir(appDir)
    val heat = project.layout.projectDirectory.file("build/wix311/heat.exe").asFile.absolutePath

    // heat dir "./MuJing" -cg DefaultFeature -gg -sfrag -sreg -template product -out MuJing.wxs -var var.SourceDir
    commandLine(
        heat,
        "dir",
        "./MuJing",
        "-nologo",
        "-cg",
        "DefaultFeature",
        "-gg",
        "-sfrag",
        "-sreg",
        "-template",
        "product",
        "-out",
        "MuJing.wxs",
        "-var",
        "var.SourceDir"
    )

}

project.tasks.register("editWxs") {
    group = "compose wix"
    description = "Edit the Wxs File"
    val harvest = tasks.named("harvest")
    dependsOn(harvest)
    doLast {
        editWixTask(
            shortcutName = shortcutName,
            iconPath = iconPath,
            removeIconPath = removeIconPath,
            licensePath = licensePath,
            manufacturer = manufacturer
        )
    }
}

project.tasks.register<Exec>("compileWxs") {
    group = "compose wix"
    description = "Compile Wxs to Wixobj"
    val editWxs = tasks.named("editWxs")
    dependsOn(editWxs)
    workingDir(appDir)
    val candle = project.layout.projectDirectory.file("build/wix311/candle.exe").asFile.absolutePath
    // candle main.wxs -dSourceDir=".\MuJing"
    commandLine(candle, "MuJing.wxs","-nologo", "-dSourceDir=.\\MuJing")
}

project.tasks.register<Exec>("light") {
    group = "compose wix"
    description = "Linking the .wixobj file and creating a MSI"
    val compileWxs = tasks.named("compileWxs")
    dependsOn(compileWxs)
    workingDir(appDir)
    val light = project.layout.projectDirectory.file("build/wix311/light.exe").asFile.absolutePath

    // light -ext WixUIExtension -cultures:zh-CN -spdb MuJing.wixobj -o MuJing.msi
    commandLine(light, "-ext", "WixUIExtension", "-cultures:zh-CN", "-spdb","-nologo", "MuJing.wixobj", "-o", "MuJing-${project.version}.msi")
}

project.tasks.register<Zip>("packagePortable") {
    group = "compose wix"
    description = "Create a portable zip package"
    val createRemoveConfigExe = tasks.named("createRemoveConfigExe")
    dependsOn(createRemoveConfigExe)

    archiveFileName.set("MuJing-${project.version}.zip")
    destinationDirectory.set(appDir.asFile)

    from(appDir.dir("MuJing"))
    into("MuJing")
}


private fun editWixTask(
    shortcutName: String,
    iconPath: String,
    removeIconPath: String,
    licensePath: String,
    manufacturer:String
) {
    val wixFile = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing.wxs").asFile

    val dbf = DocumentBuilderFactory.newInstance()
    val doc = dbf.newDocumentBuilder().parse(wixFile)
    doc.documentElement.normalize()

    println("Root Element :" + doc.documentElement.nodeName)
    val productElement = doc.documentElement.getElementsByTagName("Product").item(0) as Element
    println(productElement.nodeName)
    productElement.setAttribute("Manufacturer", manufacturer)
    productElement.setAttribute("Codepage", "936")

    // 这个 Name 属性会出现在安装引导界面
    // 控制面板-程序列表里也是这个名字
    productElement.setAttribute("Name", "幕境")
    productElement.setAttribute("Version", "${project.version}")

    // 设置升级码, 用于升级,大版本更新时，可能需要修改这个值
    // 如果要修改这个值，可能还需要修改安装位置，如果不修改安装位置，两个版本会安装在同一个位置
    // 这段代码和 MajorUpgrade 相关，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
    val upgradeCode = createNameUUID("v2.0")
    productElement.setAttribute("UpgradeCode", upgradeCode)


    val packageElement = productElement.getElementsByTagName("Package").item(0) as Element
    println(packageElement.nodeName)
    packageElement.setAttribute("Comments", "幕境")
    packageElement.setAttribute("Compressed", "yes")
    packageElement.setAttribute("InstallerVersion", "200")
    packageElement.setAttribute("Languages", "1033")
    packageElement.setAttribute("Manufacturer", manufacturer)
    packageElement.setAttribute("Platform", "x64")

    //    <CustomAction Id="RunRemoveConfigExe"
    //        FileKey="RemoveConfig.exe"
    //        ExeCommand=""
    //        Execute="deferred"
    //        Impersonate="yes"
    //        Return="ignore" />
    //    <InstallExecuteSequence>
    //        <Custom Action="RunRemoveConfigExe" After="UnpublishFeatures">(REMOVE = "ALL") AND (NOT UPGRADINGPRODUCTCODE)</Custom>
    //    </InstallExecuteSequence>
    val removeConfig = doc.createElement("CustomAction").apply {
        setAttributeNode(doc.createAttribute("Id").also { it.value = "RunRemoveConfigExe" })
        setAttributeNode(doc.createAttribute("FileKey").also { it.value = "RemoveConfig.exe" })
        setAttributeNode(doc.createAttribute("ExeCommand").also { it.value = "--uninstall" })
        setAttributeNode(doc.createAttribute("Execute").also { it.value = "deferred" })
        setAttributeNode(doc.createAttribute("Impersonate").also { it.value = "yes" })
        setAttributeNode(doc.createAttribute("Return").also { it.value = "ignore" })
    }
    productElement.appendChild(removeConfig)

    //    <CustomAction Id="RunVlcCacheGen"
    //                  Execute="deferred"
    //                  Impersonate="no"
    //                  Directory="INSTALLDIR"
    //                  ExeCommand="[INSTALLDIR]app\\resources\\VLC\\vlc-cache-gen.exe [INSTALLDIR]app\\resources\\VLC\\plugins"
    //                  Return="check" />
    val runVlcCacheGen = doc.createElement("CustomAction").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "RunVlcCacheGen" })
        setAttributeNode(doc.createAttribute("Execute").also { it.value = "deferred" })
        setAttributeNode(doc.createAttribute("Impersonate").also { it.value = "no" })
        setAttributeNode(doc.createAttribute("Directory").also { it.value = "INSTALLDIR" })
        setAttributeNode(doc.createAttribute("ExeCommand").also { it.value = "[INSTALLDIR]app\\resources\\VLC\\vlc-cache-gen.exe [INSTALLDIR]app\\resources\\VLC\\plugins" })
        setAttributeNode(doc.createAttribute("Return").also { it.value = "check" })
    }
    productElement.appendChild(runVlcCacheGen)

    val installExecuteSequence = doc.createElement("InstallExecuteSequence")
    productElement.appendChild(installExecuteSequence)
//    <Custom Action="RunRemoveConfigExe" After="UnpublishFeatures">(REMOVE = "ALL") AND (NOT UPGRADINGPRODUCTCODE)</Custom>
    val customActionRef = doc.createElement("Custom").apply{
        setAttributeNode(doc.createAttribute("Action").also { it.value = "RunRemoveConfigExe" })
        setAttributeNode(doc.createAttribute("After").also { it.value = "UnpublishFeatures" })
        appendChild(doc.createTextNode("(REMOVE = \"ALL\") AND (NOT UPGRADINGPRODUCTCODE)"))
    }
    installExecuteSequence.appendChild(customActionRef)

    // <Custom Action="RunVlcCacheGen" After="InstallFiles">NOT Installed</Custom>
    val customActionGenCache = doc.createElement("Custom").apply{
        setAttributeNode(doc.createAttribute("Action").also { it.value = "RunVlcCacheGen" })
        setAttributeNode(doc.createAttribute("After").also { it.value = "InstallFiles" })
        appendChild(doc.createTextNode("NOT Installed"))
    }
    installExecuteSequence.appendChild(customActionGenCache)

    val targetDirectory = doc.documentElement.getElementsByTagName("Directory").item(0) as Element

    // 桌面文件夹
    // <Directory Id="DesktopFolder" Name="Desktop" />
    val desktopFolderElement = directoryBuilder(doc, id = "DesktopFolder").apply{
        setAttributeNode(doc.createAttribute("Name").also { it.value = "Desktop" })
    }
    val desktopGuid = createNameUUID("DesktopShortcutComponent")
    val desktopComponent = componentBuilder(doc, id = "DesktopShortcutComponent", guid = desktopGuid)
    val desktopReg = registryBuilder(doc, id = "DesktopShortcutReg", productCode = "[ProductCode]")
    // <Shortcut Advertise="no" Directory="DesktopFolder" Target = "[INSTALLDIR]MuJing.exe" Icon="icon.ico" IconIndex="0" Id="DesktopShortcut" Name="幕境" WorkingDirectory="INSTALLDIR"/>
    val desktopShortcut = shortcutBuilder(
        doc,
        id = "DesktopShortcut",
        directory = "DesktopFolder",
        workingDirectory = "INSTALLDIR",
        name = shortcutName,
        target = "[INSTALLDIR]MuJing.exe",
        icon="icon.ico"
    )
    //   <RemoveFile Id="DesktopShortcut" On="uninstall" Name="幕境.lnk" Directory="DesktopFolder"/>
    val removeDesktopShortcut = doc.createElement("RemoveFile").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "DesktopShortcut" })
        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
        setAttributeNode(doc.createAttribute("Name").also { it.value = "$shortcutName.lnk" })
        setAttributeNode(doc.createAttribute("Directory").also { it.value = "DesktopFolder" })
    }
    desktopComponent.appendChild(desktopShortcut)
    desktopComponent.appendChild(desktopReg)
    desktopComponent.appendChild(removeDesktopShortcut)
    desktopFolderElement.appendChild(desktopComponent)
    targetDirectory.appendChild(desktopFolderElement)



    // 开始菜单文件夹
    val programMenuFolderElement = directoryBuilder(doc, id = "ProgramMenuFolder", name = "Programs")
    val programeMenuDir = directoryBuilder(doc, id = "ProgramMenuDir", name = "幕境")
    val menuGuid = createNameUUID("programMenuDirComponent")
    val programMenuDirComponent = componentBuilder(doc, id = "programMenuDirComponent", guid = menuGuid)
    val startMenuShortcut = shortcutBuilder(
        doc,
        id = "startMenuShortcut",
        directory = "ProgramMenuDir",
        workingDirectory = "INSTALLDIR",
        name = shortcutName,
        target = "[INSTALLDIR]MuJing.exe"
    )
    val uninstallShortcut = shortcutBuilder(
        doc,
        id = "uninstallShortcut",
        name = "卸载幕境",
        directory = "ProgramMenuDir",
        target = "[System64Folder]msiexec.exe",
        arguments = "/x [ProductCode]",
        icon = "removeIcon.ico"
    )
    val removeFolder = removeFolderBuilder(doc, id = "CleanUpShortCut", directory = "ProgramMenuDir")
    val pRegistryValue = registryBuilder(doc, id = "ProgramMenuShortcutReg", productCode = "[ProductCode]")

    programMenuFolderElement.appendChild(programeMenuDir)
    programeMenuDir.appendChild(programMenuDirComponent)
    programMenuDirComponent.appendChild(startMenuShortcut)
    programMenuDirComponent.appendChild(uninstallShortcut)
    programMenuDirComponent.appendChild(removeFolder)
    programMenuDirComponent.appendChild(pRegistryValue)

    //<Component Guid="*" Id="RemoveShortcutComponent" Win64="yes">
    //  <RemoveFile Id="RemoveMenuShortcut" On="uninstall" Name="幕境.lnk" Directory="ProgramMenuDir"/>
    //  <RegistryValue Id="RemoveMenuShortcutReg" Key="Software\MuJing" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
    //</Component>
    val removeShortcutComponent = componentBuilder(doc, id = "RemoveShortcutComponent", guid = createNameUUID("RemoveShortcutComponent"))
    val removeMenuShortcut = doc.createElement("RemoveFile").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "RemoveMenuShortcut" })
        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
        setAttributeNode(doc.createAttribute("Name").also { it.value = "*.lnk" })
        setAttributeNode(doc.createAttribute("Directory").also { it.value = "ProgramMenuDir" })
    }
    val removeMenuShortcutReg = registryBuilder(doc, id = "RemoveMenuShortcutReg", productCode = "[ProductCode]")
    removeShortcutComponent.appendChild(removeMenuShortcut)
    removeShortcutComponent.appendChild(removeMenuShortcutReg)


    targetDirectory.appendChild(programMenuFolderElement)
    targetDirectory.appendChild(removeShortcutComponent)

    // 添加 ProgramFiles64Folder 节点
    val programFilesElement = doc.createElement("Directory")
    val idAttr = doc.createAttribute("Id")
    idAttr.value = "ProgramFiles64Folder"
    programFilesElement.setAttributeNode(idAttr)
    targetDirectory.appendChild(programFilesElement)
    val installDir = targetDirectory.getElementsByTagName("Directory").item(0)
    // 移除 installDir 节点
    val removedNode = targetDirectory.removeChild(installDir)
    // 将 installDir 节点添加到 programFilesElement 节点
    programFilesElement.appendChild(removedNode)
    // 设置安装目录的 Id 为 INSTALLDIR，快捷方式需要引用这个 Id
    val installDirElement = programFilesElement.getElementsByTagName("Directory").item(0) as Element
    installDirElement.setAttribute("Id", "INSTALLDIR")

    val fileComponents = installDirElement.getElementsByTagName("Component")
    for (i in 0 until fileComponents.length) {
        val component = fileComponents.item(i) as Element
        val files = component.getElementsByTagName("File")
        val file = files.item(0) as Element
        // 设置 RemoveConfig.exe文件的 Id 为 RemoveConfig.exe
        if(file.getAttribute("Source").endsWith("RemoveConfig.exe")){
            file.setAttribute("Id","RemoveConfig.exe")
        }

    }

    // <Component Guid="{GUID}" Id="installProduct">
    //    <RegistryValue Root="HKLM" Key="Software\MuJing"
    //                   Name="InstallLocation" Type="string" Value="[INSTALLDIR]" KeyPath="yes"/>
    //</Component>
    val installGuid = createNameUUID("installProduct")
    val installComponent = componentBuilder(doc, id = "installProduct", guid = installGuid)
    val installRegistry = doc.createElement("RegistryValue").apply{
        setAttributeNode(doc.createAttribute("Root").also { it.value = "HKLM" })
        setAttributeNode(doc.createAttribute("Key").also { it.value = "Software\\MuJing" })
        setAttributeNode(doc.createAttribute("Name").also { it.value = "InstallLocation" })
        setAttributeNode(doc.createAttribute("Type").also { it.value = "string" })
        setAttributeNode(doc.createAttribute("Value").also { it.value = "[INSTALLDIR]" })
        setAttributeNode(doc.createAttribute("KeyPath").also { it.value = "yes" })
    }
    installComponent.appendChild(installRegistry)
    installDirElement.appendChild(installComponent)


    // 设置所有组件的架构为 64 位
    val components = doc.documentElement.getElementsByTagName("Component")
    for (i in 0 until components.length) {
        val component = components.item(i) as Element
        val win64 = doc.createAttribute("Win64")
        win64.value = "yes"
        component.setAttributeNode(win64)
    }

    // 设置 Feature 节点
    val featureElement = doc.getElementsByTagName("Feature").item(0) as Element
    featureElement.setAttribute("Id", "Complete")
    featureElement.setAttribute("Title", "幕境")

    // 设置 UI
    // 添加 <Property Id="WIXUI_INSTALLDIR" Value="INSTALLDIR" />
    val installUI = doc.createElement("Property").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "WIXUI_INSTALLDIR" })
        setAttributeNode(doc.createAttribute("Value").also { it.value = "INSTALLDIR" })
    }
    productElement.appendChild(installUI)

    // 添加 <UIRef Id="WixUI_InstallDir" />
    val installDirUIRef = doc.createElement("UIRef").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_InstallDir" })
    }
    productElement.appendChild(installDirUIRef)

    // 添加 <UIRef Id="WixUI_ErrorProgressText" />
    val errText = doc.createElement("UIRef").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_ErrorProgressText" })
    }
    productElement.appendChild(errText)

    //  添加 Icon, 这个 Icon 会显示在控制面板的应用程序列表
    //  <Icon Id="icon.ico" SourceFile="D:\MuJing\wix\MuJing\logo.ico"/>

    val iconElement = doc.createElement("Icon").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "icon.ico" })
        setAttributeNode(doc.createAttribute("SourceFile").also { it.value = iconPath })
    }
    productElement.appendChild(iconElement)

    //  <Property Id="ARPPRODUCTICON" Value="icon.ico" />
    val iconProperty = doc.createElement("Property").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "ARPPRODUCTICON" })
        setAttributeNode(doc.createAttribute("Value").also { it.value = "icon.ico" })
    }
    productElement.appendChild(iconProperty)

    //<Icon Id="removeIcon.ico" SourceFile="removeIconPath"/>
    val removeIconElement = doc.createElement("Icon").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "removeIcon.ico" })
        setAttributeNode(doc.createAttribute("SourceFile").also { it.value = removeIconPath })
    }
    productElement.appendChild(removeIconElement)
    // 设置 license file
    //  <WixVariable Id="WixUILicenseRtf" Value="license.rtf" />
    val wixVariable = doc.createElement("WixVariable").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUILicenseRtf" })
        setAttributeNode(doc.createAttribute("Value").also { it.value = licensePath })
    }
    productElement.appendChild(wixVariable)

    // 安装新版时，自动卸载旧版本，已经安装新版，再安装旧版本，提示用户先卸载新版。
    // 这段逻辑要和 UpgradeCode 一起设置，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
    // 如果 UpgradeCode 改变了，可能会安装两个版本
    // <MajorUpgrade AllowSameVersionUpgrades="yes" DowngradeErrorMessage="新版的[ProductName]已经安装，如果要安装旧版本，请先把新版本卸载。" />
    val majorUpgrade = doc.createElement("MajorUpgrade").apply{
        setAttributeNode(doc.createAttribute("AllowSameVersionUpgrades").also { it.value = "yes" })
        val message = "新版的[ProductName]已经安装，如果要安装旧版本，请先把新版本卸载。"
        setAttributeNode(doc.createAttribute("DowngradeErrorMessage").also { it.value = message })
    }
    productElement.appendChild(majorUpgrade)


    //    <Property Id="INSTALLED">
    //      <RegistrySearch Id="SearchOldVersion" Root="HKCU" Key="Software\深圳市龙华区幕境网络工作室\幕境\2.3.1"  Name="ProductCode" Type="raw" Win64 = "yes" />
    //    </Property>
    val installedProperty = doc.createElement("Property")
    val installedPropertyId = doc.createAttribute("Id")
    installedPropertyId.value = "INSTALLED"
    installedProperty.setAttributeNode(installedPropertyId)
    // 幕境 v2 的所有版本
    val oldVersionList = listOf("2.3.1","2.3.0","2.2.25","2.2.23","2.2.20","2.2.16","2.2.13","2.2.12","2.2.3","2.1.6","2.1.5","2.1.4","2.1.1","2.0.8","2.0.7","2.0.6","2.0.5","2.0.4","2.0.3","2.0.2")
    var id = 1
    oldVersionList.forEach{version ->
        val registrySearch = registrySearchBuilder(doc,version,id)
        id++
        installedProperty.appendChild(registrySearch)
    }
    productElement.appendChild(installedProperty)

    // Condition 的语法好变态，如果不看文档根本没法写，语法的详细解释在这里：
    // https://learn.microsoft.com/en-us/windows/win32/msi/conditional-statement-syntax#summary-of-conditional-statement-syntax
    //<!--          NOT INSTALLED -->
    //<!--  如果 INSTALLED 为空或 null，那么 NOT INSTALLED 的结果将是 FALSE-->
    //<!--  如果 INSTALLED 不为空或 null，那么 NOT INSTALLED 的结果将是 TRUE-->
    //    <Condition Message="已经安装了幕境的另一个版本，无法继续安装此版本。可以使用”控制面板“中”添加/删除程序“来删除该版本">
    //        <![CDATA[NOT INSTALLED]]>
    //    </Condition>
    val installCondition = doc.createElement("Condition").apply{
        val message = "已经安装了幕境的另一个版本，无法继续安装此版本。可以使用”控制面板“中”添加/删除程序“来删除该版本"
        setAttributeNode(doc.createAttribute("Message").also { it.value = message })
        appendChild(doc.createCDATASection("NOT INSTALLED"))
    }
    productElement.appendChild(installCondition)


    // 设置 fragment 节点
    val fragmentElement = doc.getElementsByTagName("Fragment").item(0) as Element
    val componentGroup = fragmentElement.getElementsByTagName("ComponentGroup").item(0) as Element
    val desktopShortcuRef = componentRefBuilder(doc, "DesktopShortcutComponent")
    val programMenuDirRef = componentRefBuilder(doc, "programMenuDirComponent")
    val removeShortcutRef = componentRefBuilder(doc, "RemoveShortcutComponent")
    val installProductRef = componentRefBuilder(doc, "installProduct")
    componentGroup.appendChild(desktopShortcuRef)
    componentGroup.appendChild(programMenuDirRef)
    componentGroup.appendChild(installProductRef)
    componentGroup.appendChild(removeShortcutRef)

    generateXml(doc, wixFile)
}


private fun generateXml(doc: Document, file: File) {

    // Instantiate the Transformer
    val transformerFactory = TransformerFactory.newInstance()
    transformerFactory.setAttribute("indent-number", 4);
    val transformer = transformerFactory.newTransformer()

    // Enable indentation and set encoding
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

    val source = DOMSource(doc)
    val result = StreamResult(file)
    transformer.transform(source, result)
}


private fun directoryBuilder(doc: Document, id: String, name: String = ""): Element {
    val directory = doc.createElement("Directory").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        if(name.isNotEmpty()){
            setAttributeNode(doc.createAttribute("Name").also { it.value = name })
        }
    }
    return directory
}

private fun componentBuilder(doc: Document, id: String, guid: String): Element {
    val component = doc.createElement("Component").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        setAttributeNode(doc.createAttribute("Guid").also { it.value = guid })
    }
    return component
}

private fun registryBuilder(doc: Document, id: String, productCode: String): Element {
    val regComponentElement = doc.createElement("RegistryValue").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        setAttributeNode(doc.createAttribute("Root").also { it.value = "HKCU" })
        setAttributeNode(doc.createAttribute("Key").also { it.value = "Software\\MuJing" })
        setAttributeNode(doc.createAttribute("Type").also { it.value = "string" })
        setAttributeNode(doc.createAttribute("Name").also { it.value = "ProductCode" })
        setAttributeNode(doc.createAttribute("Value").also { it.value = productCode })
        setAttributeNode(doc.createAttribute("KeyPath").also { it.value = "yes" })
    }
    return regComponentElement
}

private fun registrySearchBuilder(doc:Document,version:String,id:Int):Element{
    val registrySearch = doc.createElement("RegistrySearch").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = "SearchOldVersion$id" })
        setAttributeNode(doc.createAttribute("Root").also { it.value = "HKCU" })
        setAttributeNode(doc.createAttribute("Key").also { it.value = "Software\\深圳市龙华区幕境网络工作室\\幕境\\$version" })
        setAttributeNode(doc.createAttribute("Name").also { it.value = "ProductCode" })
        setAttributeNode(doc.createAttribute("Type").also { it.value = "raw" })
        setAttributeNode(doc.createAttribute("Win64").also { it.value = "yes" })
    }
    return registrySearch
}
private fun shortcutBuilder(
    doc: Document,
    id: String,
    directory: String = "",
    workingDirectory: String = "",
    name: String,
    target: String,
    description: String = "",
    arguments: String = "",
    icon:String = ""
): Element {
    val shortcut = doc.createElement("Shortcut").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        setAttributeNode(doc.createAttribute("Name").also { it.value = name })
        setAttributeNode(doc.createAttribute("Advertise").also { it.value = "no" })
        setAttributeNode(doc.createAttribute("Target").also { it.value = target })
        if(directory.isNotEmpty()){
            setAttributeNode(doc.createAttribute("Directory").also { it.value = directory })
        }
        if(workingDirectory.isNotEmpty()){
            setAttributeNode(doc.createAttribute("WorkingDirectory").also { it.value = workingDirectory })
        }
        if(description.isNotEmpty()){
            setAttributeNode(doc.createAttribute("Description").also { it.value = description })
        }
        if(arguments.isNotEmpty()){
            setAttributeNode(doc.createAttribute("Arguments").also { it.value = arguments })
        }
        if(icon.isNotEmpty()){
            setAttributeNode(doc.createAttribute("Icon").also { it.value = icon })
        }
    }

    return shortcut
}

private fun removeFolderBuilder(doc: Document, id: String,directory:String): Element {
    val removeFolder = doc.createElement("RemoveFolder").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        setAttributeNode(doc.createAttribute("Directory").also { it.value = directory })
        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
    }
    return removeFolder
}

private fun componentRefBuilder(doc: Document, id: String): Element {
    val componentRef = doc.createElement("ComponentRef").apply{
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
    }
    return componentRef
}

private fun createNameUUID(str: String): String {
    return "{" + UUID.nameUUIDFromBytes(str.toByteArray(StandardCharsets.UTF_8)).toString().uppercase() + "}"
}

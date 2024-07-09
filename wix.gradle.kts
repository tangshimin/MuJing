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
val licensePath = project.file("license.rtf").absolutePath
val manufacturer = "深圳市龙华区幕境网络工作室"
val shortcutName = "幕境"
val appName = "幕境"

project.tasks.register("renameApp") {
    group = "compose wix"
    description = "rename the app-image"
    val createVlcCacheDistributable = tasks.named("createVlcCacheDistributable")
    dependsOn(createVlcCacheDistributable)
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

tasks.register<Exec>("createUninstallExe") {
    group = "compose wix"
    description = "create uninstall exe"
    workingDir(project.layout.projectDirectory.dir("uninstall"))
    commandLine("gradlew.bat", "createDistributable")
    val renameApp = tasks.named("renameApp")
    dependsOn(renameApp)
    doLast{
        val uninstallApp =project.layout.projectDirectory.dir("uninstall/build/compose/binaries/main/app/uninstall/app/").getAsFile()
        val mujingApp = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing/app/").getAsFile()
        val mujing = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing/").getAsFile()

        val uninstallJar = uninstallApp.listFiles { file -> file.name.startsWith("uninstall") && file.extension == "jar" }?.first()
        val uninstallcfg = project.layout.projectDirectory.dir("uninstall/build/compose/binaries/main/app/uninstall/app/uninstall.cfg").getAsFile()
        val uninstallExe = project.layout.projectDirectory.dir("uninstall/build/compose/binaries/main/app/uninstall/uninstall.exe").getAsFile()

        // 需要把 uninstall.cfg 和 uninstall.jar 复制到 mujingApp 里面, 把 uninstall.exe 复制到 mujing 里面
        uninstallJar?.copyTo(File(mujingApp, uninstallJar?.name))
        uninstallcfg.copyTo(File(mujingApp, uninstallcfg.name))
        uninstallExe.copyTo(File(mujing, uninstallExe.name))
    }

}

project.tasks.register<Exec>("harvest") {
    group = "compose wix"
    description = "Generates Wxs authoring from application image"
    val uninstall = tasks.named("createUninstallExe")
    dependsOn(uninstall)
    workingDir(appDir)
    val heat = project.layout.projectDirectory.file("build/wix311/heat.exe").getAsFile().absolutePath

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
    val candle = project.layout.projectDirectory.file("build/wix311/candle.exe").getAsFile().absolutePath
    // candle main.wxs -dSourceDir=".\MuJing"
    commandLine(candle, "MuJing.wxs","-nologo", "-dSourceDir=.\\MuJing")
}

project.tasks.register<Exec>("light") {
    group = "compose wix"
    description = "Linking the .wixobj file and creating a MSI"
    val compileWxs = tasks.named("compileWxs")
    dependsOn(compileWxs)
    workingDir(appDir)
    val light = project.layout.projectDirectory.file("build/wix311/light.exe").getAsFile().absolutePath

    // light -ext WixUIExtension -cultures:zh-CN -spdb MuJing.wixobj -o MuJing.msi
    commandLine(light, "-ext", "WixUIExtension", "-cultures:zh-CN", "-spdb","-nologo", "MuJing.wixobj", "-o", "MuJing-${project.version}.msi")
}




private fun editWixTask(
    shortcutName: String,
    iconPath: String,
    licensePath: String,
    manufacturer:String
) {
    val wixFile = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing.wxs").getAsFile()

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
    val upgradeCode = createNameUUID("UpgradeCode")
    productElement.setAttribute("UpgradeCode", upgradeCode)


    val packageElement = productElement.getElementsByTagName("Package").item(0) as Element
    println(packageElement.nodeName)
    packageElement.setAttribute("Comments", "幕境")
    packageElement.setAttribute("Compressed", "yes")
    packageElement.setAttribute("InstallerVersion", "200")
    packageElement.setAttribute("Languages", "1033")
    packageElement.setAttribute("Manufacturer", manufacturer)
    packageElement.setAttribute("Platform", "x64")


    val targetDirectory = doc.documentElement.getElementsByTagName("Directory").item(0) as Element

    // 添加桌面快捷方式
    val desktopFolderElement = directoryBuilder(doc, id = "DesktopFolder")
    val desktopGuid = createNameUUID("DesktopFolder")
    val shortcutComponentElement = componentBuilder(doc, id = "DesktopFolder", guid = desktopGuid)
    val regComponentElement = registryBuilder(doc, id = "DesktopShortcutReg", productCode = "[ProductCode]")
    val shortcutElement = shortcutBuilder(
        doc,
        id = "DesktopShortcut",
        directory = "DesktopFolder",
        workingDirectory = "INSTALLDIR",
        name = shortcutName,
        target = "[INSTALLDIR]MuJing.exe"
    )
    shortcutComponentElement.appendChild(regComponentElement)
    shortcutComponentElement.appendChild(shortcutElement)
    desktopFolderElement.appendChild(shortcutComponentElement)
    targetDirectory.appendChild(desktopFolderElement)

    // 添加开始菜单快捷方式
    val programMenuFolderElement = directoryBuilder(doc, id = "ProgramMenuFolder", name = "Programs")
    val programeMenuDir = directoryBuilder(doc, id = "ProgramMenuDir", name = "幕境")
    val menuGuid = createNameUUID("programeMenuDirComponent")
    val programeMenuDirComponent = componentBuilder(doc, id = "programeMenuDirComponent", guid = menuGuid)
    val startMenuShortcut = shortcutBuilder(
        doc,
        id = "startMenuShortcut",
        directory = "ProgramMenuDir",
        workingDirectory = "INSTALLDIR",
        name = shortcutName,
        target = "[INSTALLDIR]MuJing.exe"
    )
    val removeFolder = removeFolderBuilder(doc, id = "ProgramMenuDir")
    val pRegistryValue = registryBuilder(doc, id = "ProgramMenuShortcutReg", productCode = "[ProductCode]")

    programMenuFolderElement.appendChild(programeMenuDir)
    programeMenuDir.appendChild(programeMenuDirComponent)
    programeMenuDirComponent.appendChild(startMenuShortcut)
    programeMenuDirComponent.appendChild(removeFolder)
    programeMenuDirComponent.appendChild(pRegistryValue)

    // 添加卸载软件的快捷方式
    val uninstallGuid = createNameUUID("UninstallProduct")
    val uninstallComponent = componentBuilder(doc, id = "UninstallProduct", guid = uninstallGuid)
    val uninstallShortcut = shortcutBuilder(
        doc,
        id = "uninstallShortcut",
        name = "卸载幕境",
        directory = "ProgramMenuDir",
        workingDirectory = "INSTALLDIR",
        target = "[INSTALLDIR]uninstall.exe",
    )
    val uninstallRegistry = registryBuilder(doc, id = "uninstallShortcutReg", productCode = "[ProductCode]")
    uninstallComponent.appendChild(uninstallShortcut)
    uninstallComponent.appendChild(uninstallRegistry)

    programeMenuDir.appendChild(uninstallComponent)
//    programMenuFolderElement.appendChild(uninstallComponent)

    targetDirectory.appendChild(programMenuFolderElement)

    // 设置所有组件的架构为 64 位
    val components = doc.documentElement.getElementsByTagName("Component")
    for (i in 0 until components.length) {
        val component = components.item(i) as Element
        val win64 = doc.createAttribute("Win64")
        win64.value = "yes"
        component.setAttributeNode(win64)
    }

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


    // 设置 Feature 节点
    val featureElement = doc.getElementsByTagName("Feature").item(0) as Element
    featureElement.setAttribute("Id", "Complete")
    featureElement.setAttribute("Title", "幕境")

    // 设置 UI
    // 添加 <Property Id="WIXUI_INSTALLDIR" Value="INSTALLDIR" />
    val installUI = doc.createElement("Property")
    val propertyId = doc.createAttribute("Id")
    propertyId.value = "WIXUI_INSTALLDIR"
    val peopertyValue = doc.createAttribute("Value")
    peopertyValue.value = "INSTALLDIR"
    installUI.setAttributeNode(propertyId)
    installUI.setAttributeNode(peopertyValue)
    productElement.appendChild(installUI)

    // 添加 <UIRef Id="WixUI_InstallDir" />
    val installDirUIRef = doc.createElement("UIRef")
    val dirUiId = doc.createAttribute("Id")
    dirUiId.value = "WixUI_InstallDir"
    installDirUIRef.setAttributeNode(dirUiId)
    productElement.appendChild(installDirUIRef)

    // 添加 <UIRef Id="WixUI_ErrorProgressText" />
    val errText = doc.createElement("UIRef")
    val errUiId = doc.createAttribute("Id")
    errUiId.value = "WixUI_ErrorProgressText"
    errText.setAttributeNode(errUiId)
    productElement.appendChild(errText)

    //  添加 Icon, 这个 Icon 会显示在控制面板的应用程序列表
    //  <Icon Id="icon.ico" SourceFile="D:\MuJing\wix\MuJing\logo.ico"/>
    //  <Property Id="ARPPRODUCTICON" Value="icon.ico" />
    val iconElement = doc.createElement("Icon")
    val iconId = doc.createAttribute("Id")
    iconId.value = "icon.ico"
    val iconSourceF = doc.createAttribute("SourceFile")
    iconSourceF.value = iconPath
    iconElement.setAttributeNode(iconId)
    iconElement.setAttributeNode(iconSourceF)

    val iconProperty = doc.createElement("Property")
    val iconPropertyId = doc.createAttribute("Id")
    iconPropertyId.value = "ARPPRODUCTICON"
    val iconPropertyValue = doc.createAttribute("Value")
    iconPropertyValue.value = "icon.ico"
    iconProperty.setAttributeNode(iconPropertyId)
    iconProperty.setAttributeNode(iconPropertyValue)

    productElement.appendChild(iconElement)
    productElement.appendChild(iconProperty)

    // 设置 license file
    //  <WixVariable Id="WixUILicenseRtf" Value="license.rtf" />
    val wixVariable = doc.createElement("WixVariable")
    val wixVariableId = doc.createAttribute("Id")
    wixVariableId.value = "WixUILicenseRtf"
    val wixVariableValue = doc.createAttribute("Value")
    wixVariableValue.value = licensePath
    wixVariable.setAttributeNode(wixVariableId)
    wixVariable.setAttributeNode(wixVariableValue)
    productElement.appendChild(wixVariable)

    // 安装新版时，自动卸载旧版本，已经安装新版，再安装旧版本，提示用户先卸载新版。
    // 这段逻辑要和 UpgradeCode 一起设置，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
    // 如果 UpgradeCode 改变了，可能会安装两个版本
    // <MajorUpgrade AllowSameVersionUpgrades="yes" DowngradeErrorMessage="A newer version of [ProductName] is already installed." />
    val majorUpgrade = doc.createElement("MajorUpgrade")
    val majorUpgradeDowngradeErrorMessage = doc.createAttribute("DowngradeErrorMessage")
    majorUpgradeDowngradeErrorMessage.value = "新版的[ProductName]已经安装，如果要安装旧版本，请先把新版本卸载。"
    val majorUpgradeAllowSameVersionUpgrades = doc.createAttribute("AllowSameVersionUpgrades")
    majorUpgradeAllowSameVersionUpgrades.value = "yes"
    majorUpgrade.setAttributeNode(majorUpgradeAllowSameVersionUpgrades)
    majorUpgrade.setAttributeNode(majorUpgradeDowngradeErrorMessage)
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
    val installCondition = doc.createElement("Condition")
    val installMessage = doc.createAttribute("Message")
    installMessage.value = "已经安装了幕境的另一个版本，无法继续安装此版本。可以使用”控制面板“中”添加/删除程序“来删除该版本"
    installCondition.setAttributeNode(installMessage)
    val cData= doc.createCDATASection("NOT INSTALLED")
    installCondition.appendChild(cData)
    productElement.appendChild(installCondition)


    // 设置 fragment 节点
    val fragmentElement = doc.getElementsByTagName("Fragment").item(0) as Element
    val componentGroup = fragmentElement.getElementsByTagName("ComponentGroup").item(0) as Element
    val desktopFolderRef = componentRefBuilder(doc, "DesktopFolder")
    val programMenuDirRef = componentRefBuilder(doc, "programeMenuDirComponent")
    val uninstallProductRef = componentRefBuilder(doc, "UninstallProduct")
    componentGroup.appendChild(desktopFolderRef)
    componentGroup.appendChild(programMenuDirRef)
    componentGroup.appendChild(uninstallProductRef)

//    val newWixFile = project.layout.projectDirectory.dir("build/compose/binaries/main/app/MuJing-New.wxs").getAsFile()
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
    val directory = doc.createElement("Directory")
    val attrId = doc.createAttribute("Id")
    attrId.value = id
    directory.setAttributeNode(attrId)
    if (name.isNotEmpty()) {
        val attrName = doc.createAttribute("Name")
        attrName.value = name
        directory.setAttributeNode(attrName)
    }
    return directory
}

private fun componentBuilder(doc: Document, id: String, guid: String): Element {
    val component = doc.createElement("Component")
    val scAttrId = doc.createAttribute("Id")
    scAttrId.value = id
    component.setAttributeNode(scAttrId)
    val scGuid = doc.createAttribute("Guid")
    scGuid.value = guid
    component.setAttributeNode(scGuid)
    return component
}

private fun registryBuilder(doc: Document, id: String, productCode: String): Element {
    val regComponentElement = doc.createElement("RegistryValue")
    val regAttrId = doc.createAttribute("Id")
    regAttrId.value = "$id"
    val regAttrRoot = doc.createAttribute("Root")
    regAttrRoot.value = "HKCU"
    val regKey = doc.createAttribute("Key")
    regKey.value = "Software\\MuJing"
    val regType = doc.createAttribute("Type")
    regType.value = "string"
    val regName = doc.createAttribute("Name")
    regName.value = "ProductCode"
    val regValue = doc.createAttribute("Value")
    regValue.value = productCode
    val regKeyPath = doc.createAttribute("KeyPath")
    regKeyPath.value = "yes"
    regComponentElement.setAttributeNode(regAttrId)
    regComponentElement.setAttributeNode(regAttrRoot)
    regComponentElement.setAttributeNode(regAttrRoot)
    regComponentElement.setAttributeNode(regKey)
    regComponentElement.setAttributeNode(regType)
    regComponentElement.setAttributeNode(regName)
    regComponentElement.setAttributeNode(regValue)
    regComponentElement.setAttributeNode(regKeyPath)
    return regComponentElement
}

private fun registrySearchBuilder(doc:Document,version:String,id:Int):Element{
    val registrySearch = doc.createElement("RegistrySearch")
    val registrySearchId = doc.createAttribute("Id")
    registrySearchId.value = "SearchOldVersion$id"
    val registryRoot = doc.createAttribute("Root")
    registryRoot.value = "HKCU"
    val registryKey = doc.createAttribute("Key")
    registryKey.value = "Software\\深圳市龙华区幕境网络工作室\\幕境\\$version"
    val registryName = doc.createAttribute("Name")
    registryName.value = "ProductCode"
    val registryType = doc.createAttribute("Type")
    registryType.value = "raw"
    val registryWin64 = doc.createAttribute("Win64")
    registryWin64.value = "yes"
    registrySearch.setAttributeNode(registrySearchId)
    registrySearch.setAttributeNode(registryRoot)
    registrySearch.setAttributeNode(registryKey)
    registrySearch.setAttributeNode(registryName)
    registrySearch.setAttributeNode(registryType)
    registrySearch.setAttributeNode(registryWin64)
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
    arguments: String = ""
): Element {
    val shortcut = doc.createElement("Shortcut")
    val shortcutId = doc.createAttribute("Id")
    shortcutId.value = id
    val shortcutName = doc.createAttribute("Name")
    shortcutName.value = name
    val shortcutTarget = doc.createAttribute("Target")
    shortcutTarget.value = target
    shortcut.setAttributeNode(shortcutId)

    shortcut.setAttributeNode(shortcutName)
    shortcut.setAttributeNode(shortcutTarget)

    if (directory.isNotEmpty()) {
        val shortcutDir = doc.createAttribute("Directory")
        shortcutDir.value = directory
        shortcut.setAttributeNode(shortcutDir)
    }

    if (workingDirectory.isNotEmpty()) {
        val shortcutWorkDir = doc.createAttribute("WorkingDirectory")
        shortcutWorkDir.value = workingDirectory
        shortcut.setAttributeNode(shortcutWorkDir)
    }
    if (description.isNotEmpty()) {
        val shortcutDescription = doc.createAttribute("Description")
        shortcutDescription.value = description
        shortcut.setAttributeNode(shortcutDescription)
    }

    if (arguments.isNotEmpty()) {
        val shortcutArguments = doc.createAttribute("Arguments")
        shortcutArguments.value = arguments
        shortcut.setAttributeNode(shortcutArguments)
    }

    return shortcut
}

private fun removeFolderBuilder(doc: Document, id: String): Element {
    val removeFolder = doc.createElement("RemoveFolder")
    val attrId = doc.createAttribute("Id")
    attrId.value = id
    removeFolder.setAttributeNode(attrId)
    val attrOn = doc.createAttribute("On")
    attrOn.value = "uninstall"
    removeFolder.setAttributeNode(attrOn)
    return removeFolder
}

private fun componentRefBuilder(doc: Document, id: String): Element {
    val componentRef = doc.createElement("ComponentRef")
    val attrId = doc.createAttribute("Id")
    attrId.value = id
    componentRef.setAttributeNode(attrId)
    return componentRef
}

private fun createNameUUID(str: String): String {
    return "{" + UUID.nameUUIDFromBytes(str.toByteArray(StandardCharsets.UTF_8)).toString().uppercase() + "}"
}

@file:OptIn(ExperimentalComposeUiApi::class)

package pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.*
import config.route_left_item_color
import entity.File
import kotlinx.coroutines.*
import status.currentDevice
import theme.*
import utils.*
import java.awt.event.InputEvent
import java.text.DecimalFormat
import javax.swing.JFileChooser


val fileList = mutableStateListOf<File>()
val defaultDir = mutableStateOf("/sdcard/")
private var filter = ""
private val requester = FocusRequester()

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileManage() {
    if (currentDevice.value.isNotBlank() && fileList.isEmpty() && defaultDir.value == "/sdcard/" && filter.isBlank()) {
        initFile()
    }
    if (currentDevice.value.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("请先连接设备")
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize().onKeyEvent {
                if (it.isCtrlPressed && it.key.keyCode == Key.N.keyCode) {
                    LogUtil.d("ctrl + n")
                    createFile()
                    return@onKeyEvent true
                }
                if (it.isCtrlPressed || it.isMetaPressed) {
                    LogUtil.d("filter ctrl、window、command")
                    return@onKeyEvent true
                }
                if (it.type == KeyEventType.KeyDown) {
                    if (it.key.keyCode >= Key.A.keyCode && it.key.keyCode <= Key.Z.keyCode) {
                        filter += Char(it.key.nativeKeyCode).lowercase()
                    } else if (it.key.keyCode == Key.Backspace.keyCode) {
                        if (filter.isNotBlank())
                            filter = filter.substring(0, filter.length - 1)
                    } else if (it.key.keyCode == Key.Delete.keyCode) {
                        deleteAll()
                        return@onKeyEvent true
                    } else if (it.key.keyCode == Key.F5.keyCode) {
                        initFile()
                        return@onKeyEvent true
                    } else if (it.key.keyCode == Key.Escape.keyCode) {
                        if (filter.isBlank()) {
                            LogUtil.d("backParent Esc")
                            backParent()
                            return@onKeyEvent true
                        } else {
                            filter = ""
                        }
                    }
                    initFile()
                    true
                } else {
                    false
                }
            }.focusRequester(requester).focusable()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp, top = 0.dp)) {
                val back = File("", defaultDir.value, "back", "", true)
                LazyColumn {
                    stickyHeader {
                        Row(modifier = Modifier.background(Color.White)) {
                            FileView(back) {
                                backParent()
                            }
                        }
                    }
                    items(fileList) {
                        if (it.name == "." || it.name == "..") {

                        } else FileView(it)
                    }
                }
                if (fileList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("没有找到文件", color = route_left_item_color)
                    }
                }
            }
            if (filter.isNotBlank()) Row(
                modifier = Modifier.background(route_left_item_color), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = filter,
                    color = SIMPLE_WHITE,
                    modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 3.dp, bottom = 3.dp)
                )
            }
        }
        SideEffect {
            // 直接在重组完成后请求Box的焦点
            requester.requestFocus()
        }
    }
}

fun backParent() {
    if (defaultDir.value != "/") {
        defaultDir.value = defaultDir.value.substring(0, defaultDir.value.length - 1)
        defaultDir.value = defaultDir.value.substring(0, defaultDir.value.lastIndexOf("/") + 1)
        LogUtil.d(defaultDir.value)
        filter = ""
        initFile()
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileView(
    file: File, runnable: () -> Unit = {
        if (file.isDir) {
            defaultDir.value += file.name
            defaultDir.value += "/"
            filter = ""
            initFile()
        }
    }
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 5.dp).background(Color.White).clip(
            RoundedCornerShape(5.dp)
        ).combinedClickable(onDoubleClick = { runnable() }, onClick = {}),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(getFileIcon(file.name, file.isDir)),
            "icon",
            tint = if (file.isDir) GOOGLE_BLUE else SIMPLE_GRAY,
            modifier = Modifier.size(36.dp)
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Row {
                SelectionContainer {
                    Text(
                        buildAnnotatedString {
                            append(file.name + " ")
                            withStyle(style = SpanStyle(SIMPLE_GRAY)) {
                                append(file.permission)
                            }
                        }
                    )
                }
            }
            Row {
                Text(file.date, fontSize = 12.sp, color = SIMPLE_GRAY)
                Text(
                    file.size, fontSize = 12.sp, color = SIMPLE_GRAY, modifier = Modifier.padding(start = 5.dp)
                )
            }
        }
        if (file.name.isNotBlank()) {
            TooltipArea(tooltip = {
                Text("copy path")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("copy")),
                    "icon",
                    tint = GOOGLE_BLUE,
                    modifier = Modifier.size(50.dp).clickable {
                        ClipboardUtil.setSysClipboardText(defaultDir.value + file.name)
                        if (!showToast.value) {
                            showToast.value = true
                            currentToastTask.value = "FileManagePathCopy"
                            toastText.value = "路径复制成功"
                        } else {
                            if (currentToastTask.value == "FileManagePathCopy") return@clickable
                            CoroutineScope(Dispatchers.Default).launch {
                                delay(1000)
                                showToast.value = true
                                currentToastTask.value = "FileManagePathCopy"
                                toastText.value = "路径复制成功"
                            }
                        }
                    }.padding(10.dp)
                )
            }
            TooltipArea(tooltip = {
                Text("rename")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("rename")),
                    "icon",
                    tint = GOOGLE_BLUE,
                    modifier = Modifier.size(50.dp).clickable {
                        title.value = "请输入文件新名称"
                        titleColor.value = GOOGLE_GREEN
                        dialogText.value = file.name
                        showCheckBox.value = false
                        hint.value = "请输入文件新名字"
                        runBoolean.value = {
                            var isError = false
                            var errorMsg = ""
                            if (dialogText.value.isBlank()) {
                                isError = true
                                errorMsg = "请正确输入内容"
                            }
                            val list = fileList.map { it.name }
                            if (list.isNotEmpty()) {
                                if (list.contains(dialogText.value.trim())) {
                                    errorMsg = "名称已存在"
                                    isError = true
                                }
                            }
                            if (isError) {
                                if (!showToast.value) {
                                    currentToastTask.value = "ConfirmDialogRenameError"
                                    toastText.value = errorMsg
                                    showToast.value = true
                                } else {
                                    if (currentToastTask.value != "ConfirmDialogRenameError") {
                                        CoroutineScope(Dispatchers.Default).launch {
                                            delay(1000)
                                            currentToastTask.value = "ConfirmDialogRenameError"
                                            toastText.value = errorMsg
                                            showToast.value = true
                                        }
                                    }
                                }
                                false
                            } else {
                                CoroutineScope(Dispatchers.Default).launch {
                                    shell("mv ${defaultDir.value}${file.name} ${defaultDir.value}${dialogText.value}")
                                    currentToastTask.value = "FileManageRename"
                                    toastText.value = "重命名成功"
                                    showToast.value = true
                                    initFile()
                                }
                                run.value = { }
                                true
                            }
                        }
                        showingConfirmDialog.value = true
                    }.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            if (!file.isDir) {
                TooltipArea(tooltip = {
                    Text("push")
                }) {
                    Icon(
                        painter = painterResource(getRealLocation("push")),
                        "icon",
                        tint = GOOGLE_GREEN,
                        modifier = Modifier.size(50.dp).clickable {
                            val path = PathSelector.selectPath("选择文件", JFileChooser.FILES_ONLY)
                            if (path.isNotBlank()) {
                                val start =
                                    if (path.contains("\\")) path.lastIndexOf("\\") + 1 else path.lastIndexOf("/") + 1
                                val path1 = path.substring(start, path.length)
                                title.value = "警告"
                                titleColor.value = GOOGLE_RED
                                dialogText.value = "是否把文件$path1 push到 ${file.name}"
                                needRun.value = true
                                run.value = {
                                    run.value = {}
                                    needRun.value = false
                                    CoroutineScope(Dispatchers.Default).launch {
                                        push(path, defaultDir.value + file.name)
                                        if (showToast.value) {
                                            delay(1000)
                                        }
                                        currentToastTask.value = "FileManagePush"
                                        toastText.value = "文件push成功"
                                        showToast.value = true
                                        initFile()
                                    }
                                }
                                showingDialog.value = true
                            }
                        }.padding(9.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            TooltipArea(tooltip = {
                Text("pull")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("save")),
                    "icon",
                    tint = GOOGLE_YELLOW,
                    modifier = Modifier.size(50.dp).clickable {
                        val path = PathSelector.selectDir("保存到")
                        if (path.isNotBlank()) {
                            CoroutineScope(Dispatchers.Default).launch {
                                val line = if (path.contains("\\")) "\\" else "/"
                                pull("${defaultDir.value}${file.name}", "$path$line${file.name}")
                                if (showToast.value) {
                                    delay(1000)
                                }
                                currentToastTask.value = "FileManageSave"
                                toastText.value = "文件已保存到$path"
                                showToast.value = true
                            }
                        }
                    }.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            TooltipArea(tooltip = {
                Text("delete")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("delete")),
                    "icon",
                    tint = GOOGLE_RED,
                    modifier = Modifier.size(50.dp).clickable {
                        title.value = "警告"
                        titleColor.value = GOOGLE_RED
                        dialogText.value = "是否删除${defaultDir.value}${file.name}"
                        needRun.value = true
                        run.value = {
                            run.value = {}
                            GlobalScope.launch {
                                shell("rm -rf '${defaultDir.value}${file.name}'")
                                if (showToast.value) {
                                    delay(1000)
                                }
                                currentToastTask.value = "FileManageDelete"
                                toastText.value = "${file.name}已删除"
                                showToast.value = true
                                initFile()
                            }
                        }
                        showingDialog.value = true
                    }.padding(10.dp)
                )
                Spacer(modifier = Modifier.width(15.dp))
            }
        } else {
            TooltipArea(tooltip = {
                Text("Jump to the copied path")
            }) {
                Icon(painter = painterResource(getRealLocation("go")), null, modifier = Modifier.size(50.dp).clickable {
                    var path = ClipboardUtil.getSysClipboardText()
                    val res = shell("ls $path")
                    if (path.trim().isBlank() || res.trim().isBlank()) {
                        if (!showToast.value) {
                            showToast.value = true
                            currentToastTask.value = "FileManagePaste"
                            toastText.value = "不是有效路径"
                        } else {
                            if (currentToastTask.value == "FileManagePaste") return@clickable
                            CoroutineScope(Dispatchers.Default).launch {
                                delay(1000)
                                showToast.value = true
                                currentToastTask.value = "FileManagePaste"
                                toastText.value = "不是有效路径"
                            }
                        }
                        return@clickable
                    } else {
                        if (res.contains("/") || res.contains("\\")) {
                            val end = if (path.contains("\\")) path.lastIndexOf("\\") + 1 else path.lastIndexOf("/") + 1
                            path = path.substring(0, end)
                        } else {
                            if (path.get(path.length - 1) != '/' && path.get(path.length - 1) != '\\') path += if (path.contains(
                                    "\\"
                                )
                            ) "\\" else "/"
                        }
                        defaultDir.value = path
                        initFile()
                        if (!showToast.value) {
                            toastText.value = "已跳转"
                            currentToastTask.value = "FileManageGoPath"
                            showToast.value = true
                        } else {
                            if (currentToastTask.value != "FileManageGoPath") {
                                CoroutineScope(Dispatchers.Default).launch {
                                    delay(1000)
                                    toastText.value = "已跳转"
                                    showToast.value = true
                                    currentToastTask.value = "FileManageGoPath"
                                }
                            }
                        }
                    }
                }.padding(10.dp), tint = GOOGLE_BLUE)
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("Paste the path file you copied")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("paste")),
                    null,
                    modifier = Modifier.size(50.dp).clickable {
                        val path = ClipboardUtil.getSysClipboardText()
                        val res = shell("ls $path")
                        if (path.trim().isBlank() || res.trim().isBlank()) {
                            if (!showToast.value) {
                                showToast.value = true
                                currentToastTask.value = "FileManagePaste"
                                toastText.value = "不是有效路径"
                            } else {
                                if (currentToastTask.value == "FileManagePaste") return@clickable
                                CoroutineScope(Dispatchers.Default).launch {
                                    delay(1000)
                                    showToast.value = true
                                    currentToastTask.value = "FileManagePaste"
                                    toastText.value = "不是有效路径"
                                }
                            }
                            return@clickable
                        } else {
                            title.value = "警告"
                            titleColor.value = GOOGLE_RED
                            dialogText.value = "是否把${path}复制到此目录下"
                            needRun.value = true
                            run.value = {
                                needRun.value = false
                                run.value = {}
                                CoroutineScope(Dispatchers.Default).launch {
                                    shell("cp $path ${defaultDir.value}")
                                    currentToastTask.value = "FileManageFileCopy"
                                    toastText.value = "${path}复制成功"
                                    showToast.value = true
                                    initFile()
                                }
                            }
                            showingDialog.value = true
                        }
                    }.padding(10.dp),
                    tint = GOOGLE_BLUE
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("push")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("push")),
                    null,
                    modifier = Modifier.size(50.dp).clickable {
                        val path = PathSelector.selectFileOrDir("选择文件")
                        if (path.isNotBlank()) {
                            val start =
                                if (path.contains("\\")) path.lastIndexOf("\\") + 1 else path.lastIndexOf("/") + 1
                            val path1 = path.substring(start, path.length)
                            title.value = "警告"
                            titleColor.value = GOOGLE_RED
                            dialogText.value = "是否把文件$path1 push到 ${defaultDir.value}"
                            needRun.value = true
                            run.value = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    run.value = {}
                                    push(path, defaultDir.value + path1)
                                    if (showToast.value) {
                                        delay(1000)
                                    }
                                    currentToastTask.value = "FileManagePush"
                                    toastText.value = "文件push成功"
                                    showToast.value = true
                                    initFile()
                                }
                            }
                            showingDialog.value = true
                        }
                    }.padding(9.dp),
                    tint = GOOGLE_BLUE
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("create folder or file")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("folder-add")),
                    null,
                    modifier = Modifier.size(50.dp).clickable {
                        createFile()
                    }.padding(10.dp),
                    tint = GOOGLE_BLUE
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("back")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("back")),
                    null,
                    modifier = Modifier.size(50.dp).clickable {
                        backParent()
                    }.padding(10.dp),
                    tint = GOOGLE_BLUE
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("refresh")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("sync")),
                    null,
                    modifier = Modifier.size(50.dp).clickable {
                        initFile()
                    }.padding(10.dp),
                    tint = GOOGLE_BLUE
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            TooltipArea(tooltip = {
                Text("rm -rf *")
            }) {
                Icon(
                    painter = painterResource(getRealLocation("delete")),
                    "icon",
                    tint = GOOGLE_RED,
                    modifier = Modifier.size(50.dp).clickable {
                        deleteAll()
                    }.padding(10.dp)
                )
                Spacer(modifier = Modifier.width(15.dp))
            }
        }
    }
}

fun deleteAll() {
    title.value = "警告"
    titleColor.value = GOOGLE_RED
    dialogText.value = "是否删除${defaultDir.value}下所有文件"
    needRun.value = true
    run.value = {
        run.value = {}
        CoroutineScope(Dispatchers.Default).launch {
            shell("rm -rf '${defaultDir.value}'")
            mkdir(defaultDir.value)
            if (showToast.value) {
                delay(1000)
            }
            currentToastTask.value = "FileManageDeleteAll"
            toastText.value = "删除成功"
            showToast.value = true
            initFile()
        }
    }
    showingDialog.value = true
}

fun createFile() {
    title.value = "请输入文件夹/文件名称"
    titleColor.value = GOOGLE_GREEN
    dialogText.value = ""
    showCheckBox.value = true
    checkBox.value = true
    checkBoxText.value = "文件"
    hint.value = "请输入文件夹/文件名称"
    runBoolean.value = {
        var isError = false
        var errorMsg = ""
        if (dialogText.value.isBlank()) {
            isError = true
            errorMsg = "请正确输入内容"
        }
        if (dialogText.value.contains("/")) {
            val res = shell("ls ${dialogText.value}")
            if (res.isNotBlank()) {
                isError = true
                errorMsg = "重复创建"
            }
        } else {
            val listDir = fileList.filter { it.isDir }.map { it.name }
            if (listDir.isNotEmpty()) {
                if (listDir.contains(dialogText.value.trim())) {
                    errorMsg = "文件夹已存在"
                    isError = true
                }
            }
            val listFile = fileList.filter { !it.isDir }.map { it.name }
            if (listFile.isNotEmpty()) {
                if (listFile.contains(dialogText.value.trim())) {
                    errorMsg = "文件已存在"
                    isError = true
                }
            }
        }
        if (isError) {
            if (!showToast.value) {
                currentToastTask.value = "NewFileConfirmDialogRenameError"
                toastText.value = errorMsg
                showToast.value = true
            } else {
                if (currentToastTask.value != "NewFileConfirmDialogRenameError") {
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(1000)
                        currentToastTask.value = "NewFileConfirmDialogRenameError"
                        toastText.value = errorMsg
                        showToast.value = true
                    }
                }
            }
            false
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                if (checkBox.value) {
                    if (dialogText.value.contains("/")) {
                        val end = dialogText.value.lastIndexOf("/")
                        mkdir(defaultDir.value + dialogText.value.substring(0, end))
                    }
                    touch(defaultDir.value + dialogText.value)
                    toastText.value = "文件创建成功"
                } else {
                    mkdir(defaultDir.value + dialogText.value)
                    toastText.value = "文件夹创建成功"
                }
                currentToastTask.value = "FileManageRename"
                showToast.value = true
                initFile()
            }
            run.value = { }
            true
        }
    }
    showingConfirmDialog.value = true
}

fun initFile() {
    fileList.clear()
    var cmd = "ls -al ${defaultDir.value}"
    if (filter.isNotBlank()) {
        cmd += " | grep -i '${filter}'"
        if (BashUtil.split == "\\") {
            cmd = "\"" + cmd + "\""
        }
    }
    val res = shell(cmd)
    var arr = res.replace("total 0", "").trim().split("\n").filter {
        it.isNotBlank()
    }
    if (arr.isEmpty()) return
    if (arr.size != 1) arr = arr.subList(1, arr.size)
    arr.forEach {
        val contentArr = it.split(" ").filter {
            it.trim().isNotEmpty()
        }
        val permission = contentArr[0].substring(1).replace("-".toRegex(), "")
        val size = contentArr[4]
        val date = "${contentArr[5]}  ${contentArr[6]}".substring(2)
        val isDir = !contentArr[0].startsWith("-")
        val name = contentArr[7]
        fileList.add(File(name, permission, setSize(size), date, isDir))
    }
}


fun setSize(oldSize: String): String {
    var size = 0
    size = try {
        oldSize.toInt()
    } catch (e: Exception) {
        NumberValueUtil.getStrNumber(oldSize)
    }
    //获取到的size为：1705230
    val GB = 1024 * 1024 * 1024 //定义GB的计算常量
    val MB = 1024 * 1024 //定义MB的计算常量
    val KB = 1024 //定义KB的计算常量
    val df = DecimalFormat("0.00") //格式化小数
    var resultSize = ""
    resultSize = if (size / GB >= 1) {
        //如果当前Byte的值大于等于1GB
        df.format((size / GB.toFloat()).toDouble()) + "GB   "
    } else if (size / MB >= 1) {
        //如果当前Byte的值大于等于1MB
        df.format((size / MB.toFloat()).toDouble()) + "MB   "
    } else if (size / KB >= 1) {
        //如果当前Byte的值大于等于1KB
        df.format((size / KB.toFloat()).toDouble()) + "KB   "
    } else {
        size.toString() + "B   "
    }
    return resultSize
}

fun getFileIcon(fileName: String, isDir: Boolean): String {
    return if (isDir) getRealLocation("folder")
    else if (fileName.endsWith(".apk")) getRealLocation("android")
    else if (fileName.endsWith(".jar")) getRealLocation("java")
    else if (fileName.endsWith(".json")) getRealLocation("json")
    else if (fileName.endsWith(".so")) getRealLocation("dependency")
    else if (fileName.endsWith(".cfg") || fileName.endsWith(".conf")) getRealLocation("settings")
    else if (fileName.endsWith(".txt") || fileName.endsWith(".xml")) getRealLocation("file-text")
    else if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) getRealLocation("file-image")
    else if (fileName.endsWith(".zip") || fileName.endsWith(".tar") || fileName.endsWith(".gz") || fileName.endsWith(".7z")) getRealLocation(
        "file-zip"
    )
    else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".rf")) getRealLocation("music")
    else getRealLocation("file")
}
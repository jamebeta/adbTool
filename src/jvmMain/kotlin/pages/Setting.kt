package pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import components.PathSelector
import components.currentToastTask
import components.showToast
import components.toastText
import config.route_left_background
import config.route_left_item_clicked_color
import config.route_left_item_color
import kotlinx.coroutines.*
import status.*
import status.autoSync
import theme.GOOGLE_BLUE
import utils.*
import java.io.File
import kotlin.text.StringBuilder


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Settings() {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(5.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            Text("ADBTool by xiaoming", color = route_left_item_color, modifier = Modifier.clickable {
                if (!showToast.value) {
                    toastText.value = "Hello World!"
                    currentToastTask.value = "SettingAuthor"
                    showToast.value = true
                } else {
                    if (currentToastTask.value != "SettingAuthor") {
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(1000)
                            toastText.value = "Hello World!"
                            showToast.value = true
                            currentToastTask.value = "SettingAuthor"
                        }
                    }
                }
            })
        }
        Column(modifier = Modifier.fillMaxSize().padding(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    autoSync.value,
                    onCheckedChange = {
                        autoSync.value = it
                        if (autoSync.value)
                            ListenDeviceUtil.listenDevices()
                        PropertiesUtil.setValue("autoSync", if (autoSync.value) "1" else "0", "")
                        LogUtil.d("autoSync value change ==> " + autoSync.value)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = GOOGLE_BLUE)
                )
                TooltipArea(tooltip = {
                    Text("关闭后手动点击手机图标刷新,建议开启,最小化时取消刷新")
                }) {
                    Text(text = "自动刷新",
                        color = route_left_item_color,
                        modifier = Modifier.align(Alignment.CenterVertically).clickable {
                            autoSync.value = !autoSync.value
                            if (autoSync.value)
                                ListenDeviceUtil.listenDevices()
                            PropertiesUtil.setValue("autoSync", if (autoSync.value) "1" else "0", "")
                            LogUtil.d("autoSync value change ==> " + autoSync.value)
                        })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    saveLog.value,
                    onCheckedChange = {
                        saveLog.value = it
                        PropertiesUtil.setValue("saveLog", if (saveLog.value) "1" else "0", "")
                        LogUtil.d("saveLog value change ==> " + saveLog.value)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = GOOGLE_BLUE)
                )

                Text(text = "保存日志",
                    color = route_left_item_color,
                    modifier = Modifier.align(Alignment.CenterVertically).clickable {
                        saveLog.value = !saveLog.value
                        PropertiesUtil.setValue("saveLog", if (saveLog.value) "1" else "0", "")
                        LogUtil.d("saveLog value change ==> " + saveLog.value)
                    })
            }
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingLable("自动时长(s)")
                for (i in 5..60 step 5) {
                    SelectButton(
                        "$i",
                        if (i != checkDevicesTime.value) route_left_background else GOOGLE_BLUE,
                        if (i != checkDevicesTime.value) route_left_item_color else route_left_item_clicked_color
                    ) {
                        checkDevicesTime.value = i
                        PropertiesUtil.setValue("checkDevicesTime", "${checkDevicesTime.value}", "")
                        LogUtil.d("checkDevicesTime value change ==> " + checkDevicesTime.value)
                    }
                }
            }
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingLable("程序起始页")
                pages.forEachIndexed { index, page ->
                    SelectButton(
                        page.name,
                        if (index != status.index.value) route_left_background else GOOGLE_BLUE,
                        if (index != status.index.value) route_left_item_color else route_left_item_clicked_color
                    ) {
                        status.index.value = index
                        PropertiesUtil.setValue("index", "${status.index.value}", "")
                        LogUtil.d("index value change ==> ${status.index.value}")
                    }
                }
            }

            Row(
                modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingLable("当前adb")
                TextField(
                    adb.value,
                    onValueChange = { },
                    modifier = Modifier.weight(1f).height(48.dp).padding(end = 10.dp),
                    enabled = false,
                    trailingIcon = {
                        TooltipArea(tooltip = {
                            Text("切换")
                        }) {
                            Icon(
                                painterResource(getRealLocation("folder")),
                                null,
                                modifier = Modifier.size(30.dp).clickable {
                                    val path = if (isWindows) {
                                        PathSelector.selectFile(StringBuilder("切换为"), "exe")
                                    } else {
                                        PathSelector.selectFile(StringBuilder("切换为"))
                                    }
                                    if (path.isNotBlank()) {
                                        adb.value = path
                                        PropertiesUtil.setValue("adb", adb.value, "")
                                        CoroutineScope(Dispatchers.Default).launch {
                                            if (showToast.value) {
                                                delay(1000)
                                            }
                                            currentToastTask.value = "SettingAdbPathChangeSuccess"
                                            toastText.value = "已更新"
                                            showToast.value = true
                                        }
                                    }
                                },
                                tint = route_left_item_color
                            )
                        }

                    }
                )
            }

            Row(
                modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingLable("桌面目录")
                val file = File(desktop.value)
                if (!file.exists()) {
                    desktop.value = BashUtil.workDir
                }
                TextField(
                    desktop.value,
                    onValueChange = { },
                    modifier = Modifier.weight(1f).height(48.dp).padding(end = 10.dp),
                    enabled = false,
                    trailingIcon = {
                        TooltipArea(tooltip = {
                            Text("切换目录")
                        }) {
                            Icon(
                                painterResource(getRealLocation("folder")),
                                null,
                                modifier = Modifier.size(30.dp).clickable {
                                    val path = PathSelector.selectDir("切换目录到")
                                    if (path.isNotBlank()) {
                                        desktop.value = path
                                        PropertiesUtil.setValue("desktop", desktop.value, "")
                                        CoroutineScope(Dispatchers.Default).launch {
                                            if (showToast.value) {
                                                delay(1000)
                                            }
                                            currentToastTask.value = "SettingFilePathChangeSuccess"
                                            toastText.value = "已更新"
                                            showToast.value = true
                                        }
                                    } else {
                                        CoroutineScope(Dispatchers.Default).launch {
                                            if (showToast.value) {
                                                delay(1000)
                                            }
                                            currentToastTask.value = "SettingFilePathChangeThrow"
                                            toastText.value = "异常"
                                            showToast.value = true
                                        }
                                    }
                                },
                                tint = route_left_item_color
                            )
                        }

                    }
                )
            }
        }
    }
}

@Composable
fun SelectButton(str: String, backgroundColor: Color, textColor: Color, click: () -> Unit) {
    Button(
        onClick = click,
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(str, color = textColor)
    }
}

@Composable
fun SettingLable(str: String, width: Int = 100) {
    Text(str, color = route_left_item_color, modifier = Modifier.width(width.dp))
}


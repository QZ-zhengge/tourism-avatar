package com.example.uniapp_box

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.uniapp_box.float.ExoPlayerManager
import com.example.uniapp_box.float.FloatingWindow
import com.example.uniapp_box.ui.theme.UniappBoxTheme

/**
 * 主 Activity
 *
 * 功能：
 * - WebView 加载 H5 页面
 * - 悬浮窗叠加在 WebView 之上
 * - 支持通过 Intent 传入 URL
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从 Intent 中提取 URL
        val initialUrl = extractUrlFromIntent(intent) ?: DEFAULT_URL
        
        setContent {
            UniappBoxTheme {
                MainScreen(initialUrl = initialUrl)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的 Intent（当 Activity 已存在时）
        setIntent(intent)
        // 注意：由于 Compose 状态管理，这里可能需要更复杂的处理
        // 目前简单处理，重启 Activity 可能更可靠
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放 ExoPlayer 资源
        ExoPlayerManager.forceRelease()
    }
    
    /**
     * 从 Intent 中提取 URL
     */
    private fun extractUrlFromIntent(intent: Intent?): String? {
        intent ?: return null
        
        val action = intent.action
        val data = intent.data
        
        return when {
            // 处理 VIEW action
            action == Intent.ACTION_VIEW && data != null -> {
                val scheme = data.scheme
                
                when (scheme) {
                    // 自定义 scheme: uniappbox://open/https://example.com
                    "uniappbox" -> {
                        val host = data.host
                        val path = data.path
                        
                        if (host == "open" && path != null) {
                            // 路径部分就是 URL（去掉开头的 /）
                            val url = path.removePrefix("/")
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                url
                            } else {
                                // 如果没有协议，添加 https://
                                "https://$url"
                            }
                        } else {
                            null
                        }
                    }
                    
                    // http/https 链接直接打开
                    "http", "https" -> {
                        data.toString()
                    }
                    
                    else -> null
                }
            }
            
            // 处理通过 extra 传入的 URL
            else -> {
                intent.getStringExtra(EXTRA_URL)
            }
        }
    }
    
    companion object {
        // 默认URL - 可以是远程URL或本地assets路径
        // 本地文件格式: "file:///android_asset/h5/index.html"
        const val DEFAULT_URL = "file:///android_asset/h5/index.html"
        const val EXTRA_URL = "extra_url"

        // 本地H5文件目录名（在assets目录下）
        const val LOCAL_H5_DIR = "h5"
    }
}

@Composable
private fun MainScreen(initialUrl: String) {
    val context = LocalContext.current
    
    // 使用 rememberSaveable 在配置更改时保持 URL 状态
    var currentUrl by rememberSaveable { mutableStateOf(initialUrl) }
    
    // 麦克风权限状态
    var hasMicPermission by remember { mutableStateOf(false) }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }
    
    // 检查初始权限状态
    remember {
        hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 请求权限的回调函数
    val requestMicPermission = {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // WebView（H5 内容）
        WebViewContainer(
            url = currentUrl,
            modifier = Modifier.fillMaxSize()
        )

        // 悬浮窗（叠加在 WebView 之上）
        FloatingWindow(
            hasMicPermission = hasMicPermission,
            onRequestMicPermission = requestMicPermission
        )
    }
}

@Composable
private fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 检查是否是本地assets文件
    val isLocalFile = url.startsWith("file:///android_asset/")

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // 基础设置
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    // 更多设置以支持各种网页
                    settings.databaseEnabled = true
                    settings.setGeolocationEnabled(true)
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.defaultTextEncodingName = "UTF-8"

                    // 设置 User-Agent 为移动端
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            Log.d("WebView", "Page finished loading: $url")
                            // 注入 JavaScript 调试代码
                            view?.evaluateJavascript("console.log('Page loaded successfully')", null)
                            // 获取页面标题
                            view?.evaluateJavascript("document.title", { title ->
                                Log.d("WebView", "Page title: $title")
                            })
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            isLoading = false
                            hasError = true
                            errorMessage = "错误码: $errorCode - ${description ?: "未知错误"}"
                            Log.e("WebView", "Error: $errorCode - $description at $failingUrl")
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            Log.w("WebView", "SSL error: ${error?.toString()}")
                            // 忽略 SSL 错误（仅用于调试）
                            handler?.proceed()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            Log.d("WebView", "Progress: $newProgress%")
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                Log.d("WebView", "Console: ${it.message()} [${it.sourceId()}:${it.lineNumber()}]")
                            }
                            return true
                        }
                    }

                    // 启用 WebGL 支持
                    settings.setSupportMultipleWindows(false)

                    // 加载URL（支持本地和远程）
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载中...", color = Color.Gray)
                    if (isLocalFile) {
                        Text("加载本地文件", color = Color.LightGray, fontSize = 12.sp)
                    } else {
                        Text(url, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }

        // 错误状态
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败", color = Color.Red, fontSize = 16.sp)
                    Text(errorMessage, color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLocalFile) {
                        Text("请确保uniapp H5文件已打包到assets/h5目录", color = Color.LightGray, fontSize = 12.sp)
                    } else {
                        Text(url, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

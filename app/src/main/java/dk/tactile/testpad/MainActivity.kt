package dk.tactile.testpad

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.status
import dk.tactile.testpad.ui.theme.TestPADTheme

class MainActivity : ComponentActivity() {
    private val assetPackName = "on_demand_pack"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val assetPackManager = AssetPackManagerFactory.getInstance(this)
            var status = remember { mutableStateOf("Waiting for download...") }
            TestPADTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DownloadScreen(assetPackManager, assetPackName, status, this)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun DownloadScreen(
    assetPackManager: AssetPackManager,
    assetPackName: String,
    status: MutableState<String>,
    activity: ComponentActivity
) {
    val downloadUpdateListener = remember {
        AssetPackStateUpdateListener { state ->
            when (state.status) {
                AssetPackStatus.PENDING -> status.value = "Download pending..."
                AssetPackStatus.DOWNLOADING -> status.value = "Downloading: ${
                    (state.bytesDownloaded().toDouble() / state.totalBytesToDownload()
                        .toDouble() * 100).toInt()
                }%"

                AssetPackStatus.COMPLETED -> status.value = "Download completed"
                AssetPackStatus.FAILED -> status.value = "Download failed"
                // handle other states if needed
            }
        }
    }

    DisposableEffect(activity) {
        assetPackManager.registerListener(downloadUpdateListener)
        onDispose {
            assetPackManager.unregisterListener(downloadUpdateListener)
        }
    }

    // Getting the version code.
    var versionCode: Long = 0
    try {
        versionCode =
            activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Button(onClick = {
                status.value = "Initiating Download..."
                assetPackManager.fetch(listOf(assetPackName))
                    .addOnCompleteListener { state ->
                        status.value = "Did succeed: ${state.isSuccessful} ${state.exception}"
                    }
            }) {
                Text("Download On Demand")
            }

            Text(status.value)

            Text(
                "App Version: $versionCode",
                modifier = Modifier.align(Alignment.End),
                fontSize = 14.sp
            )

        }
    }
}
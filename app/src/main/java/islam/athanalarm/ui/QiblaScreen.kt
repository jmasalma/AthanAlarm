package islam.athanalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import islam.athanalarm.MainViewModel
import islam.athanalarm.R
import islam.athanalarm.view.QiblaCompassView
import net.sourceforge.jitl.astro.Dms
import java.text.DecimalFormat

@Composable
fun QiblaScreen(viewModel: MainViewModel) {
    val location by viewModel.location.observeAsState()
    val qiblaDirection by viewModel.qiblaDirection.observeAsState()
    val northDirection by viewModel.northDirection.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Column {
                Text(text = stringResource(id = R.string.latitude))
                Text(text = stringResource(id = R.string.longitude))
                Text(text = stringResource(id = R.string.qibla))
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                val df = DecimalFormat("#.###")
                val latDms = location?.let { Dms(it.latitude) }
                val lonDms = location?.let { Dms(it.longitude) }
                val qiblaDms = qiblaDirection?.let { Dms(it) }

                Text(text = "${latDms?.degree}° ${latDms?.minute}' ${df.format(latDms?.second)}''")
                Text(text = "${lonDms?.degree}° ${lonDms?.minute}' ${df.format(lonDms?.second)}''")
                Text(text = "${qiblaDms?.degree}° ${qiblaDms?.minute}' ${df.format(qiblaDms?.second)}''")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = { context ->
                QiblaCompassView(context, null)
            },
            update = { view ->
                northDirection?.let { north ->
                    qiblaDirection?.let { qibla ->
                        view.setDirections(north, qibla.toFloat())
                    }
                }
            }
        )
    }
}
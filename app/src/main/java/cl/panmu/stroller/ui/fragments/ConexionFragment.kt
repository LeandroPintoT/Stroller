package cl.panmu.stroller.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.util.QrScanner
import io.obswebsocket.community.client.OBSRemoteController
import io.obswebsocket.community.client.message.event.outputs.RecordStateChangedEvent
import io.obswebsocket.community.client.message.event.outputs.StreamStateChangedEvent
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.floor
import kotlin.math.roundToInt


class ConexionFragment : Fragment(R.layout.main_activity) {

    private var _binding: View? = null
    private lateinit var view: View
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var jsonObs: JSONObject
    lateinit var mainHandler: Handler
    var lastBytes: Long = 0
    private val viewModel: PageViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        mainHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflater.inflate(R.layout.fragment_conexion, container, false)
        view = _binding!!
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            recargaBotones(!isConnected)
            requireActivity().runOnUiThread {
                val btnStream: Button = view.findViewById(R.id.btnStream)
                val btnRecord: Button = view.findViewById(R.id.btnRecord)
                btnStream.visibility = if (isConnected) View.VISIBLE else View.INVISIBLE
                btnRecord.visibility = if (isConnected) View.VISIBLE else View.INVISIBLE
            }
        }

        viewModel.isStreaming.observe(viewLifecycleOwner) { isStreaming ->
            // setea la visibilidad de la tabla de estadisticas de stream/grabacion
            view.findViewById<TableLayout>(R.id.infoLayout).visibility = if (isStreaming) View.VISIBLE else if (viewModel.isRecording.value!!) View.VISIBLE else View.INVISIBLE
            // actualiza boton stream
            requireActivity().runOnUiThread {
                val btnStream: Button = view.findViewById(R.id.btnStream)
                btnStream.setText(if (isStreaming) R.string.btn_detener_transmision else R.string.btn_iniciar_transmision)
                // si esta desconectado, hace el startForResult, si no, desconecta y recarga
                btnStream.setOnClickListener(
                    if (isStreaming) { {
                        AlertDialog
                            .Builder(requireActivity())
                            .setTitle(R.string.alerta_emision_titulo)
                            .setMessage(R.string.alerta_emision_final_msg)
                            .setPositiveButton(R.string.alerta_emision_final_btn_pos) { _, _ ->
                                viewModel.obsController.value?.stopStream {
                                    if (it.isSuccessful)
                                        requireActivity().runOnUiThread { viewModel.isStreaming(false) }
                                    else
                                        Toast.makeText(view.context, "No fue posible detener la emisión", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_emision_final_btn_neg) { _, _ -> }
                            .show()
                    } }
                    else { {
                        AlertDialog
                            .Builder(requireActivity())
                            .setTitle(R.string.alerta_emision_titulo)
                            .setMessage(R.string.alerta_emision_inicio_msg)
                            .setPositiveButton(R.string.alerta_emision_inicio_btn_pos) { _, _ ->
                                viewModel.obsController.value?.startStream {
                                    if (it.isSuccessful) {
                                        requireActivity().runOnUiThread {
                                            viewModel.isStreaming(true)
                                            viewModel.timeStream(LocalDateTime.now())
                                        }
                                    }
                                    else
                                        Toast.makeText(view.context, "No fue posible iniciar la emisión", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_emision_inicio_btn_neg) { _, _ -> }
                            .show()
                    } }
                )
            }
        }

        viewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            view.findViewById<TableLayout>(R.id.infoLayout).visibility = if (isRecording) View.VISIBLE else if (viewModel.isStreaming.value!!) View.VISIBLE else View.INVISIBLE
            // actualiza boton stream
            requireActivity().runOnUiThread {
                val btnRecord: Button = view.findViewById(R.id.btnRecord)
                btnRecord.setText(if (isRecording) R.string.btn_detener_grabacion else R.string.btn_iniciar_grabacion)
                // si esta desconectado, hace el startForResult, si no, desconecta y recarga
                btnRecord.setOnClickListener(
                    if (isRecording) { {
                        AlertDialog
                            .Builder(requireActivity())
                            .setTitle(R.string.alerta_grabacion_titulo)
                            .setMessage(R.string.alerta_grabacion_final_msg)
                            .setPositiveButton(R.string.alerta_grabacion_final_btn_pos) { _, _ ->
                                viewModel.obsController.value?.stopRecord {
                                    if (it.isSuccessful)
                                        requireActivity().runOnUiThread { viewModel.isRecording(false) }
                                    else
                                        Toast.makeText(view.context, "No fue posible iniciar la grabación", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_grabacion_final_btn_neg) { _, _ -> }
                            .show()
                    } }
                    else { {
                        AlertDialog
                            .Builder(requireActivity())
                            .setTitle(R.string.alerta_grabacion_titulo)
                            .setMessage(R.string.alerta_grabacion_inicio_msg)
                            .setPositiveButton(R.string.alerta_grabacion_inicio_btn_pos) { _, _ ->
                                viewModel.obsController.value?.startRecord {
                                    if (it.isSuccessful) {
                                        requireActivity().runOnUiThread {
                                            viewModel.isRecording(true)
                                            viewModel.timeRecord(LocalDateTime.now())
                                        }
                                    }
                                    else
                                        Toast.makeText(view.context, "No fue posible iniciar la grabación", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_grabacion_inicio_btn_neg) { _, _ -> }
                            .show()
                    } }
                )
            }
        }

        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val valor: String = intent.getStringExtra("barcode").toString()
                    val datos = valor.split("//")[1]
                    val host = datos.split(":")[0]
                    val port: Int = Integer.parseInt(datos.split("/")[0].split(":")[1])
                    val pass = datos.split("/")[1]
                    jsonObs = JSONObject()
                    jsonObs.put("host", host)
                    jsonObs.put("port", port)
                    jsonObs.put("pass", pass)

                    viewModel.obsController(OBSRemoteController.builder()
                        .autoConnect(false)
                        .host(host) // Default host
                        .port(port) // Default port
                        .password(pass) // Provide your password here
                        .connectionTimeout(3) // Seconds the client will wait for OBS to respond
                        .lifecycle() // para agregar callbacks
                            .onReady(::onObsReady) // agrega el callback onReady
                            .onConnect { onObsConnect() }
                            .onClose { onObsClose() }
                            .onDisconnect(::onObsDisconnect)
                            .and() // hace build al lifecycle
                        .registerEventListener(StreamStateChangedEvent::class.java) {
                            requireActivity().runOnUiThread { viewModel.isStreaming(it.outputActive) }
                        }
                        .registerEventListener(RecordStateChangedEvent::class.java) {
                            requireActivity().runOnUiThread { viewModel.isRecording(it.outputActive) }
                        }
                        .build())
                    viewModel.obsController.value?.connect()
                }
            }
        }

        val config = File(requireActivity().filesDir, "config.cfg")
        if (config.exists() && config.readLines().isNotEmpty()) {
            jsonObs = JSONObject(config.readLines().first())
            viewModel.obsController(OBSRemoteController.builder()
                .autoConnect(false)
                .host(jsonObs.get("host").toString()) // Default host
                .port(Integer.parseInt(jsonObs.get("port").toString())) // Default port
                .password(jsonObs.get("pass").toString()) // Provide your password here
                .connectionTimeout(3) // Seconds the client will wait for OBS to respond
                .lifecycle() // para agregar callbacks
                .onReady(::onObsReady) // agrega el callback onReady
                .onConnect { onObsConnect() }
                .onClose { onObsClose() }
                .onDisconnect(::onObsDisconnect)
                .and() // hace build al lifecycle
                .registerEventListener(StreamStateChangedEvent::class.java) {
                    requireActivity().runOnUiThread { viewModel.isStreaming(it.outputActive) }
                }
                .registerEventListener(RecordStateChangedEvent::class.java) {
                    requireActivity().runOnUiThread { viewModel.isRecording(it.outputActive) }
                }
                .build())
            viewModel.obsController.value?.connect()
        }
        else {
            recargaBotones(true)
        }
    }

    private fun onObsReady() {
        Log.d("ONOBSREADY", "ready")

        requireActivity().runOnUiThread {
            val txtTitulo: TextView = view.findViewById(R.id.txtTitulo)
            txtTitulo.setText(R.string.titulo_conexion)
            viewModel.isConnected(true)
        }

        viewModel.obsController.value?.getRecordStatus {
            if (it.outputActive)
                requireActivity().runOnUiThread {
                    val dif = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - it.outputDuration.toLong()
                    val ldt = Instant.ofEpochMilli(dif).atZone(ZoneOffset.UTC).toLocalDateTime()
                    viewModel.timeRecord(ldt)
                    viewModel.isRecording(it.outputActive)
                }
        }

        viewModel.obsController.value?.getStreamStatus {
            if (it.outputActive)
                requireActivity().runOnUiThread {
                    val dif = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - it.outputDuration.toLong()
                    val ldt = Instant.ofEpochMilli(dif).atZone(ZoneOffset.UTC).toLocalDateTime()
                    viewModel.timeStream(ldt)
                    viewModel.isStreaming(it.outputActive)
                }
        }

        val config = File(requireActivity().filesDir, "config.cfg")
        if (config.exists()) {
            if (config.delete() && config.createNewFile())
                config.writeText(jsonObs.toString())
        } else {
            if (config.createNewFile())
                config.writeText(jsonObs.toString())
        }
    }

    private fun onObsClose() {
        Log.d("ONOBSCLOSE", "Cerrado")
    }

    private fun onObsConnect() {
        Log.d("ONOBSCONNECT", "Conectado")
    }

    private fun onObsDisconnect() {
        Log.d("ONOBSDISCONNECT", "Desconectado")
        requireActivity().runOnUiThread {
            val txtTitulo: TextView = view.findViewById(R.id.txtTitulo)
            txtTitulo.setText(R.string.titulo_conexion_no_conectado)
            viewModel.isConnected(false)
        }
    }

    private fun recargaBotones(isNowDisconnected: Boolean) {
        val btn: Button = view.findViewById(R.id.btnConDescon)
        btn.setText(if (isNowDisconnected) R.string.btn_read_qr else R.string.btn_disconnect)
        // si esta desconectado, hace el startForResult, si no, desconecta y recarga
        btn.setOnClickListener(
            if (isNowDisconnected) { {
                startForResult.launch(Intent(requireActivity(), QrScanner::class.java))
            } }
            else { {
                viewModel.obsController.value?.disconnect()
            } }
        )
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, 1000)
             if (viewModel.isConnected.value != null && viewModel.isConnected.value!!) {
                if (viewModel.isStreaming.value!!) {
                    viewModel.obsController.value?.getStreamStatus { streamStats ->
                        val currBytes = streamStats.outputBytes.toLong()
                        val bitrate = ((currBytes - lastBytes) * 8) / 1000
                        lastBytes = currBytes
                        viewModel.obsController.value?.getStats { stats ->
                            view.findViewById<TextView>(R.id.txtDuracion).text = if (viewModel.timeStream.value != null)
                                (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - viewModel.timeStream.value!!.toEpochSecond(ZoneOffset.UTC)).toString() else "0"
                            val usage = ((stats.cpuUsage.toFloat() * 100).roundToInt() / 100).toString() + "%"
                            view.findViewById<TextView>(R.id.txtCPU).text = usage
                            view.findViewById<TextView>(R.id.txtFPS).text = ((stats.activeFps.toFloat() * 100).roundToInt() / 100).toString()
                            view.findViewById<TextView>(R.id.txtSkipped).text = stats.outputSkippedFrames.toString()
                            val mem = ((stats.memoryUsage.toFloat() * 10).roundToInt() / 10).toString() + " MB"
                            view.findViewById<TextView>(R.id.txtMemoria).text = mem
                            view.findViewById<TextView>(R.id.txtBitrate).text = bitrate.toString()
                        }
                    }
                }
                else if (viewModel.isRecording.value!!) {
                    viewModel.obsController.value?.getRecordStatus { recordStats ->
                        val currBytes = recordStats.outputBytes.toLong()
                        val bitrate = ((currBytes - lastBytes) * 8) / 1000
                        lastBytes = currBytes
                        viewModel.obsController.value?.getStats { stats ->
                            view.findViewById<TextView>(R.id.txtDuracion).text = if (viewModel.timeRecord.value != null)
                                getTimeFormatted(viewModel.timeRecord.value!!) else "0"
                            val usage = ((stats.cpuUsage.toFloat() * 100).roundToInt().toFloat() / 100).toString() + "%"
                            view.findViewById<TextView>(R.id.txtCPU).text = usage
                            view.findViewById<TextView>(R.id.txtFPS).text = ((stats.activeFps.toFloat() * 100).roundToInt().toFloat() / 100).toString()
                            view.findViewById<TextView>(R.id.txtSkipped).text = stats.outputSkippedFrames.toString()
                            val mem = ((stats.memoryUsage.toFloat() * 10).roundToInt().toFloat() / 10).toString() + " MB"
                            view.findViewById<TextView>(R.id.txtMemoria).text = mem
                            view.findViewById<TextView>(R.id.txtBitrate).text = bitrate.toString()
                        }
                    }
                }
            }
        }
    }

    private fun getTimeFormatted(inicio: LocalDateTime): String {
        val difSecs = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) - inicio.toEpochSecond(ZoneOffset.UTC)
        val secs = difSecs % 60
        val mins = floor(difSecs.toDouble() / 60)
        val horas = floor(mins / 60)
        return String.format("%02d:%02d:%02d", horas.toInt(), mins.toInt(), secs.toInt())
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateTextTask)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateTextTask)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
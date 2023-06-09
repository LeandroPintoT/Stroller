package cl.panmu.stroller.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.util.QrScanner
import io.obswebsocket.community.client.OBSRemoteController
import io.obswebsocket.community.client.WebSocketCloseCode
import io.obswebsocket.community.client.listener.lifecycle.ReasonThrowable
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
    private lateinit var loadingWindow: PopupWindow

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

        val btnDatos = view.findViewById<Button>(R.id.btnDatos)
        btnDatos.setOnClickListener {
            muestraDatosPopup()
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            recargaBotones(!isConnected)
            requireActivity().runOnUiThread {
                val btnsLayout: TableLayout = view.findViewById(R.id.tableLayoutBtnsStream)
                btnsLayout.visibility = if (isConnected) View.VISIBLE else View.INVISIBLE
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

                    conectar(jsonObs.get("host").toString(), jsonObs.get("port").toString(), jsonObs.get("pass").toString())
                }
            }
        }

        val config = File(requireActivity().filesDir, "config.cfg")
        if (config.exists() && config.readLines().isNotEmpty()) {
            jsonObs = JSONObject(config.readLines().first())
            conectar(jsonObs.get("host").toString(), jsonObs.get("port").toString(), jsonObs.get("pass").toString())
        }
        else {
            recargaBotones(true)
        }
    }

    private fun conectar(host: String, port: String, pass: String) {
        viewModel.obsController(OBSRemoteController.builder()
            .autoConnect(false)
            .host(host) // Default host
            .port(Integer.parseInt(port)) // Default port
            .password(pass) // Provide your password here
            .connectionTimeout(3) // Seconds the client will wait for OBS to respond
            .lifecycle() // para agregar callbacks
            .onReady(::onObsReady) // agrega el callback onReady
            .onConnect { onObsConnect() }
            .onClose { code -> onObsClose(code) }
            .onDisconnect(::onObsDisconnect)
            .onCommunicatorError { toggleSpinner(view, false) }
            .onControllerError { r ->
                requireActivity().runOnUiThread {
                    Log.d("ERROR", "Localized: ${r.throwable.localizedMessage} - Razon: ${r.reason} - Msg: ${r.throwable.message}")
                    toggleSpinner(view, false)
                    AlertDialog
                        .Builder(requireActivity())
                        .setTitle(R.string.alerta_conexion_error)
                        .setMessage("${resources.getString(R.string.alerta_conexion_error_msg)}:\n${getRazon(r)}")
                        .setPositiveButton(R.string.alerta_conexion_error_btn_pos) { _, _ -> }
                        .show()
                }
            }
            .and() // hace build al lifecycle
            .registerEventListener(StreamStateChangedEvent::class.java) {
                requireActivity().runOnUiThread { viewModel.isStreaming(it.outputActive) }
            }
            .registerEventListener(RecordStateChangedEvent::class.java) {
                requireActivity().runOnUiThread { viewModel.isRecording(it.outputActive) }
            }
            .build())

        toggleSpinner(view, true)

        Thread {
            viewModel.obsController.value?.connect()
        }.start()
    }

    @SuppressLint("InflateParams")
    private fun toggleSpinner(view: View, show: Boolean) {
        requireActivity().runOnUiThread {
            if (show && !this::loadingWindow.isInitialized) {
                val popupView = LayoutInflater.from(view.context).inflate(R.layout.loading_spinner, null)
                popupView.findViewById<ConstraintLayout>(R.id.loadingBackground).background.alpha = 150
                // crea la ventana del popup, si no es focusable, no se puede levantar el teclado para los edittext
                loadingWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
                loadingWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
            else if (show) {
                loadingWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
            else if (this::loadingWindow.isInitialized) {
                loadingWindow.dismiss()
            }
        }
    }

    private fun getRazon(r: ReasonThrowable): String {
        val razon = r.reason
        val msg = r.throwable.message
        if (razon.startsWith("Could not contact OBS") && msg != null) {
            if (msg.startsWith("Unable to resolve host")) {
                return resources.getString(R.string.alerta_conexion_error_msg_host)
            }
            return msg
        }
        else if (msg == null) {
            return resources.getString(R.string.alerta_conexion_error_msg_port)
        }
        return msg
    }

    private fun guardaDatos(jsonObs: JSONObject) {
        val config = File(requireActivity().filesDir, "config.cfg")
        if (config.exists()) {
            if (config.delete() && config.createNewFile())
                config.writeText(jsonObs.toString())
        } else {
            if (config.createNewFile())
                config.writeText(jsonObs.toString())
        }
    }

    private fun onObsReady() {
        Log.d("ONOBSREADY", "ready")

        requireActivity().runOnUiThread {
            val txtTitulo: TextView = view.findViewById(R.id.txtTituloConexion)
            txtTitulo.setText(R.string.titulo_conexion)
            val btnDatos = view.findViewById<Button>(R.id.btnDatos)
            btnDatos.visibility = View.INVISIBLE
            btnDatos.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
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

        guardaDatos(jsonObs)

        toggleSpinner(view, false)
    }

    private fun onObsClose(code: WebSocketCloseCode) {
        Log.d("ONOBSCLOSE", "Cerrado: Code: ${code.code} - Name: ${code.name}")
        if (code.code == 4009) {
            requireActivity().runOnUiThread {
                toggleSpinner(view, false)
                AlertDialog
                    .Builder(requireActivity())
                    .setTitle(R.string.alerta_conexion_error)
                    .setMessage(R.string.alerta_conexion_error_msg_pass)
                    .setPositiveButton(R.string.alerta_conexion_error_btn_pos) { _, _ -> }
                    .show()
            }
        }
    }

    private fun onObsConnect() {
        Log.d("ONOBSCONNECT", "Conectado")
    }

    private fun onObsDisconnect() {
        Log.d("ONOBSDISCONNECT", "Desconectado")
        requireActivity().runOnUiThread {
            val txtTitulo: TextView = view.findViewById(R.id.txtTituloConexion)
            txtTitulo.setText(R.string.titulo_conexion_no_conectado)
            val btnDatos = view.findViewById<Button>(R.id.btnDatos)
            btnDatos.visibility = View.VISIBLE
            btnDatos.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
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
                                getTimeFormatted(viewModel.timeStream.value!!) else "0"
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
        val auxMins = floor(difSecs.toDouble() / 60)
        val mins = auxMins % 60
        val horas = floor(auxMins / 60)
        return String.format("%02d:%02d:%02d", horas.toInt(), mins.toInt(), secs.toInt())
    }

    @SuppressLint("InflateParams")
    private fun muestraDatosPopup() {
        val popupView = LayoutInflater.from(view.context).inflate(R.layout.popup_conexion_datos, null)
        popupView.findViewById<ConstraintLayout>(R.id.popupBackground).background.alpha = 150
        // crea la ventana del popup, si no es focusable, no se puede levantar el teclado para los edittext
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        val btnCancelar = popupView.findViewById<Button>(R.id.btnDatosCancelar)
        btnCancelar.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(popupView.windowToken, 0)
            popupWindow.dismiss()
        }
        val etxtHost = popupView.findViewById<EditText>(R.id.etxt_ip)
        val etxtPort = popupView.findViewById<EditText>(R.id.etxt_port)
        val etxtPass = popupView.findViewById<EditText>(R.id.etxt_pass)
        val btnConectar = popupView.findViewById<Button>(R.id.btnDatosConectar)
        btnConectar.setOnClickListener {
            if (etxtHost.text.toString().trim() != "" && etxtPort.text.toString().trim() != "" && etxtPass.text.toString().trim() != "") {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(popupView.windowToken, 0)
                conectar(etxtHost.text.toString(), etxtPort.text.toString(), etxtPass.text.toString())
                popupWindow.dismiss()
            }
            else
                Toast.makeText(view.context, "Debes ingresar datos en todos los campos", Toast.LENGTH_LONG).show()
        }
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
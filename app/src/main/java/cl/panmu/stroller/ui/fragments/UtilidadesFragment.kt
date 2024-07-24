package cl.panmu.stroller.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException


class UtilidadesFragment : Fragment() {
    private var _binding: View? = null
    private lateinit var view: View
    private val viewModel: PageViewModel by activityViewModels()
    private val port: Int = 9898

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = inflater.inflate(R.layout.fragment_utilidades, container, false)
        view = _binding!!

        val textoTitulo: TextView = view.findViewById(R.id.txtTituloUtilidades)
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            textoTitulo.text = resources.getText(if (isConnected) R.string.titulo_escenas else R.string.titulo_no_conectado)
        }

        val btnRestarContador = view.findViewById<Button>(R.id.btnRestarContador)
        btnRestarContador.setOnClickListener {
            enviarRestaContador()
            btnRestarContador.clearFocus()
        }

        val btnSumarContador = view.findViewById<Button>(R.id.btnSumarContador)
        btnSumarContador.setOnClickListener {
            enviarSumaContador()
            btnSumarContador.clearFocus()
        }

        return binding
    }

    private fun enviarRestaContador() {
        val clientSocket = Socket()
        val timeout = 100
        try {
            clientSocket.connect(InetSocketAddress(viewModel.host, port), timeout)
            val dos = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream()))
            val ba = "resta1muertes".toByteArray()

            dos.write(ba, 0, ba.size)
            dos.flush()
            clientSocket.close()
        } catch (_: SocketTimeoutException) { }
    }

    private fun enviarSumaContador() {
        val clientSocket = Socket()
        val timeout = 100
        try {
            clientSocket.connect(InetSocketAddress(viewModel.host, port), timeout)
            val dos = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream()))
            val ba = "suma1muertes".toByteArray()

            dos.write(ba, 0, ba.size)
            dos.flush()
            clientSocket.close()
        } catch (_: SocketTimeoutException) { }
    }
}
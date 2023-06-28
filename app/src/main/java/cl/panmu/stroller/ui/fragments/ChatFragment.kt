package cl.panmu.stroller.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import org.json.JSONObject
import java.io.File


class ChatFragment : Fragment() {
    private var _binding: View? = null
    private lateinit var view: View
    private val viewModel: PageViewModel by activityViewModels()

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
        _binding = inflater.inflate(R.layout.fragment_chat, container, false)
        view = _binding!!

        val etxtCanal = view.findViewById<EditText>(R.id.etxt_chat_name)
        etxtCanal.setText(viewModel.username.value)

        val btnConectar = view.findViewById<Button>(R.id.btnConectarChat)
        btnConectar.setOnClickListener {
            viewModel.username(etxtCanal.text.toString())
            val userlogged = File(requireActivity().filesDir, "userlogged.cfg")
            val json = JSONObject()
            json.put("user", viewModel.username.value)
            if (userlogged.exists()) {
                if (userlogged.delete() && userlogged.createNewFile())
                    userlogged.writeText(json.toString())
            } else {
                if (userlogged.createNewFile())
                    userlogged.writeText(json.toString())
            }
            btnConectar.clearFocus()
            conectarChat()
        }

        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conectarChat()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun conectarChat() {
        viewModel.obsController.value?.getStreamServiceSettings {
            val etxtCanal = view.findViewById<EditText>(R.id.etxt_chat_name)
            val wv = view.findViewById<WebView>(R.id.webView)

            val canal = etxtCanal.text.toString()
            if (canal != "") {
                requireActivity().runOnUiThread { viewModel.username(canal) }
                val url =
                    when (it.streamServiceSettings.get("service").asString) {
                        "Twitch" -> "https://www.twitch.tv/embed/$canal/chat?parent=$canal.dev&darkpopout"
                        "YouTube - RTMPS" -> "https://www.youtube.com/live_chat?v=$canal&embed_domain=$canal.dev"
                        else -> ""
                    }
                if (url != "") {
                    requireActivity().runOnUiThread {
                        wv.settings.javaScriptEnabled = true
                        wv.loadUrl(url)
                    }
                }
            }
            else {
                requireActivity().runOnUiThread {
                    val html = resources.getString(R.string.txt_chat_webview)
                    wv.loadData(html, "text/html", "UTF-8")
                }
            }
        }
    }
}
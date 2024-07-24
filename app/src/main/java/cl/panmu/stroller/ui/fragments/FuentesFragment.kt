package cl.panmu.stroller.ui.fragments

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.util.ObsSourceItem
import cl.panmu.stroller.util.Util
import kotlin.math.roundToLong

class FuentesFragment : Fragment() {

    private var _binding: View? = null
    private lateinit var view: View
    private val viewModel: PageViewModel by activityViewModels()
    lateinit var mainHandler: Handler
    private var numFPS: Int = 1
    private val minFPS: Int = 0
    private val maxFPS: Int = 30

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
        // Inflate the layout for this fragment
        _binding = inflater.inflate(R.layout.fragment_fuentes, container, false)
        view = _binding!!

        val activity = requireActivity()

        val textoTitulo: TextView = view.findViewById(R.id.txtTituloFuentes)
        val btnRecargar: ImageButton = view.findViewById(R.id.btnRecargar)
        val btnBajarFPS = view.findViewById<ImageButton>(R.id.btnBajarFPS)
        val btnSubirFPS = view.findViewById<ImageButton>(R.id.btnSubirFPS)
        val txtNumFPS = view.findViewById<TextView>(R.id.txtNumFPS)
        val llPreview = view.findViewById<LinearLayout>(R.id.llPreview)

        btnRecargar.setOnClickListener {
            obtenerFuentes()
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            requireActivity().runOnUiThread { textoTitulo.text = resources.getText(if (isConnected) R.string.titulo_fuentes else R.string.titulo_no_conectado) }

            if (isConnected) {
                numFPS = 1
                activity.runOnUiThread{
                    txtNumFPS.text = numFPS.toString()
                    btnBajarFPS.visibility = View.VISIBLE
                    btnSubirFPS.visibility = View.VISIBLE
                    llPreview.visibility = View.VISIBLE
                    btnRecargar.visibility = View.VISIBLE
                }
            }
            else {
                numFPS = 0
                activity.runOnUiThread {
                    btnRecargar.visibility = View.INVISIBLE
                    llPreview.visibility = View.INVISIBLE
                    val sourcesLayout: TableLayout = view.findViewById(R.id.sourcesLayout)
                    sourcesLayout.removeAllViews()
                }
            }
        }

        btnBajarFPS.setOnClickListener {
            numFPS = Util.clamp(numFPS - 1, minFPS, maxFPS)
            requireActivity().runOnUiThread {
                txtNumFPS.text = if (numFPS == 0) resources.getString(R.string.texto_preview_desactivado) else numFPS.toString()

                btnSubirFPS.visibility = View.VISIBLE
                if (numFPS == minFPS)
                    btnBajarFPS.visibility = View.INVISIBLE
            }
        }
        btnSubirFPS.setOnClickListener {
            numFPS = Util.clamp(numFPS + 1, minFPS, maxFPS)
            requireActivity().runOnUiThread {
                txtNumFPS.text = numFPS.toString()
                btnBajarFPS.visibility = View.VISIBLE
                if (numFPS == minFPS)
                    btnSubirFPS.visibility = View.INVISIBLE
            }
        }

        viewModel.sourceList.observe(viewLifecycleOwner) {
            recargarFuentes(it)
        }
        return binding
    }

    private fun obtenerFuentes() {
        viewModel.clearSourceList()
        viewModel.obsController.value?.getSceneItemList(viewModel.currScene.value) { resItemList ->
            for (item in resItemList.sceneItems) {
                if (item.sourceType == "OBS_SOURCE_TYPE_SCENE") {
                    viewModel.obsController.value?.getGroupSceneItemList(item.sourceName) { res2 ->
                        for (item2 in res2.sceneItems) {
                            if (item2.inputKind != "wasapi_output_capture" && item2.inputKind != "wasapi_input_capture") {
                                requireActivity().runOnUiThread { viewModel.addToSourceList(item.sourceName, item2.sourceName, item2.sceneItemId, item.sourceName) }
                            }
                        }
                    }
                }
                else if (item.inputKind != "wasapi_output_capture" && item.inputKind != "wasapi_input_capture") {
                    requireActivity().runOnUiThread { viewModel.addToSourceList(viewModel.currScene.value!!, item.sourceName, item.sceneItemId, "") }
                }
            }
        }
    }

    private fun recargarFuentes(arr: ArrayList<ObsSourceItem>) {
        val tabla: TableLayout = view.findViewById(R.id.sourcesLayout)
        tabla.removeAllViews()

        arr.sortedBy { fuente -> fuente.sceneName }.forEach { fuente ->
            val row = TableRow(view.context)
            row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            tabla.addView(row)

            val texto = TextView(view.context)
            texto.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT)
            val txt = if(fuente.sourceParent == "") fuente.sourceName else "(${fuente.sourceParent}) ${fuente.sourceName}"
            texto.text = txt
            texto.textSize = resources.getDimension(R.dimen.fuente_text)
            texto.maxLines = 1
            texto.ellipsize = TextUtils.TruncateAt.END
            texto.setPadding(resources.getDimension(R.dimen.fuente_text_padding).toInt(), 0, resources.getDimension(R.dimen.fuente_text_padding).toInt(), 0)
            texto.setTextColor(resources.getColor(R.color.white, null))
            row.addView(texto)

            val imgbtn = ImageButton(view.context)
            val imgBtnSize = resources.getDimension(R.dimen.fuente_imgbtn).toInt()
            imgbtn.layoutParams = TableRow.LayoutParams(imgBtnSize, imgBtnSize)
            imgbtn.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.dark_ocean_blue, null))
            imgbtn.adjustViewBounds = true
            imgbtn.scaleType = ImageView.ScaleType.FIT_END
            imgbtn.setPadding(1)
            viewModel.obsController.value?.getSceneItemEnabled(fuente.sceneName, fuente.sourceId) {
                if (it.isSuccessful) {
                    requireActivity().runOnUiThread { imgbtn.setImageResource(if (it.sceneItemEnabled) R.drawable.ic_visibility else R.drawable.ic_visibility_off) }
                }
            }
            imgbtn.contentDescription = resources.getText(R.string.desc_fuentes_imgbtn)
            imgbtn.setOnClickListener {
                viewModel.obsController.value?.getSceneItemEnabled(fuente.sceneName, fuente.sourceId) { res ->
                    viewModel.obsController.value?.setSceneItemEnabled(fuente.sceneName, fuente.sourceId, !res.sceneItemEnabled) {
                        requireActivity().runOnUiThread { imgbtn.setImageResource(if (!res.sceneItemEnabled) R.drawable.ic_visibility else R.drawable.ic_visibility_off) }
                    }
                }
            }
            row.addView(imgbtn)
        }
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            if (numFPS == 0) {
                mainHandler.postDelayed(this, 1000)
                val imgViewer = view.findViewById<ImageView>(R.id.imgViewer)
                requireActivity().runOnUiThread { imgViewer.visibility = View.INVISIBLE }
            }
            else {
                mainHandler.postDelayed(this, ((1f / numFPS) * 1000).roundToLong())
                viewModel.obsController.value?.getSourceScreenshot(viewModel.currScene.value, "jpg", 1280, 720, 90) {
                    if (it.isSuccessful) {
                        val imgViewer = view.findViewById<ImageView>(R.id.imgViewer)
                        val imageBytes = Base64.decode(it.imageData.replace("data:image/jpg;base64,", ""), 0)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        requireActivity().runOnUiThread { imgViewer.visibility = View.VISIBLE; imgViewer.setImageBitmap(bitmap) }
                    }
                }
            }
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
}
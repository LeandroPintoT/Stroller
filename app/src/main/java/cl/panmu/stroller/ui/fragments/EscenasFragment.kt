package cl.panmu.stroller.ui.fragments

import android.annotation.SuppressLint
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.os.StrictMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import io.obswebsocket.community.client.model.Scene


class EscenasFragment : Fragment() {

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflater.inflate(R.layout.fragment_escenas, container, false)
        view = _binding!!

        val textoTitulo: TextView = view.findViewById(R.id.txtTituloEscenas)
        val btnRecargar: ImageButton = view.findViewById(R.id.btnRecargar)

        btnRecargar.setOnClickListener {
            obtenerEscenas()
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            textoTitulo.text = resources.getText(if (isConnected) R.string.titulo_escenas else R.string.titulo_no_conectado)

            if (isConnected) {
                btnRecargar.visibility = View.VISIBLE
                obtenerEscenas()
            }
            else {
                btnRecargar.visibility = View.INVISIBLE
                val sceneLayout: TableLayout = view.findViewById(R.id.scenesLayout)
                sceneLayout.removeAllViews()
            }
        }

        return binding
    }

    private fun recargaEscenas(escenas: List<Scene>, currScene: String) {
        // vacia la tabla
        requireActivity().runOnUiThread {
            val sceneLayout: TableLayout = view.findViewById(R.id.scenesLayout)
            sceneLayout.removeAllViews()

            if (escenas.isNotEmpty()) {
                // declara el maximo de columnas
                val maxCols = 2
                var col = 0
                var rowId = View.generateViewId()
                var rowEscenas: TableRow

                obtenerAudio(currScene)
                obtenerFuentes(currScene)

                for (escena in escenas) {
                    // si es la primera columna, agrega una nueva fila
                    if (col == 0) {
                        rowId = View.generateViewId()
                        rowEscenas = TableRow(view.context)
                        val paramsEscenas = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
                        paramsEscenas.gravity = Gravity.CENTER
                        rowEscenas.id = rowId
                        sceneLayout.addView(rowEscenas, paramsEscenas)
                    }
                    // si no, obtiene la ultima fila creada
                    else {
                        rowEscenas = sceneLayout.findViewById(rowId) as TableRow
                    }
                    // crea el boton de la escena
                    val btn = Button(view.context)
                    btn.background.colorFilter = LightingColorFilter(resources.getColor(R.color.ocean_blue, null), 1)
                    btn.setTextColor(resources.getColor(R.color.white, null))
                    btn.text = escena.sceneName
                    btn.setOnClickListener {
                        viewModel.obsController.value?.setCurrentProgramScene(escena.sceneName) {
                            if (it.isSuccessful) {
                                requireActivity().runOnUiThread { viewModel.currScene(escena.sceneName) }
                                recargaEscenas(escenas, escena.sceneName)
                            }
                        }
                    }
                    // si es la escena seleccionada, le setea un color diferente
                    if (escena.sceneName == currScene) {
                        btn.background.colorFilter = LightingColorFilter(resources.getColor(R.color.green, null), 1)
                    }
                    // asigna 0 width y 1 de weight
                    val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    // agrega el boton a la fila
                    rowEscenas.addView(btn, params)
                    // actualiza la columna siguiente
                    col = if (col / (maxCols - 1) != 1) (col + 1) else 0
                }
            }
        }
    }

    private fun obtenerEscenas() {
        requireActivity().runOnUiThread {
            lateinit var escenas: List<Scene>

            viewModel.obsController.value?.getCurrentProgramScene { res ->
                requireActivity().runOnUiThread { viewModel.currScene(res.currentProgramSceneName) }
            }
            viewModel.obsController.value?.getSceneList { res ->
                escenas = res.scenes.asReversed()
                requireActivity().runOnUiThread { recargaEscenas(escenas, viewModel.currScene.value!!) }
            }
        }
    }

    private fun obtenerAudio(currScene: String) {
        viewModel.clearAudioList()
        viewModel.obsController.value?.getSceneItemList(currScene) { res ->
            for (item in res.sceneItems) {
                if (item.sourceType == "OBS_SOURCE_TYPE_SCENE") {
                    viewModel.obsController.value?.getGroupSceneItemList(item.sourceName) { res2 ->
                        for (item2 in res2.sceneItems) {
                            if (item2.sourceType == "OBS_SOURCE_TYPE_INPUT") {
                                viewModel.obsController.value?.getInputVolume(item2.sourceName) { resInput ->
                                    if (resInput.isSuccessful) {
                                        requireActivity().runOnUiThread { viewModel.addToAudioList(item2.sourceName, resInput.inputVolumeMul) }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (item.sourceType == "OBS_SOURCE_TYPE_INPUT") {
                    viewModel.obsController.value?.getInputVolume(item.sourceName) { resInput ->
                        if (resInput.isSuccessful) {
                            requireActivity().runOnUiThread { viewModel.addToAudioList(item.sourceName, resInput.inputVolumeMul) }
                        }
                    }
                }
            }
        }
    }

    private fun obtenerFuentes(currScene: String) {
        viewModel.clearSourceList()
        viewModel.obsController.value?.getSceneItemList(currScene) { resItemList ->
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
                    requireActivity().runOnUiThread { viewModel.addToSourceList(currScene, item.sourceName, item.sceneItemId, "") }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}